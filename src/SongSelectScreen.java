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

    @Override
    public void render(Graphics2D g, int width, int height) {
        g.drawImage(background, 0, 0, width, height, null);

        double seconds = (System.nanoTime() - animationStart) / 1_000_000_000.0;
        font.drawPulsingCentered(g, "SELECT A SONG", width / 2, 75, 2, 1.0, seconds);

        // draw the songs in the same order as the file
        for (int i = 0; i < songs.size(); i++) {
            String title = songs.get(i);
            int x = (width - font.getTextWidth(title, 1)) / 2;
            int y = 210 + i * 65;
            font.draw(g, title, x, y, 1);
        }
    }
}