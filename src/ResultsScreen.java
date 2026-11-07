import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.function.LongSupplier;

public final class ResultsScreen implements Screen {

    private static final long COUNT_DURATION_NANOS = 800_000_000L;
    private static final String[] ROW_LABELS = {
            "TOTAL NOTES", "HIT NOTES", "SICK", "GOOD", "BAD", "SHIT", "MISS",
            "MAX COMBO", "RAW SCORE", "ACCURACY"
    };

    private final GameScreen game;
    private final BitmapFont font = new BitmapFont("/images/font/font.txt");
    private final String[] options = {"RESTART", "SONG SELECT"};
    private final String songName;
    private final String resultText;
    private final String controllerText;
    private final int winningVoice;
    private final boolean twoPlayers;
    private final int playerVoice;
    private final NoteJudge.Statistics[] statistics = new NoteJudge.Statistics[2];
    private final double[] performances = new double[2];
    private final SpriteAnimation[] characters = new SpriteAnimation[2];
    private final String[] characterNames = new String[2];
    private final String[] controllerNames = new String[2];
    private final LongSupplier clock;
    private final long startNanos;
    private int selectedOption;

    public ResultsScreen(GameScreen game) {
        this(game, System::nanoTime);
    }

    ResultsScreen(GameScreen game, LongSupplier clock) {
        this.game = game;
        this.clock = clock;
        songName = game.getSongName();
        twoPlayers = game.isTwoPlayers();
        playerVoice = game.getPlayerVoice();
        winningVoice = twoPlayers ? game.getWinningVoice() : 0;

        // snapshot statistics once so the display cannot change the final score
        for (int side = 0; side < 2; side++) {
            int voice = side + 1;
            if (!twoPlayers && voice != playerVoice) continue;
            statistics[side] = game.getStatisticsForVoice(voice);
            performances[side] = game.getPerformanceForVoice(voice);
            characters[side] = game.getIdleForVoice(voice);
            characterNames[side] = game.getCharacterForVoice(voice).toUpperCase(Locale.ROOT);
            controllerNames[side] = game.getControllerForVoice(voice);
        }

        if (twoPlayers) {
            resultText = winningVoice == 0 ? "DRAW" : characterNames[winningVoice - 1] + " WINS";
            controllerText = winningVoice == 0 ? "TIED RESULT" : controllerNames[winningVoice - 1];
        } else {
            resultText = characterNames[playerVoice - 1];
            controllerText = controllerNames[playerVoice - 1];
        }
        startNanos = clock.getAsLong();
    }

    public GameScreen getGame() {
        return game;
    }

    public String getSelectedOption() {
        return options[selectedOption];
    }

    public void moveSelection(int direction) {
        selectedOption = Math.floorMod(selectedOption + direction, options.length);
    }

    private long getElapsedNanos() {
        return Math.max(0, clock.getAsLong() - startNanos);
    }

    private double getCountProgress(long elapsedNanos) {
        // finish the fast count after 800 ms, easing into the exact final totals
        double progress = Math.min(1.0, elapsedNanos / (double) COUNT_DURATION_NANOS);
        return 1.0 - Math.pow(1.0 - progress, 3);
    }

