import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class SongSelectScreen implements Screen {

    private final BufferedImage background;
    private final BitmapFont font;
    private final List<String> songs = new ArrayList<>();
    private final long animationStart = System.nanoTime();

    public SongSelectScreen() {
        background = ImageLoader.load("/images/backgrounds/circuito_480x270.png");
        font = new BitmapFont("/images/font/font.txt");

        // read one song title per line
        for (String line : TextLoader.load("/songs/songs.txt")) {
            String title = line.strip();
            if (!title.isEmpty()) {
                songs.add(title);
            }
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