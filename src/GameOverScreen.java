import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Locale;

public final class GameOverScreen implements Screen {

    private final GameScreen game;
    private final BitmapFont font = new BitmapFont("/images/font/font.txt");
    private final String[] options = {"RESTART", "SONG SELECT"};
    private final String outcome;
    private final String defeatedController;
    private final long deathPhaseMs;
    private final long startNanos = System.nanoTime();
    private int selectedOption;

    public GameOverScreen(GameScreen game) {
        if (!game.isGameOver()) {
            throw new IllegalArgumentException("Game over requires a defeated character");
        }
        this.game = game;
        boolean both = game.isDefeated(1) && game.isDefeated(2);
        int loser = game.isDefeated(1) ? 1 : 2;
        int winner = loser == 1 ? 2 : 1;
        if (game.isTwoPlayers()) {
            outcome = both ? "DRAW" : game.getCharacterForVoice(winner).toUpperCase(Locale.ROOT) + " WINS";
            defeatedController = both ? "BOTH CHARACTERS LOST" : game.getControllerForVoice(loser) + " LOST";
        } else {
            // solo defeat offers another attempt without declaring the bot a winner
            outcome = game.getCharacterForVoice(game.getPlayerVoice()).toUpperCase(Locale.ROOT);
            defeatedController = game.getControllerForVoice(game.getPlayerVoice());
        }
        // finish every death frame and briefly hold the final pose before the menu
        long duration = 0;
        for (int voice = 1; voice <= 2; voice++) {
            if (game.isDefeated(voice)) {
                duration = Math.max(duration, game.getDeathForVoice(voice).getDurationMs());
            }
        }
        deathPhaseMs = Math.max(1_000L, duration + 300L);
    }

    public GameScreen getGame() {
        return game;
    }

    public String getSelectedOption() {
        return options[selectedOption];
    }

    public void moveSelection(int direction) {
        if (!isMenuReady()) {
            return;
        }
        selectedOption = Math.floorMod(selectedOption + direction, options.length);
    }

    public boolean isMenuReady() {
        return elapsedMs() >= deathPhaseMs;
    }

    private long elapsedMs() {
        // keep the defeat sequence moving while the song clock is paused
        return Math.max(0, (System.nanoTime() - startNanos) / 1_000_000L);
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        game.render(g, width, height);
        long animationMs = elapsedMs();
        Graphics2D menu = (Graphics2D) g.create();
        try {
            menu.setColor(new Color(0, 0, 0, 190));
            menu.fillRect(0, 0, width, height);
            if (animationMs < deathPhaseMs) {
                // show only the death animation before revealing game over and its options
                for (int voice = 1; voice <= 2; voice++) {
                    if (!game.isDefeated(voice)) {
                        continue;
                    }
                    BufferedImage frame = game.getDeathForVoice(voice).getFrame(animationMs);
                    double scale = Math.min(240.0 / frame.getWidth(), 240.0 / frame.getHeight());
                    int imageWidth = (int) Math.round(frame.getWidth() * scale);
                    int imageHeight = (int) Math.round(frame.getHeight() * scale);
                    menu.drawImage(frame, (width - imageWidth) / 2, (height - imageHeight) / 2,
                            imageWidth, imageHeight, null);
                }
                return;
            }
            int panelWidth = Math.min(700, width - 40);
            int panelHeight = 490;
            int left = (width - panelWidth) / 2;
            int top = (height - panelHeight) / 2;
            menu.setColor(new Color(12, 12, 20, 245));
            menu.fillRoundRect(left, top, panelWidth, panelHeight, 18, 18);
            menu.setColor(new Color(185, 85, 150));
            menu.drawRoundRect(left, top, panelWidth, panelHeight, 18, 18);
            drawText(menu, "GAME OVER", width / 2, top + 16, 2, panelWidth - 50);
            drawText(menu, outcome, width / 2, top + 90, 1, panelWidth - 50);
            drawText(menu, game.getSongName(), width / 2, top + 128, 0.8, panelWidth - 50);

            boolean both = game.isDefeated(1) && game.isDefeated(2);
            for (int voice = 1; voice <= 2; voice++) {
                if (!game.isDefeated(voice)) {
                    continue;
                }
                int x = width / 2 + (both ? (voice == 1 ? -105 : 105) : 0);
                BufferedImage frame = game.getDeathForVoice(voice).getFrame(animationMs);
                double scale = Math.min(180.0 / frame.getWidth(), 180.0 / frame.getHeight());
                int imageWidth = (int) Math.round(frame.getWidth() * scale);
                int imageHeight = (int) Math.round(frame.getHeight() * scale);
                menu.drawImage(frame, x - imageWidth / 2, top + 343 - imageHeight,
                        imageWidth, imageHeight, null);
            }
            drawText(menu, defeatedController, width / 2, top + 350, 0.8, panelWidth - 50);
            int buttonWidth = (panelWidth - 90) / 2;
            for (int i = 0; i < options.length; i++) {
                int x = left + 30 + i * (buttonWidth + 30);
                if (i == selectedOption) {
                    menu.setColor(new Color(65, 35, 85));
                    menu.fillRoundRect(x, top + 402, buttonWidth, 46, 8, 8);
                }
                drawText(menu, options[i], x + buttonWidth / 2, top + 410, 1, buttonWidth - 20);
            }
            drawText(menu, "ARROWS ENTER", width / 2, top + 461, 0.55, panelWidth - 50);
        } finally {
            menu.dispose();
        }
    }

    private void drawText(Graphics2D g, String text, int x, int y, double preferred, int maxWidth) {
        text = GameText.display(text);
        int textWidth = font.getTextWidth(text, 1);
        double scale = Math.min(preferred, maxWidth / (double) Math.max(1, textWidth));
        Graphics2D copy = (Graphics2D) g.create();
        try {
            copy.translate(x, y);
            copy.scale(scale, scale);
            font.draw(copy, text, -textWidth / 2, 0, 1);
        } finally {
            copy.dispose();
        }
    }
}
