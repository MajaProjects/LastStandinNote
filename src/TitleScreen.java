import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class TitleScreen implements Screen {

    private final BufferedImage fondoInicio;

    public TitleScreen() {
        // load the start background through the shared image loader
        fondoInicio = ImageLoader.load(
                "/images/backgrounds/circuito_480x270.png"
        );
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        // draw the background across the available area
        g.drawImage(fondoInicio, 0, 0, width, height, null);
    }
}