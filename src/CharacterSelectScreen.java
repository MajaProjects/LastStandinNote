import java.awt.*;
import java.awt.image.BufferedImage;

public class CharacterSelectScreen implements Screen {

    private final String songName;
    private final boolean twoPlayers;
    private final BufferedImage background;
    private final BitmapFont font;
    private final SpriteAnimation tuxIdle;
    private final SpriteAnimation windowsIdle;
    private final long animationStart = System.nanoTime();

    // start with tux selected for the first player
    private int selectedCharacter = 0;

    public CharacterSelectScreen(String songName, boolean twoPlayers) {
        this.songName = songName;
        this.twoPlayers = twoPlayers;

        background = ImageLoader.load(
                "/images/backgrounds/circuito_480x270.png"
        );
        font = new BitmapFont("/images/font/font.txt");

        // load both idle animations from the shared sprite sheet
        SpriteAtlas atlas = new SpriteAtlas("/images/sprites/sprites.txt");
        tuxIdle = atlas.getAnimation("tux", "idle");
        windowsIdle = atlas.getAnimation("windows", "idle");
    }

    public void moveSelection(int direction) {
        // wrap between tux at index 0 and windows at index 1
        selectedCharacter = Math.floorMod(selectedCharacter + direction, 2);
    }

    public String getSelectedCharacter() {
        // return the identifier used in the sprite configuration
        if (selectedCharacter == 0) {
            return "tux";
        }

        return "windows";
    }

    public String getOtherCharacter() {
        // assign the remaining character to the other participant
        if (selectedCharacter == 0) {
            return "windows";
        }

        return "tux";
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        g.drawImage(background, 0, 0, width, height, null);

        // darken the background to improve readability
        g.setColor(new Color(0, 0, 0, 110));
        g.fillRect(0, 0, width, height);

        // choose animation frames using the elapsed menu time
        long elapsedMs = (System.nanoTime() - animationStart) / 1_000_000;
        double seconds = elapsedMs / 1000.0;

        font.drawPulsingCentered(
                g, "PICK A CHARACTER", width / 2, 60, 2, 1.0, seconds
        );

        int songX = (width - font.getTextWidth(songName, 1)) / 2;
        font.draw(g, songName, songX, 150, 1);

        String tuxLabel = "TUX";
        String windowsLabel = "WINDOWS";

        // mark the character selected by the first player
        if (selectedCharacter == 0) {
            tuxLabel = "* TUX *";
        } else {
            windowsLabel = "* WINDOWS *";
        }

        int characterY = height / 2 - 110;

        drawCharacter(
                g, tuxIdle.getFrame(elapsedMs),
                tuxLabel, width / 3, characterY
        );

        drawCharacter(
                g, windowsIdle.getFrame(elapsedMs),
                windowsLabel, width * 2 / 3, characterY
        );

        // separate the controls from the background
        int controlsTop = height - 64;

        g.setColor(new Color(12, 12, 20));
        g.fillRect(0, controlsTop, width, 64);

        g.setColor(new Color(95, 75, 120));
        g.drawLine(0, controlsTop, width - 1, controlsTop);

        int firstPlayerX = width / 3;
        int secondPlayerX = width * 2 / 3;

        // move the controls with the character assignment
        if (selectedCharacter == 1) {
            firstPlayerX = width * 2 / 3;
            secondPlayerX = width / 3;
        }

        drawControls(g, "ASDF", firstPlayerX, controlsTop + 12);

        // the second participant uses the keyboard in local multiplayer
        if (twoPlayers) {
            drawControls(g, "JKLÑ", secondPlayerX, controlsTop + 12);
        }
    }

    private void drawControls(
            Graphics2D g,
            String keys,
            int centerX,
            int top
    ) {
        int keyWidth = 36;
        int keyHeight = 40;
        int gap = 8;

        // center the complete group below its character
        int groupWidth = keys.length() * keyWidth + (keys.length() - 1) * gap;
        int left = centerX - groupWidth / 2;

        for (int i = 0; i < keys.length(); i++) {
            int x = left + i * (keyWidth + gap);

            g.setColor(new Color(30, 30, 45));
            g.fillRect(x, top, keyWidth, keyHeight);

            g.setColor(new Color(120, 95, 155));
            g.drawRect(x, top, keyWidth - 1, keyHeight - 1);

            // center the letter inside its key box
            String key = String.valueOf(keys.charAt(i));
            int textX = x + (keyWidth - font.getTextWidth(key, 1)) / 2;

            font.draw(g, key, textX, top + 4, 1);
        }
    }

    private void drawCharacter(
            Graphics2D g,
            BufferedImage frame,
            String name,
            int centerX,
            int top
    ) {
        // preserve the sprite proportions at twice its original size
        int width = frame.getWidth() * 2;
        int height = frame.getHeight() * 2;
        int left = centerX - width / 2;

        g.drawImage(frame, left, top, width, height, null);

        int nameX = centerX - font.getTextWidth(name, 1) / 2;
        font.draw(g, name, nameX, top + height + 12, 1);
    }
}