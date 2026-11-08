import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SongSelectScreen implements Screen {

    private final BufferedImage background;
    private final BitmapFont font;
    private final List<String> songs = new ArrayList<>();
    private final long animationStart = System.nanoTime();
    // keep each resource folder at the same index as its song title
    private final List<String> songFolders = new ArrayList<>();
    // load records on entry and keep rendering free of preferences reads
    private final List<EnumMap<Difficulty, HighScores.Entry>> highScores = new ArrayList<>();
    private final Map<String, BufferedImage> rankLetters = new HashMap<>();

    public SongSelectScreen() {
        background = ImageLoader.load(
                "/images/backgrounds/circuito_480x270.png"
        );
        font = new BitmapFont("/images/font/font.txt");

        // read the resource folder and display name for each song
        for (String originalLine : TextLoader.load("/songs/songs.txt")) {
            String line = originalLine.strip();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] fields = line.split(";", -1);

            if (fields.length != 2) {
                throw new IllegalArgumentException(
                        "Each song needs a folder and a display name"
                );
            }

            String folder = fields[0].strip();
            String title = fields[1].strip();

            // use simple folder names that work inside the bundled resources
            if (!folder.matches("[a-z0-9_]+") || title.isEmpty()) {
                throw new IllegalArgumentException("Invalid song entry: " + line);
            }

            songFolders.add(folder);
            songs.add(title);
        }

        if (songs.isEmpty()) {
            throw new IllegalArgumentException("The song list is empty");
        }

        for (String folder : songFolders) {
            EnumMap<Difficulty, HighScores.Entry> records = new EnumMap<>(Difficulty.class);
            for (Difficulty level : Difficulty.values()) {
                HighScores.getEntry(folder, level).ifPresent(entry -> records.put(level, entry));
            }
            highScores.add(records);
        }
        for (String rank : new String[]{"P", "S", "A", "B", "C", "D"}) {
            rankLetters.put(rank, createRankLetter(rank));
        }
    }
    public SongSelectScreen(Difficulty difficulty) {
        // load the same resources and songs as the regular constructor
        this();

        // restore the difficulty selected earlier in the session
        this.difficulty = difficulty;
    }

    public SongSelectScreen(Difficulty difficulty, boolean twoPlayers) {
        this(difficulty);
        this.twoPlayers = twoPlayers;
    }
    private final BufferedImage songBar = ImageLoader.load("/images/ui/barra_cancion.png");
    private int selectedSong = 0;
    // keep the highlighted row and the chosen difficulty
    private Difficulty difficulty = Difficulty.ADMIN;
    private boolean twoPlayers;

    public void moveSelection(int direction) {
        // move only between the available songs
        selectedSong = Math.floorMod(
                selectedSong + direction,
                songs.size()
        );
    }


    public void changeDifficulty(int direction) {
        // move between user, admin and root
        difficulty = difficulty.move(direction);
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }
    public String getSelectedSong() {
        return songs.get(selectedSong);
    }
    public String getSelectedSongFolder() {
        // return the folder containing the selected song's chart and audio
        return "/songs/" + songFolders.get(selectedSong) + "/";
    }

    public void saveCompletedScore(GameScreen game) {
        // only completed solo games belong in the personal record table
        if (twoPlayers || game.isTwoPlayers() || !game.isFinished() || game.isGameOver()) return;

        int score = game.getStatisticsForVoice(game.getPlayerVoice()).score();
        double accuracy = game.getPerformanceForVoice(game.getPlayerVoice());
        String folder = songFolders.get(selectedSong);
        if (HighScores.saveIfBetter(folder, difficulty, score, accuracy)) {
            HighScores.getEntry(folder, difficulty)
                    .ifPresent(entry -> highScores.get(selectedSong).put(difficulty, entry));
        }
    }
    private void drawSongButton(Graphics2D g, String title, int centerX,
                                int centerY, boolean selected, double seconds) {
        Graphics2D button = (Graphics2D) g.create();

        try {
            // scale the bar and its text around the same point
            button.translate(centerX, centerY);

            if (selected) {
                double pulse = 1.0 + 0.025 * (1.0 - Math.cos(seconds * 2.0 * Math.PI));
                button.scale(pulse, pulse);
            }

            int left = -songBar.getWidth() / 2;
            int top = -songBar.getHeight() / 2;
            button.drawImage(songBar, left, top, null);

            // center the label inside the dark part of the sprite
            int textX = left + 147 - font.getTextWidth(title, 1) / 2;
            font.draw(button, title, textX, top + 7, 1);
        } finally {
            button.dispose();
        }
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        g.drawImage(background, 0, 0, width, height, null);

        double seconds = (System.nanoTime() - animationStart) / 1_000_000_000.0;
        font.drawPulsingCentered(g, GameText.display("SELECT A SONG"), width / 2, 75, 2, 1.0, seconds);

        for (int i = 0; i < songs.size(); i++) {
            boolean selected = i == selectedSong;
            int centerX = width / 2;
            if (selected) {
                centerX -= 12;
            }

            int centerY = height / 2 - 40 + i * 75;
            drawSongButton(g, songs.get(i), centerX, centerY, selected, seconds);
            if (!twoPlayers) {
                int recordX = width / 2 + songBar.getWidth() / 2 + 28;
                drawRank(g, i, recordX, centerY);
                if (selected) drawHighScore(g, i, recordX + 62, centerY);
            }
        }
        // draw the difficulty as a separate menu row
        drawDifficultySelector(g, width, height);
    }

    private void drawHighScore(Graphics2D g, int song, int left, int centerY) {
        HighScores.Entry entry = highScores.get(song).get(difficulty);
        if (entry == null) return;

        int panelWidth = 200;
        RetroButton.draw(g, left, centerY - 35, panelWidth, 70, true);
        GameHud.drawText(g, font, "BEST SCORE", left + panelWidth / 2,
                centerY - 29, 0.42, panelWidth - 20);
        GameHud.drawText(g, font, Integer.toString(entry.score()), left + panelWidth / 2,
                centerY - 15, 0.7, panelWidth - 20);
        String accuracy = "--";
        if (entry.accuracy().isPresent()) {
            double exact = entry.accuracy().getAsDouble();
            double displayed = Math.round(exact * 100.0) / 100.0;
            if (exact < 100) displayed = Math.min(99.99, displayed);
            accuracy = String.format(Locale.ROOT, "%.2f%%", displayed);
        }
        GameHud.drawText(g, font, GameText.display("BEST ACCURACY") + " " + accuracy,
                left + panelWidth / 2, centerY + 13, 0.45, panelWidth - 20);
    }

    private void drawRank(Graphics2D g, int song, int left, int centerY) {
        HighScores.Entry entry = highScores.get(song).get(difficulty);
        if (entry == null || entry.accuracy().isEmpty()) return;
        String rank = HighScores.rank(entry.accuracy().getAsDouble());
        Color color = switch (rank) {
            case "P" -> new Color(255, 210, 80);
            case "S" -> new Color(85, 215, 255);
            case "A" -> new Color(100, 220, 145);
            case "B" -> new Color(110, 160, 250);
            case "C" -> new Color(255, 175, 80);
            default -> new Color(225, 100, 135);
        };
        int size = 42;
        int top = centerY - size / 2;
        g.setColor(new Color(5, 4, 12));
        g.fillRect(left + 3, top + 3, size, size);
        g.setColor(color);
        g.fillRect(left, top, size, size);
        g.setColor(color.darker().darker().darker());
        g.fillRect(left + 3, top + 3, size - 6, size - 6);
        BufferedImage letter = rankLetters.get(rank);
        g.drawImage(letter, left + (size - letter.getWidth()) / 2,
                top + (size - letter.getHeight()) / 2, null);
    }

    private BufferedImage createRankLetter(String rank) {
        BufferedImage image = new BufferedImage(font.getTextWidth(rank, 1), 48,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D letter = image.createGraphics();
        try {
            font.draw(letter, rank, 0, 0, 1);
        } finally {
            letter.dispose();
        }
        int minX = image.getWidth(), minY = image.getHeight(), maxX = 0, maxY = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                if ((pixel >>> 24) != 0) {
                    // lighten the letter while preserving its dark outline and inner holes
                    int brightness = Math.max((pixel >> 16) & 255,
                            Math.max((pixel >> 8) & 255, pixel & 255));
                    image.setRGB(x, y, (pixel & 0xff000000) | brightness * 0x010101);
                    minX = Math.min(minX, x); minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
                }
            }
        }
        return image.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
    private void drawDifficultySelector(Graphics2D g, int width, int height) {
        int rowX = width / 2 - 250;
        int rowY = height - 82;
        int rowWidth = 500;
        int rowHeight = 60;

        // draw a separate background for the difficulty display
        g.setColor(new java.awt.Color(12, 12, 20, 220));
        g.fillRoundRect(rowX, rowY, rowWidth, rowHeight, 12, 12);

        g.setColor(new java.awt.Color(95, 75, 120));
        g.drawRoundRect(rowX, rowY, rowWidth, rowHeight, 12, 12);

        // show the selected difficulty
        font.draw(g, GameText.display("DIFFICULTY"), rowX + 45, rowY + 12, 1);
        font.draw(g, difficulty.name(), rowX + 280, rowY + 12, 1);

        // fill one, two or three segments for the chosen difficulty
        int filledLevels = difficulty.ordinal() + 1;
        int meterX = rowX + 420;
        int meterY = rowY + 21;

        for (int i = 0; i < 3; i++) {
            int segmentX = meterX + i * 21;

            if (i < filledLevels) {
                g.setColor(new java.awt.Color(170, 80, 230));
                g.fillRect(segmentX, meterY, 14, 18);
            }

            g.setColor(new java.awt.Color(220, 180, 255));
            g.drawRect(segmentX, meterY, 13, 17);
        }
    }
}
