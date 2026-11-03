import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class MenuScreen implements Screen {

    private final BufferedImage background;
    private final BitmapFont font;

    public MenuScreen() {
        // load the resources used by the main menu
        background = ImageLoader.load(
                "/images/backgrounds/circuito_480x270.png"
        );

        font = new BitmapFont("/images/font/font.txt");
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        g.drawImage(background, 0, 0, width, height, null);

        drawCentered(g, "MENU PRINCIPAL", width, 65, 2);

        // display the main navigation options
        drawCentered(g, "JUGAR", width, 210, 1);
        drawCentered(g, "OPCIONES", width, 275, 1);
        drawCentered(g, "CREDITOS", width, 340, 1);
        drawCentered(g, "SALIR", width, 405, 1);
    }

    private void drawCentered(
            Graphics2D g,
            String text,
            int width,
            int y,
            int scale
    ) {
        int x = (width - font.getTextWidth(text, scale)) / 2;
        font.draw(g, text, x, y, scale);
    }
}