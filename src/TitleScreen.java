import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class TitleScreen implements Screen {

    private final BufferedImage fondoInicio;
    private final BitmapFont font;

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

        // center and draw the game title using the original sprites
        String title = "LAST STANDIN NOTE";
        int scale = 2;
        int titleWidth = font.getTextWidth(title, scale);
        int titleX = (width - titleWidth) / 2;

        // show the key used to open the main menu
        String prompt = "PULSA ENTER";
        int promptX = (width - font.getTextWidth(prompt, 1)) / 2;

        font.draw(g, prompt, promptX, height * 3 / 4, 1);

        font.draw(g, title, titleX, 100, scale);
    }
}
