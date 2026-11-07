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
    private final long animationStart = System.nanoTime();
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

        // animate the main menu title continuously
        double seconds = (System.nanoTime() - animationStart)
                / 1_000_000_000.0;

        font.drawPulsingCentered(
                g,
                GameText.display("MENU PRINCIPAL"),
                width / 2,
                65,
                2,
                1.0,
                seconds
        );

        // shift the selected option to the left
        for (int i = 0; i < options.length; i++) {
            String label = GameText.display(options[i]);
            int optionX = (width - font.getTextWidth(label, 1)) / 2;
            int optionY = 210 + i * 65;

            if (i == selectedOption) {
                optionX -= selectionOffset;
            }

            drawOption(
                    g,
                    label,
                    optionX,
                    optionY,
                    i == selectedOption
            );
        }

        // place the selection marker beside the active option
        String selectedText = GameText.display(options[selectedOption]);

        int optionX = (width - font.getTextWidth(selectedText, 1)) / 2;
        optionX -= selectionOffset;

        int markerX = optionX - font.getTextWidth("*", 1) - 12;
        int markerY = 210 + selectedOption * 65;

        font.draw(g, "*", markerX, markerY, 1);
    }

    private void drawOption(
            Graphics2D g,
            String text,
            int x,
            int y,
            boolean selected
    ) {
        if (!selected) {
            font.draw(g, text, x, y, 1);
            return;
        }

        // pulse smoothly between the original size and five percent larger
        double seconds = (System.nanoTime() - animationStart)
                / 1_000_000_000.0;

        double pulse = 1.0 + 0.025
                * (1.0 - Math.cos(seconds * Math.PI * 2.0));

        double halfWidth = font.getTextWidth(text, 1) / 2.0;
        Graphics2D animated = (Graphics2D) g.create();

        try {
            // keep the horizontal center fixed while scaling the text
            animated.translate(x + halfWidth, y);
            animated.scale(pulse, pulse);

            font.draw(
                    animated,
                    text,
                    -(int) Math.round(halfWidth),
                    0,
                    1
            );
        } finally {
            animated.dispose();
        }
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