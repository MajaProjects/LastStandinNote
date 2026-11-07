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
            "MUSIC VOLUME", "VOCAL VOLUME", "EFFECTS VOLUME", "SHOW FPS", "SHOW GAME STATS", "FPS LIMIT"
    };
    private final AudioSettings.Channel[] channels = AudioSettings.Channel.values();
    private int selectedOption;
    private boolean editingCustom;
    private boolean draggingCustom;

    public OptionsScreen(MenuScreen previousMenu) {
        this.previousMenu = previousMenu;
    }

    public MenuScreen getPreviousMenu() {
        return previousMenu;
    }

    public void moveSelection(int direction) {
        editingCustom = false;
        draggingCustom = false;
        selectedOption = Math.floorMod(selectedOption + direction, labels.length);
    }

    public void changeSelectedValue(int direction) {
        if (direction == 0) {
            return;
        }
        if (selectedOption == 5) {
            if (editingCustom && DisplaySettings.getFpsMode() == 4) {
                DisplaySettings.setCustomPosition(DisplaySettings.getCustomPosition()
                        + Integer.signum(direction));
            } else {
                DisplaySettings.cycleFpsMode(direction);
            }
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
        if (selectedOption == 5) {
            if (DisplaySettings.getFpsMode() == 4) {
                editingCustom = !editingCustom;
            }
            return true;
        }
        if (selectedOption < channels.length) {
            return false;
        }
        setSelectedToggle(!selectedToggle());
        return true;
    }

    public boolean cancelCustomEdit() {
        if (!editingCustom) {
            return false;
        }
        editingCustom = false;
        draggingCustom = false;
        return true;
    }

    public boolean beginCustomDrag(int x, int y, int width) {
        if (DisplaySettings.getFpsMode() != 4 || y < 426 || y > 470
                || x < 294 || x > width - 244) {
            return false;
        }
        selectedOption = 5;
        editingCustom = true;
        draggingCustom = true;
        return dragCustom(x, width);
    }

    public boolean dragCustom(int x, int width) {
        if (!draggingCustom) {
            return false;
        }
        int barWidth = Math.max(1, width - 550);
        DisplaySettings.setCustomPosition((int) Math.round((x - 300) * 261.0 / barWidth));
        return true;
    }

    public void endCustomDrag() {
        draggingCustom = false;
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
        drawCentered(g, "OPTIONS", width / 2, 30, 2.0);
        drawCentered(g, "AUDIO AND DISPLAY", width / 2, 110, 0.7);

        int left = 80;
        int rowWidth = width - left * 2;
        int barLeft = width / 2 - 20;
        int barWidth = width - barLeft - 180;
        for (int row = 0; row < labels.length; row++) {
            int top = 144 + row * 46;
            boolean selected = row == selectedOption;
            g.setColor(selected ? new Color(55, 30, 75) : new Color(18, 18, 28));
            g.fillRoundRect(left, top, rowWidth, 40, 10, 10);
            g.setColor(selected ? new Color(200, 115, 255) : new Color(85, 65, 105));
            g.drawRoundRect(left, top, rowWidth, 40, 10, 10);
            font.draw(g, labels[row], left + 24, top + 3, 1);

            if (row == 5) {
                drawCentered(g, DisplaySettings.getFpsModeLabel(), width - 180, top + 7, 0.85);
                continue;
            }

            if (row >= channels.length) {
                boolean enabled = row == 3 ? DisplaySettings.isShowFps()
                        : DisplaySettings.isShowGameStats();
                drawCentered(g, enabled ? "ON" : "OFF", width - 126, top + 3, 1.0);
                continue;
            }
            int volume = AudioSettings.getVolume(channels[row]);

            g.setColor(new Color(35, 30, 45));
            g.fillRect(barLeft, top + 14, barWidth, 12);
            g.setColor(new Color(180, 85, 235));
            g.fillRect(barLeft, top + 14, barWidth * volume / 100, 12);
            g.setColor(new Color(215, 170, 245));
            g.drawRect(barLeft, top + 14, barWidth - 1, 11);
            drawCentered(g, volume + "%", width - 126, top + 3, 1.0);
        }
        if (DisplaySettings.getFpsMode() == 4) {
            int sliderWidth = width - 550;
            int position = DisplaySettings.getCustomPosition();
            g.setColor(editingCustom ? new Color(65, 35, 85) : new Color(18, 18, 28));
            g.fillRoundRect(left, 428, rowWidth, 40, 8, 8);
            GameHud.drawText(g, font, "CUSTOM FPS", left + 100, 435, 0.65, 170);
            g.setColor(new Color(55, 40, 65));
            g.fillRect(300, 442, sliderWidth, 10);
            g.setColor(new Color(185, 90, 235));
            g.fillRect(300, 442, sliderWidth * position / 261, 10);
            g.setColor(new Color(235, 210, 255));
            g.fillRect(297 + sliderWidth * position / 261, 438, 6, 18);
            GameHud.drawText(g, font, "0", 300, 454, 0.32, 30);
            GameHud.drawText(g, font, "260", 300 + sliderWidth - 10, 454, 0.32, 35);
            GameHud.drawText(g, font, DisplaySettings.getCustomLabel(), width - 150, 435, 0.65, 170);
        }
        String hint = selectedOption == 5
                ? (DisplaySettings.getFpsMode() == 4
                ? (editingCustom ? "LEFT RIGHT ADJUST OR DRAG BAR   ENTER FINISH"
                : "LEFT RIGHT PRESET   ENTER EDIT CUSTOM FPS") : "LEFT RIGHT CHOOSE FPS PRESET")
                : "UP DOWN SELECT   LEFT RIGHT ADJUST   ENTER TOGGLE";
        drawCentered(g, hint, width / 2, height - 58, 0.5);
        drawCentered(g, editingCustom ? "ESC FINISH EDIT   SETTINGS SAVE AUTOMATICALLY"
                : "ESC BACK   SETTINGS SAVE AUTOMATICALLY", width / 2, height - 34, 0.5);
    }

    private void drawCentered(Graphics2D g, String text, int x, int y, double scale) {
        Graphics2D copy = (Graphics2D) g.create();
        try {
            copy.translate(x, y);
            copy.scale(scale, scale);
            font.drawAligned(copy, text, -font.getTextWidth(text, 1) / 2, 0, 1);
        } finally {
            copy.dispose();
        }
    }
}
