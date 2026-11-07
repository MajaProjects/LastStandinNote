import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class WindowController implements AutoCloseable {

    private final JFrame window;
    private final GamePanel panel;
    private final Runnable settingsListener = this::applySettings;
    private final Timer transitionFallback;
    private final WindowAdapter focusListener;
    private Rectangle windowedBounds;
    private boolean initialized;
    private boolean appliedFullscreen;
    private int appliedWidth;
    private int appliedHeight;
    private boolean closed;

    public WindowController(JFrame window, GamePanel panel) {
        this.window = window;
        this.panel = panel;
        transitionFallback = new Timer(350, event -> finishTransition());
        transitionFallback.setRepeats(false);
        focusListener = new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent event) {
                finishTransition();
            }
        };
        window.addWindowFocusListener(focusListener);
        DisplaySettings.addDisplayListener(settingsListener);
        panel.setWindowController(this);
    }

    public void showWindow() {
        applySettings();
    }

    private void applySettings() {
        if (closed) return;
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::applySettings);
            return;
        }
        boolean fullscreen = DisplaySettings.isFullscreen();
        int width = DisplaySettings.getWindowWidth();
        int height = DisplaySettings.getWindowHeight();
        if (initialized && appliedFullscreen == fullscreen
                && (fullscreen || (appliedWidth == width && appliedHeight == height))) return;

        GraphicsConfiguration config = window.getGraphicsConfiguration();
        Rectangle screen = config.getBounds();
        Insets desktopInsets = Toolkit.getDefaultToolkit().getScreenInsets(config);
        Rectangle usable = new Rectangle(screen.x + desktopInsets.left, screen.y + desktopInsets.top,
                screen.width - desktopInsets.left - desktopInsets.right,
                screen.height - desktopInsets.top - desktopInsets.bottom);
        Rectangle previousBounds = initialized && !appliedFullscreen ? window.getBounds() : windowedBounds;
        if (initialized && !appliedFullscreen) windowedBounds = new Rectangle(window.getBounds());

        panel.prepareDisplayChange();
        // borderless fullscreen uses the current desktop mode and restores native decorations on exit
        if (window.isUndecorated() != fullscreen) {
            window.dispose();
            window.setUndecorated(fullscreen);
        }
        window.setExtendedState(JFrame.NORMAL);
        if (fullscreen) {
            window.setBounds(screen);
        } else {
            panel.setPreferredSize(new Dimension(width, height));
            window.pack();
            Insets frameInsets = window.getInsets();
            Dimension content = fitWindowContent(width, height,
                    usable.width - frameInsets.left - frameInsets.right,
                    usable.height - frameInsets.top - frameInsets.bottom);
            panel.setPreferredSize(content);
            window.pack();
            int centerX = previousBounds == null ? usable.x + usable.width / 2
                    : previousBounds.x + previousBounds.width / 2;
            int centerY = previousBounds == null ? usable.y + usable.height / 2
                    : previousBounds.y + previousBounds.height / 2;
            window.setLocation(Math.max(usable.x, Math.min(centerX - window.getWidth() / 2,
                            usable.x + usable.width - window.getWidth())),
                    Math.max(usable.y, Math.min(centerY - window.getHeight() / 2,
                            usable.y + usable.height - window.getHeight())));
            windowedBounds = new Rectangle(window.getBounds());
        }
        initialized = true;
        appliedFullscreen = fullscreen;
        appliedWidth = width;
        appliedHeight = height;
        window.setVisible(true);
        window.requestFocus();
        transitionFallback.restart();
        if (window.isFocused()) finishTransition();
    }

    static Dimension fitWindowContent(int width, int height, int availableWidth, int availableHeight) {
        double scale = Math.min(1.0, Math.min(Math.max(1, availableWidth) / (double) width,
                Math.max(1, availableHeight) / (double) height));
        return new Dimension(Math.max(1, (int) Math.floor(width * scale)),
                Math.max(1, (int) Math.floor(height * scale)));
    }

    private void finishTransition() {
        transitionFallback.stop();
        panel.finishDisplayChange();
    }

    @Override
    public void close() {
        closed = true;
        transitionFallback.stop();
        DisplaySettings.removeDisplayListener(settingsListener);
        window.removeWindowFocusListener(focusListener);
    }
}
