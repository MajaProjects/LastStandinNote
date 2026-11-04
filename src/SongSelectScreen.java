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
    private final BufferedImage songBar = ImageLoader.load("/images/ui/barra_cancion.png");
    private int selectedSong = 0;

    public void moveSelection(int direction) {
        if (!songs.isEmpty()) {
            selectedSong = Math.floorMod(selectedSong + direction, songs.size());
        }
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
    }
}