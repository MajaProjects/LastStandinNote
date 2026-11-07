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
            "RESOLUTION", "MUSIC VOLUME", "VOCAL VOLUME", "EFFECTS VOLUME",
            "WINDOW MODE", "LANGUAGE", "FPS LIMIT", "SHOW FPS", "SHOW GAME STATS"
    };
    private static final int RESOLUTION = 0;
    private static final int FIRST_VOLUME = 1;
    private static final int WINDOW_MODE = 4;
    private static final int LANGUAGE = 5;
    private static final int FPS_LIMIT = 6;
    private static final int SHOW_FPS = 7;
    private static final int SHOW_STATS = 8;
    private static final int CONFIRM = 9;
    private static final int VISIBLE_ROWS = 5;
    private final AudioSettings.Channel[] channels = AudioSettings.Channel.values();
    private final SettingsDraft draft = new SettingsDraft();
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
        selectedOption = Math.floorMod(selectedOption + direction, labels.length + 1);
    }

    public void changeSelectedValue(int direction) {
        if (direction == 0) {
            return;
        }
        if (selectedOption == CONFIRM) return;
        if (selectedOption == WINDOW_MODE) {
            draft.setFullscreen(direction > 0);
            return;
        }
        if (selectedOption == RESOLUTION) {
            draft.cycleResolution(direction);
            return;
        }
        if (selectedOption == LANGUAGE) {
            draft.cycleLanguage(direction);
            return;
        }
        if (selectedOption == FPS_LIMIT) {
            if (editingCustom && draft.display().fpsMode() == 4) {
                draft.setCustomPosition(draft.display().customPosition()
                        + Integer.signum(direction));
            } else {
                draft.cycleFpsMode(direction);
            }
            return;
        }
        if (selectedOption == SHOW_FPS || selectedOption == SHOW_STATS) {
            setSelectedToggle(direction > 0);
            return;
        }
        // change only the selected channel in five percent steps
        AudioSettings.Channel channel = channels[selectedOption - FIRST_VOLUME];
        draft.setVolume(channel, draft.getVolume(channel)
                + Integer.signum(direction) * 5);
    }

    public boolean toggleSelected() {
        if (selectedOption == CONFIRM) return false;
        if (selectedOption == WINDOW_MODE) {
            draft.setFullscreen(!draft.display().fullscreen());
            return true;
        }
        if (selectedOption == RESOLUTION) {
            draft.cycleResolution(1);
            return true;
        }
        if (selectedOption == LANGUAGE) {
            draft.cycleLanguage(1);
            return true;
        }
        if (selectedOption == FPS_LIMIT) {
            if (draft.display().fpsMode() == 4) {
                editingCustom = !editingCustom;
            }
            return true;
        }
        if (selectedOption >= FIRST_VOLUME && selectedOption < FIRST_VOLUME + channels.length) {
            return false;
        }
        setSelectedToggle(!selectedToggle());
        return true;
    }

    public boolean isConfirmSelected() {
        return selectedOption == CONFIRM;
    }

    public boolean selectConfirmAt(int x, int y, int width) {
        if (x < 80 || x > width - 80 || y < 428 || y > 468) return false;
        selectedOption = CONFIRM;
        editingCustom = false;
        draggingCustom = false;
        return true;
    }

    public boolean hasUnsavedChanges() {
        return draft.hasChanges();
    }

    public void toggleFullscreen() {
        draft.setFullscreen(!draft.display().fullscreen());
    }

    public void discardChanges() {
        draft.discard();
        editingCustom = false;
        draggingCustom = false;
    }

    public void confirmSettings() {
        draft.apply();
        editingCustom = false;
        draggingCustom = false;
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
        if (draft.display().fpsMode() != 4 || y < 372 || y > 416
                || x < 294 || x > width - 244) {
            return false;
        }
        selectedOption = FPS_LIMIT;
        editingCustom = true;
        draggingCustom = true;
        return dragCustom(x, width);
    }

    public boolean dragCustom(int x, int width) {
        if (!draggingCustom) {
            return false;
        }
        int barWidth = Math.max(1, width - 550);
        draft.setCustomPosition((int) Math.round((x - 300) * 261.0 / barWidth));
        return true;
    }

    public void endCustomDrag() {
        draggingCustom = false;
    }

    private boolean selectedToggle() {
        return selectedOption == SHOW_FPS ? draft.display().showFps() : draft.display().showGameStats();
    }

    private void setSelectedToggle(boolean enabled) {
        if (selectedOption == SHOW_FPS) {
            draft.setShowFps(enabled);
        } else {
            draft.setShowGameStats(enabled);
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
        int firstVisible = Math.max(0, Math.min(labels.length - VISIBLE_ROWS,
                selectedOption - VISIBLE_ROWS + 1));
        for (int row = firstVisible; row < Math.min(labels.length, firstVisible + VISIBLE_ROWS); row++) {
            int top = 144 + (row - firstVisible) * 46;
            boolean selected = row == selectedOption;
            g.setColor(selected ? new Color(55, 30, 75) : new Color(18, 18, 28));
            g.fillRoundRect(left, top, rowWidth, 40, 10, 10);
            g.setColor(selected ? new Color(200, 115, 255) : new Color(85, 65, 105));
            g.drawRoundRect(left, top, rowWidth, 40, 10, 10);
            font.draw(g, GameText.display(labels[row]), left + 24, top + 3, 1);

            if (row == WINDOW_MODE || row == RESOLUTION) {
                GameHud.drawText(g, font, row == WINDOW_MODE ? (draft.display().fullscreen() ? "FULLSCREEN" : "WINDOWED")
                        : draft.display().resolutionLabel(), width - 180, top + 7, 0.85, 176);
                continue;
            }

            if (row == LANGUAGE) {
                GameHud.drawText(g, font, draft.language().displayName(), width - 180, top + 7, 0.85, 176);
                continue;
            }
            if (row == FPS_LIMIT) {
                GameHud.drawText(g, font, draft.display().fpsModeLabel(), width - 180, top + 7, 0.85, 176);
                continue;
            }

            if (row == SHOW_FPS || row == SHOW_STATS) {
                boolean enabled = row == SHOW_FPS ? draft.display().showFps()
                        : draft.display().showGameStats();
                drawCentered(g, enabled ? "ON" : "OFF", width - 126, top + 3, 1.0);
                continue;
            }
            int volume = draft.getVolume(channels[row - FIRST_VOLUME]);

            g.setColor(new Color(35, 30, 45));
            g.fillRect(barLeft, top + 14, barWidth, 12);
            g.setColor(new Color(180, 85, 235));
            g.fillRect(barLeft, top + 14, barWidth * volume / 100, 12);
            g.setColor(new Color(215, 170, 245));
            g.drawRect(barLeft, top + 14, barWidth - 1, 11);
            drawCentered(g, volume + "%", width - 126, top + 3, 1.0);
        }
        // reveal the extra display rows by scrolling while preserving the existing FPS controls
        g.setColor(new Color(45, 30, 60));
        g.fillRect(width - 63, 144, 5, 224);
        g.setColor(new Color(180, 90, 235));
        int thumbHeight = 224 * VISIBLE_ROWS / labels.length;
        g.fillRect(width - 63, 144 + firstVisible * (224 - thumbHeight) / (labels.length - VISIBLE_ROWS),
                5, thumbHeight);
        // keep confirmation at the bottom, after all options and the optional custom slider
        g.setColor(isConfirmSelected() ? new Color(85, 40, 115) : new Color(30, 20, 45));
        g.fillRoundRect(left, 428, rowWidth, 40, 10, 10);
        g.setColor(new Color(190, 110, 240));
        g.drawRoundRect(left, 428, rowWidth, 40, 10, 10);
        drawCentered(g, "CONFIRM AND BACK", width / 2, 433, 0.85);
        if (draft.display().fpsMode() == 4) {
            int sliderWidth = width - 550;
            int position = draft.display().customPosition();
            g.setColor(editingCustom ? new Color(65, 35, 85) : new Color(18, 18, 28));
            g.fillRoundRect(left, 374, rowWidth, 40, 8, 8);
            GameHud.drawText(g, font, "CUSTOM FPS", left + 100, 381, 0.65, 170);
            g.setColor(new Color(55, 40, 65));
            g.fillRect(300, 388, sliderWidth, 10);
            g.setColor(new Color(185, 90, 235));
            g.fillRect(300, 388, sliderWidth * position / 261, 10);
            g.setColor(new Color(235, 210, 255));
            g.fillRect(297 + sliderWidth * position / 261, 384, 6, 18);
            GameHud.drawText(g, font, "0", 300, 400, 0.32, 30);
            GameHud.drawText(g, font, "260", 300 + sliderWidth - 10, 400, 0.32, 35);
            GameHud.drawText(g, font, draft.display().customLabel(), width - 150, 381, 0.65, 170);
        }
        String hint = isConfirmSelected() ? "ENTER OR CLICK TO CONFIRM AND RETURN"
                : selectedOption == WINDOW_MODE ? "LEFT WINDOWED   RIGHT FULLSCREEN   F11 TOGGLE"
                : selectedOption == RESOLUTION ? (draft.display().fullscreen()
                ? "FULLSCREEN USES DESKTOP RESOLUTION   F11 WINDOWED"
                : "LEFT RIGHT SELECT RESOLUTION   ENTER NEXT")
                : selectedOption == LANGUAGE ? "LEFT RIGHT SELECT LANGUAGE   ENTER NEXT" : selectedOption == FPS_LIMIT
                ? (draft.display().fpsMode() == 4
                ? (editingCustom ? "LEFT RIGHT ADJUST OR DRAG BAR   ENTER FINISH"
                : "LEFT RIGHT PRESET   ENTER EDIT CUSTOM FPS") : "LEFT RIGHT CHOOSE FPS PRESET")
                : "UP DOWN SELECT   LEFT RIGHT ADJUST   ENTER TOGGLE";
        drawCentered(g, hint, width / 2, height - 58, 0.5);
        drawCentered(g, editingCustom ? "ESC FINISH EDIT   CONFIRM TO APPLY"
                : hasUnsavedChanges() ? "ESC BACK   UNSAVED CHANGES - CONFIRM TO APPLY"
                : "ESC BACK   NO UNSAVED CHANGES", width / 2, height - 34, 0.5);
    }

    private void drawCentered(Graphics2D g, String text, int x, int y, double scale) {
        text = GameText.display(text);
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
