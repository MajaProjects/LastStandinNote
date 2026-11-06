import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public final class LoadingScreen implements Screen {

    private final Screen returnScreen;
    private final BitmapFont font = new BitmapFont("/images/font/font.txt");
    private final long startNanos = System.nanoTime();
    private volatile boolean abandoned;
    private boolean pauseAfterLoad;

    public LoadingScreen(Screen returnScreen) {
        this.returnScreen = returnScreen;
    }

    public Screen getReturnScreen() {
        return returnScreen;
    }

    public void abandon() {
        abandoned = true;
    }

    public boolean isAbandoned() {
        return abandoned;
    }

    public void pauseAfterLoad() {
        pauseAfterLoad = true;
    }

    public boolean shouldPauseAfterLoad() {
        return pauseAfterLoad;
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        Graphics2D copy = (Graphics2D) g.create();
        try {
            copy.setColor(new Color(9, 9, 16));
            copy.fillRect(0, 0, width, height);
            drawCentered(copy, "LOADING SONG", width / 2, height / 2 - 110, 2);
            drawCentered(copy, "PLEASE WAIT", width / 2, height / 2 - 20, 1);

            // keep the interface visibly alive while audio loads in the background
            int angle = (int) (((System.nanoTime() - startNanos) / 1_000_000L) % 1000 * 360 / 1000);
            copy.setStroke(new BasicStroke(4));
            copy.setColor(new Color(170, 80, 230));
            copy.drawArc(width / 2 - 18, height / 2 + 35, 36, 36, -angle, 260);
            drawCentered(copy, "ESC TO CANCEL", width / 2, height / 2 + 110, 1);
        } finally {
            copy.dispose();
        }
    }

    private void drawCentered(Graphics2D g, String text, int x, int y, int scale) {
        font.draw(g, text, x - font.getTextWidth(text, scale) / 2, y, scale);
    }
}