import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class ModeSelectScreen implements Screen {

    private final BufferedImage background;
    private final BitmapFont font;
    private final long animationStart = System.nanoTime();

    private final String[] modes = {
            "INDIVIDUAL",
            "DOS JUGADORES"
    };

    private int selectedMode = 0;

    public ModeSelectScreen() {
        // load the resources used by mode selection
        background = ImageLoader.load(
                "/images/backgrounds/circuito_480x270.png"
        );

        font = new BitmapFont("/images/font/font.txt");
    }

    public void moveSelection(int direction) {
        selectedMode = Math.floorMod(
                selectedMode + direction,
                modes.length
        );
    }

    public String getSelectedMode() {
        return modes[selectedMode];
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        g.drawImage(background, 0, 0, width, height, null);

        double seconds = (System.nanoTime() - animationStart)
                / 1_000_000_000.0;

        // animate the heading using the shared font method
        font.drawPulsingCentered(
                g,
                GameText.display("SELECCIONA EL MODO"),
                width / 2,
                75,
                1,
                1.15,
                seconds
        );

        // highlight the selected mode with an offset and a marker
        for (int i = 0; i < modes.length; i++) {
            String label = GameText.display(modes[i]);
            int x = (width - font.getTextWidth(label, 1)) / 2;
            int y = 230 + i * 90;

            if (i == selectedMode) {
                x -= 12;

                int markerX = x - font.getTextWidth("*", 1) - 12;
                font.draw(g, "*", markerX, y, 1);
            }

            font.draw(g, label, x, y, 1);
        }

        String hint = GameText.display("ESC PARA VOLVER");
        int hintX = (width - font.getTextWidth(hint, 1)) / 2;

        font.draw(g, hint, hintX, height - 65, 1);
    }
}
