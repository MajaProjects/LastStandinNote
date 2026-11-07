import java.awt.Color;
import java.awt.Graphics2D;

public final class SaveChangesScreen implements Screen {

    public enum Choice { SAVE, DISCARD, KEEP_EDITING }

    private final OptionsScreen options;
    private final boolean applicationExit;
    private final BitmapFont font = new BitmapFont("/images/font/font.txt");
    private int selectedOption = Choice.KEEP_EDITING.ordinal();

    public SaveChangesScreen(OptionsScreen options, boolean applicationExit) {
        this.options = options;
        this.applicationExit = applicationExit;
    }

    public OptionsScreen getOptions() { return options; }
    public boolean isApplicationExit() { return applicationExit; }
    public Choice getSelectedChoice() { return Choice.values()[selectedOption]; }

    public void moveSelection(int direction) {
        selectedOption = Math.floorMod(selectedOption + direction, Choice.values().length);
    }

    public boolean selectAt(int x, int y, int width, int height) {
        int left = (width - Math.min(720, width - 40)) / 2 + 30;
        int top = (height - 330) / 2 + 124;
        int rowWidth = Math.min(720, width - 40) - 60;
        if (x < left || x >= left + rowWidth || y < top) return false;
        int row = (y - top) / 52;
        if (row >= Choice.values().length || (y - top) % 52 >= 44) return false;
        selectedOption = row;
        return true;
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        options.render(g, width, height);
        Graphics2D dialog = (Graphics2D) g.create();
        try {
            dialog.setColor(new Color(0, 0, 0, 190));
            dialog.fillRect(0, 0, width, height);
            int panelWidth = Math.min(720, width - 40);
            int left = (width - panelWidth) / 2;
            int top = (height - 330) / 2;
            dialog.setColor(new Color(15, 12, 24, 250));
            dialog.fillRoundRect(left, top, panelWidth, 330, 18, 18);
            dialog.setColor(new Color(185, 95, 235));
            dialog.drawRoundRect(left, top, panelWidth, 330, 18, 18);
            GameHud.drawText(dialog, font, "SAVE CHANGES?", width / 2, top + 24, 1.3, panelWidth - 60);
            GameHud.drawText(dialog, font, "YOUR CHANGES HAVE NOT BEEN APPLIED",
                    width / 2, top + 78, 0.65, panelWidth - 60);
            String[] labels = applicationExit
                    ? new String[]{"SAVE AND QUIT", "QUIT WITHOUT SAVING", "KEEP EDITING"}
                    : new String[]{"SAVE AND BACK", "DISCARD CHANGES", "KEEP EDITING"};
            for (int i = 0; i < labels.length; i++) {
                int y = top + 124 + i * 52;
                if (i == selectedOption) {
                    dialog.setColor(new Color(65, 35, 85));
                    dialog.fillRoundRect(left + 30, y, panelWidth - 60, 44, 8, 8);
                }
                GameHud.drawText(dialog, font, labels[i], width / 2, y + 7, 0.8, panelWidth - 90);
            }
            GameHud.drawText(dialog, font, "ARROWS SELECT   ENTER CONFIRM   ESC CANCEL",
                    width / 2, top + 294, 0.45, panelWidth - 60);
        } finally {
            dialog.dispose();
        }
    }
}
