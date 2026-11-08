import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class RetroButton {
    private RetroButton() { }

    public static void draw(Graphics2D g, int x, int y, int width, int height, boolean selected) {
        Graphics2D button = (Graphics2D) g.create();
        try {
            button.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            // build stepped corners and a hard shadow on the pixel grid
            button.setColor(new Color(5, 4, 12, 230));
            fillStepped(button, x + 4, y + 4, width, height, 4);
            button.setColor(selected ? new Color(220, 158, 255) : new Color(111, 91, 143));
            fillStepped(button, x, y, width, height, 4);
            button.setColor(selected ? new Color(75, 38, 105) : new Color(26, 22, 39));
            fillStepped(button, x + 2, y + 2, width - 4, height - 4, 3);

            // light the upper edge and recess the lower edge like an arcade button
            button.setColor(selected ? new Color(148, 78, 189) : new Color(64, 51, 84));
            button.fillRect(x + 5, y + 3, width - 10, 3);
            button.fillRect(x + 3, y + 6, 3, height - 12);
            button.setColor(selected ? new Color(34, 15, 54) : new Color(13, 10, 23));
            button.fillRect(x + 5, y + height - 6, width - 10, 3);
            button.fillRect(x + width - 6, y + 6, 3, height - 12);
        } finally {
            button.dispose();
        }
    }

    private static void fillStepped(Graphics2D g, int x, int y, int width, int height, int corner) {
        g.fillRect(x + corner, y, width - corner * 2, height);
        g.fillRect(x, y + corner, width, height - corner * 2);
    }
}
