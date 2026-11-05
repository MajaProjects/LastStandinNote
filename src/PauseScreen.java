import java.awt.Color;
import java.awt.Graphics2D;

public final class PauseScreen implements Screen {

    private final GameScreen game;
    private final BitmapFont font = new BitmapFont("/images/font/font.txt");
    private final String[] options = {"RESUME", "RESTART", "SONG SELECT"};
    private int selectedOption;
    private boolean resumeRequested;
    private String missingKeys = "";

    public PauseScreen(GameScreen game) {
        this.game = game;
    }

    public GameScreen getGame() {
        return game;
    }

    public String getSelectedOption() {
        return options[selectedOption];
    }

    public void moveSelection(int direction) {
        selectedOption = Math.floorMod(selectedOption + direction, options.length);
        cancelResume();
    }

    public void requestResume(String missingKeys) {
        selectedOption = 0;
        resumeRequested = true;
        this.missingKeys = missingKeys;
    }

    public boolean isResumeRequested() {
        return resumeRequested;
    }

    public void cancelResume() {
        resumeRequested = false;
        missingKeys = "";
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        // the game keeps drawing its frozen audio position under the menu
        game.render(g, width, height);
        Graphics2D menu = (Graphics2D) g.create();

        try {
            menu.setColor(new Color(0, 0, 0, 180));
            menu.fillRect(0, 0, width, height);

            int panelWidth = Math.min(560, width - 40);
            int panelHeight = 390;
            int left = (width - panelWidth) / 2;
            int top = (height - panelHeight) / 2;

            menu.setColor(new Color(12, 12, 20, 235));
            menu.fillRoundRect(left, top, panelWidth, panelHeight, 18, 18);
            menu.setColor(new Color(165, 95, 220));
            menu.drawRoundRect(left, top, panelWidth, panelHeight, 18, 18);

            drawCentered(menu, "PAUSED", width / 2, top + 25, 2);

            for (int i = 0; i < options.length; i++) {
                int y = top + 130 + i * 60;
                if (i == selectedOption) {
                    menu.setColor(new Color(65, 35, 85));
                    menu.fillRoundRect(left + 35, y - 8, panelWidth - 70, 46, 8, 8);
                }
                drawCentered(menu, options[i], width / 2, y, 1);
            }

            // wait for real held keys instead of granting automatic sustain hits
            String hint = resumeRequested && !missingKeys.isEmpty()
                    ? "HOLD " + missingKeys + " TO RESUME"
                    : "UP DOWN ENTER";
            drawCentered(menu, hint, width / 2, top + panelHeight - 55, 1);
        } finally {
            menu.dispose();
        }
    }

    private void drawCentered(Graphics2D g, String text, int centerX, int y, int scale) {
        int x = centerX - font.getTextWidth(text, scale) / 2;
        font.draw(g, text, x, y, scale);
    }
}
