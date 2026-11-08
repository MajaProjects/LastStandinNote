import java.awt.Color;
import java.awt.Graphics2D;

public final class ExitConfirmScreen implements Screen {

    private final Screen previousScreen;
    private final GameScreen game;
    private final boolean applicationExit;
    private final BitmapFont font = new BitmapFont("/images/font/font.txt");
    private int selectedOption;

    public ExitConfirmScreen(Screen previousScreen, GameScreen game, boolean applicationExit) {
        this.previousScreen = previousScreen;
        this.game = game;
        this.applicationExit = applicationExit;
    }

    public Screen getPreviousScreen() { return previousScreen; }
    public GameScreen getGame() { return game; }
    public boolean isApplicationExit() { return applicationExit; }
    public boolean isExitSelected() { return selectedOption == 1; }

    public void moveSelection(int direction) {
        selectedOption = Math.floorMod(selectedOption + direction, 2);
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        previousScreen.render(g, width, height);
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, width, height);
        int panelWidth = Math.min(700, width - 40);
        int left = (width - panelWidth) / 2;
        int top = (height - 270) / 2;
        g.setColor(new Color(15, 12, 24, 250));
        g.fillRoundRect(left, top, panelWidth, 270, 18, 18);
        g.setColor(new Color(185, 95, 235));
        g.drawRoundRect(left, top, panelWidth, 270, 18, 18);
        GameHud.drawText(g, font, applicationExit ? "QUIT GAME?" : "LEAVE SONG?",
                width / 2, top + 30, 1.5, panelWidth - 50);
        GameHud.drawText(g, font, game == null ? "RETURN TO YOUR GAME OR CONFIRM EXIT"
                        : "CURRENT SONG PROGRESS WILL BE LOST",
                width / 2, top + 104, 0.7, panelWidth - 40);
        String[] options = {"CONTINUE", applicationExit ? "QUIT GAME" : "LEAVE SONG"};
        int buttonWidth = (panelWidth - 90) / 2;
        for (int i = 0; i < options.length; i++) {
            int x = left + 30 + i * (buttonWidth + 30);
            RetroButton.draw(g, x, top + 160, buttonWidth, 46, i == selectedOption);
            GameHud.drawText(g, font, options[i], x + buttonWidth / 2,
                    top + 168, 0.9, buttonWidth - 20);
        }
        GameHud.drawText(g, font, "ARROWS SELECT   ENTER CONFIRM   ESC CANCEL",
                width / 2, top + 232, 0.55, panelWidth - 40);
    }
}
