import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class TitleScreen implements Screen {

    private final BufferedImage fondoInicio;
    private final BitmapFont font;
    private final long animationStart = System.nanoTime();

    public TitleScreen() {
        // load the start background through the shared image loader
        fondoInicio = ImageLoader.load(
                "/images/backgrounds/circuito_480x270.png"
        );

        // load the font atlas and its character configuration
        font = new BitmapFont("/images/font/font.txt");
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        // draw the background across the available area
        g.drawImage(fondoInicio, 0, 0, width, height, null);

        // draw a larger title with a continuous size pulse
        double seconds = (System.nanoTime() - animationStart)
                / 1_000_000_000.0;

        font.drawPulsingCentered(
                g,
                "LAST STANDIN NOTE",
                width / 2,
                90,
                2,
                1.15,
                seconds
        );

        // show the key used to open the main menu
        String prompt = GameText.display("PULSA ENTER");
        int promptX = (width - font.getTextWidth(prompt, 1)) / 2;

        font.draw(g, prompt, promptX, height * 3 / 4, 1);
    }
}