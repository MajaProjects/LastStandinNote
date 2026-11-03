import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Color;
public class CharacterSelectScreen implements Screen {

    private final String songName;
    private final BufferedImage background;
    private final BitmapFont font;
    private final SpriteAnimation tuxIdle;
    private final SpriteAnimation windowsIdle;
    private final long animationStart = System.nanoTime();

    // start with tux selected
    private int selectedCharacter = 0;

    public CharacterSelectScreen(String songName) {
        this.songName = songName;

        background = ImageLoader.load(
                "/images/backgrounds/circuito_480x270.png"
        );
        font = new BitmapFont("/images/font/font.txt");

        // load both animations from the same sprite sheet
        SpriteAtlas atlas = new SpriteAtlas("/images/sprites/sprites.txt");
        tuxIdle = atlas.getAnimation("tux", "idle");
        windowsIdle = atlas.getAnimation("windows", "idle");
    }
    public void moveSelection(int direction) {
        // wrap between tux at index 0 and windows at index 1
        selectedCharacter = Math.floorMod(selectedCharacter + direction, 2);
    }
    @Override
    public void render(Graphics2D g, int width, int height) {
        g.drawImage(background, 0, 0, width, height, null);

        // dim the background so the characters and labels stand out
        g.setColor(new Color(0, 0, 0, 110));
        g.fillRect(0, 0, width, height);

        long elapsedMs = (System.nanoTime() - animationStart) / 1_000_000;
        double seconds = elapsedMs / 1000.0;

        font.drawPulsingCentered(
                g, "PICK A CHARACTER", width / 2, 60, 2, 1.0, seconds
        );

        int songX = (width - font.getTextWidth(songName, 1)) / 2;
        font.draw(g, songName, songX, 150, 1);

        String tuxLabel = "TUX";
        String windowsLabel = "WINDOWS";

        if (selectedCharacter == 0) {
            tuxLabel = "* TUX *";
        } else {
            windowsLabel = "* WINDOWS *";
        }

        // leave room below the names for the control bar
        int characterY = height / 2 - 110;

        drawCharacter(
                g, tuxIdle.getFrame(elapsedMs),
                tuxLabel, width / 3, characterY
        );

        drawCharacter(
                g, windowsIdle.getFrame(elapsedMs),
                windowsLabel, width * 2 / 3, characterY
        );

        // keep the controls readable over the circuit background
        int controlsTop = height - 64;

        g.setColor(new Color(12, 12, 20));
        g.fillRect(0, controlsTop, width, 64);

        g.setColor(new Color(95, 75, 120));
        g.drawLine(0, controlsTop, width - 1, controlsTop);

        // align each key group with the character above it
        drawControls(g, "ASDF", width / 3, controlsTop + 12);
        drawControls(g, "JKLÑ", width * 2 / 3, controlsTop + 12);
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

        // center the four keys as one group
        int groupWidth = keys.length() * keyWidth + (keys.length() - 1) * gap;
        int left = centerX - groupWidth / 2;

        for (int i = 0; i < keys.length(); i++) {
            int x = left + i * (keyWidth + gap);

            // draw the key background and border
            g.setColor(new Color(30, 30, 45));
            g.fillRect(x, top, keyWidth, keyHeight);

            g.setColor(new Color(120, 95, 155));
            g.drawRect(x, top, keyWidth - 1, keyHeight - 1);

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
        // draw the character at twice its original size
        int width = frame.getWidth() * 2;
        int height = frame.getHeight() * 2;
        int left = centerX - width / 2;

        g.drawImage(frame, left, top, width, height, null);

        int nameX = centerX - font.getTextWidth(name, 1) / 2;
        font.draw(g, name, nameX, top + height + 12, 1);
    }
}