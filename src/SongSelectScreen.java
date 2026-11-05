import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class SongSelectScreen implements Screen {

    private final BufferedImage background;
    private final BitmapFont font;
    private final List<String> songs = new ArrayList<>();
    private final long animationStart = System.nanoTime();
    // keep each resource folder at the same index as its song title
    private final List<String> songFolders = new ArrayList<>();

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
    }
    public SongSelectScreen(Difficulty difficulty) {
        // load the same resources and songs as the regular constructor
        this();

        // restore the difficulty selected earlier in the session
        this.difficulty = difficulty;
    }
    private final BufferedImage songBar = ImageLoader.load("/images/ui/barra_cancion.png");
    private int selectedSong = 0;
    // keep the highlighted row and the chosen difficulty
    private Difficulty difficulty = Difficulty.ADMIN;

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
        font.drawPulsingCentered(g, "SELECT A SONG", width / 2, 75, 2, 1.0, seconds);

        for (int i = 0; i < songs.size(); i++) {
            boolean selected = i == selectedSong;
            int centerX = width / 2;
            if (selected) {
                centerX -= 12;
            }

            int centerY = height / 2 - 40 + i * 75;
            drawSongButton(g, songs.get(i), centerX, centerY, selected, seconds);
        }
        // draw the difficulty as a separate menu row
        drawDifficultySelector(g, width, height);
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
        font.draw(g, "DIFFICULTY", rowX + 45, rowY + 12, 1);
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