import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class MenuScreen implements Screen {

    private final BufferedImage background;
    private final BitmapFont font;

    private final String[] options = {
            "JUGAR",
            "OPCIONES",
            "CREDITOS",
            "SALIR"
    };

    private int selectedOption = 0;
    private final int selectionOffset = 12;

    public MenuScreen() {
        // load the resources used by the main menu
        background = ImageLoader.load(
                "/images/backgrounds/circuito_480x270.png"
        );

        font = new BitmapFont("/images/font/font.txt");
    }

    public void moveSelection(int direction) {
        // wrap the selection around the available options
        selectedOption = Math.floorMod(
                selectedOption + direction,
                options.length
        );
    }
    public String getSelectedOption() {
        return options[selectedOption];
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        g.drawImage(background, 0, 0, width, height, null);

        drawCentered(g, "MENU PRINCIPAL", width, 65, 2);

        // shift the selected option to the left
        for (int i = 0; i < options.length; i++) {
            int optionX = (width - font.getTextWidth(options[i], 1)) / 2;
            int optionY = 210 + i * 65;

            if (i == selectedOption) {
                optionX -= selectionOffset;
            }

            font.draw(g, options[i], optionX, optionY, 1);
        }

        // place the selection marker beside the active option
        String selectedText = options[selectedOption];

        int optionX = (width - font.getTextWidth(selectedText, 1)) / 2;
        optionX -= selectionOffset;
        int markerX = optionX - font.getTextWidth("*", 1) - 12;
        int markerY = 210 + selectedOption * 65;

        font.draw(g, "*", markerX, markerY, 1);
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