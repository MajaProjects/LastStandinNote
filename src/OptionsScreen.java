import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class OptionsScreen implements Screen {

    private final MenuScreen previousMenu;
    private final BitmapFont font = new BitmapFont("/images/font/font.txt");
    private final BufferedImage background = ImageLoader.load(
            "/images/backgrounds/circuito_480x270.png"
    );
    private final String[] labels = {
            "MUSIC VOLUME", "VOCAL VOLUME", "EFFECTS VOLUME", "SHOW FPS", "SHOW GAME STATS"
    };
    private final AudioSettings.Channel[] channels = AudioSettings.Channel.values();
    private int selectedOption;

    public OptionsScreen(MenuScreen previousMenu) {
        this.previousMenu = previousMenu;
    }

    public MenuScreen getPreviousMenu() {
        return previousMenu;
    }

    public void moveSelection(int direction) {
        selectedOption = Math.floorMod(selectedOption + direction, labels.length);
    }

    public void changeSelectedValue(int direction) {
        if (direction == 0) {
            return;
        }
        if (selectedOption >= channels.length) {
            setSelectedToggle(direction > 0);
            return;
        }
        // change only the selected channel in five percent steps
        AudioSettings.Channel channel = channels[selectedOption];
        AudioSettings.setVolume(channel, AudioSettings.getVolume(channel)
                + Integer.signum(direction) * 5);
    }

    public boolean toggleSelected() {
        if (selectedOption < channels.length) {
            return false;
        }
        setSelectedToggle(!selectedToggle());
        return true;
    }

    private boolean selectedToggle() {
        return selectedOption == 3 ? DisplaySettings.isShowFps() : DisplaySettings.isShowGameStats();
    }

    private void setSelectedToggle(boolean enabled) {
        if (selectedOption == 3) {
            DisplaySettings.setShowFps(enabled);
        } else {
            DisplaySettings.setShowGameStats(enabled);
        }
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        g.drawImage(background, 0, 0, width, height, null);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, width, height);
        drawCentered(g, "OPTIONS", width / 2, 40, 2.0);
        drawCentered(g, "AUDIO AND HUD", width / 2, 125, 0.8);

        int left = 80;
        int rowWidth = width - left * 2;
        int barLeft = width / 2 - 20;
        int barWidth = width - barLeft - 180;
        for (int row = 0; row < labels.length; row++) {
            int top = 166 + row * 58;
            boolean selected = row == selectedOption;
            g.setColor(selected ? new Color(55, 30, 75) : new Color(18, 18, 28));
            g.fillRoundRect(left, top, rowWidth, 50, 10, 10);
            g.setColor(selected ? new Color(200, 115, 255) : new Color(85, 65, 105));
            g.drawRoundRect(left, top, rowWidth, 50, 10, 10);
            font.draw(g, labels[row], left + 24, top + 8, 1);

            if (row >= channels.length) {
                boolean enabled = row == 3 ? DisplaySettings.isShowFps()
                        : DisplaySettings.isShowGameStats();
                drawCentered(g, enabled ? "ON" : "OFF", width - 126, top + 8, 1.0);
                continue;
            }
            int volume = AudioSettings.getVolume(channels[row]);

            g.setColor(new Color(35, 30, 45));
            g.fillRect(barLeft, top + 18, barWidth, 14);
            g.setColor(new Color(180, 85, 235));
            g.fillRect(barLeft, top + 18, barWidth * volume / 100, 14);
            g.setColor(new Color(215, 170, 245));
            g.drawRect(barLeft, top + 18, barWidth - 1, 13);
            drawCentered(g, volume + "%", width - 126, top + 8, 1.0);
        }
        drawCentered(g, "UP DOWN SELECT   LEFT RIGHT ADJUST   ENTER TOGGLE", width / 2, height - 68, 0.55);
        drawCentered(g, "ESC BACK   SETTINGS SAVE AUTOMATICALLY", width / 2, height - 34, 0.5);
    }

    private void drawCentered(Graphics2D g, String text, int x, int y, double scale) {
        Graphics2D copy = (Graphics2D) g.create();
        try {
            copy.translate(x, y);
            copy.scale(scale, scale);
            font.draw(copy, text, -font.getTextWidth(text, 1) / 2, 0, 1);
        } finally {
            copy.dispose();
        }
    }
}