    private int getAnimatedValue(int value, double progress) {
        return (int) Math.round(value * progress);
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        game.render(g, width, height);
        long elapsedNanos = getElapsedNanos();
        long animationMs = elapsedNanos / 1_000_000L;
        double progress = getCountProgress(elapsedNanos);
        Graphics2D menu = (Graphics2D) g.create();

        try {
            menu.setColor(new Color(0, 0, 0, 190));
            menu.fillRect(0, 0, width, height);

            int panelWidth = Math.min(920, width - 32);
            int panelHeight = 500;
            int left = (width - panelWidth) / 2;
            int top = (height - panelHeight) / 2;

            menu.setColor(new Color(12, 12, 20, 245));
            menu.fillRoundRect(left, top, panelWidth, panelHeight, 18, 18);
            menu.setColor(new Color(165, 95, 220));
            menu.drawRoundRect(left, top, panelWidth, panelHeight, 18, 18);

            drawText(menu, "RESULTS", width / 2, top + 14, 2.0, true, panelWidth - 50);
            int totalNotes = twoPlayers
                    ? statistics[0].totalNotes() + statistics[1].totalNotes()
                    : statistics[playerVoice - 1].totalNotes();
            drawText(menu, songName + " - " + totalNotes + " NOTES", width / 2,
                    top + 88, 1.0, true, panelWidth - 50);

            // show the selected character in solo play and the winner in shared play
            int winnerCenter = left + 128;
            drawText(menu, resultText, winnerCenter, top + 152, 0.85, true, 230);
            drawText(menu, controllerText, winnerCenter, top + 187, 0.65, true, 230);
            if (!twoPlayers) {
                drawCharacter(menu, characters[playerVoice - 1], animationMs,
                        winnerCenter, top + 414, 200, 190);
            } else if (winningVoice == 0) {
                // show both characters in their usual order when neither side wins
                drawCharacter(menu, characters[0], animationMs, left + 78, top + 414, 100, 165);
                drawCharacter(menu, characters[1], animationMs, left + 183, top + 414, 100, 165);
            } else {
                drawCharacter(menu, characters[winningVoice - 1], animationMs,
                        winnerCenter, top + 414, 200, 190);
            }

            menu.setColor(new Color(70, 50, 90));
            menu.drawLine(left + 255, top + 137, left + 255, top + 418);
            int tableLeft = left + 276;
            int tableWidth = panelWidth - 300;
            int[] valueCenters = {
                    tableLeft + tableWidth * 48 / 100,
                    tableLeft + tableWidth * 80 / 100
            };

            for (int row = 0; row < ROW_LABELS.length; row++) {
                drawText(menu, ROW_LABELS[row], tableLeft, top + 192 + row * 23,
                        0.72, false, 185);
            }
            for (int side = 0; side < 2; side++) {
                if (!twoPlayers && side != playerVoice - 1) continue;
                int center = twoPlayers ? valueCenters[side] : tableLeft + tableWidth * 72 / 100;
                drawText(menu, twoPlayers ? characterNames[side] : "YOUR STATS",
                        center, top + 137, 0.8, true, 175);
                drawText(menu, twoPlayers ? controllerNames[side] : "SINGLE PLAYER",
                        center, top + 166, 0.55, true, 175);

                NoteJudge.Statistics stats = statistics[side];
                int[] values = {
                        stats.totalNotes(), stats.hitNotes(), stats.sick(), stats.good(),
                        stats.bad(), stats.shit(), stats.miss(), stats.maxCombo(), stats.score()
                };
                for (int row = 0; row < ROW_LABELS.length; row++) {
                    int y = top + 192 + row * 23;
                    // keep exact raw points separate from the judgement-based accuracy
                    String value = row == values.length
                            ? formatPerformance(performances[side] * progress)
                            : String.valueOf(getAnimatedValue(values[row], progress));
                    drawText(menu, value, center, y, 0.8, true, 170);
                }
            }

            int buttonWidth = (panelWidth - 90) / 2;
            for (int i = 0; i < options.length; i++) {
                int buttonLeft = left + 30 + i * (buttonWidth + 30);
                int y = top + 441;
                if (i == selectedOption) {
                    menu.setColor(new Color(65, 35, 85));
                    menu.fillRoundRect(buttonLeft, y - 8, buttonWidth, 46, 8, 8);
                }
                drawText(menu, options[i], buttonLeft + buttonWidth / 2, y, 1.0,
                        true, buttonWidth - 25);
            }
            drawText(menu, "ARROWS ENTER", width / 2, top + 480, 0.55, true, panelWidth - 50);
        } finally {
            menu.dispose();
        }
    }

    private void drawCharacter(Graphics2D g, SpriteAnimation animation, long animationMs,
                               int centerX, int bottom, int maxWidth, int maxHeight) {
        BufferedImage frame = animation.getFrame(animationMs);
        double scale = Math.min(maxWidth / (double) frame.getWidth(),
                maxHeight / (double) frame.getHeight());
        int width = (int) Math.round(frame.getWidth() * scale);
        int height = (int) Math.round(frame.getHeight() * scale);

        // keep the idle animation moving with a small side-to-side celebration
        int sway = (int) Math.round(Math.sin(animationMs / 1000.0 * Math.PI * 2.0) * 6);
        g.drawImage(frame, centerX + sway - width / 2, bottom - height, width, height, null);
    }

    private void drawText(Graphics2D g, String text, int x, int y, double preferredScale,
                          boolean centered, int maxWidth) {
        text = GameText.display(text);
        // the atlas has no decimal point, so reserve a small gap and draw that mark below
        int points = (int) text.chars().filter(character -> character == '.').count();
        int textWidth = font.getTextWidth(text.replace(".", ""), 1) + points * 6;
        double scale = Math.min(preferredScale, maxWidth / (double) Math.max(1, textWidth));
        Graphics2D copy = (Graphics2D) g.create();
        try {
            copy.translate(x, y);
            copy.scale(scale, scale);
            int cursor = centered ? -textWidth / 2 : 0;
            String[] parts = text.split("\\.", -1);
            for (int i = 0; i < parts.length; i++) {
                font.drawAligned(copy, parts[i], cursor, 0, 1);
                cursor += font.getTextWidth(parts[i], 1);
                if (i + 1 < parts.length) {
                    copy.setColor(new Color(195, 65, 245));
                    copy.fillRect(cursor + 1, 27, 3, 3);
                    cursor += 6;
                }
            }
        } finally {
            copy.dispose();
        }
    }

    private static String formatPerformance(double percentage) {
        // reserve 100 percent for an exact full result rather than rounded near-perfection
        double displayed = Math.round(percentage * 10.0) / 10.0;
        if (percentage < 100.0 - 0.000000001) {
            displayed = Math.min(displayed, 99.9);
        }
        return String.format(Locale.ROOT, "%.1f%%", displayed);
    }
}
