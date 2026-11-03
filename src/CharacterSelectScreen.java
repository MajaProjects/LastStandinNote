import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

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

        long elapsedMs = (System.nanoTime() - animationStart) / 1_000_000;
        double seconds = elapsedMs / 1000.0;

        font.drawPulsingCentered(
                g, "PICK A CHARACTER", width / 2, 60, 2, 1.0, seconds
        );

        int songX = (width - font.getTextWidth(songName, 1)) / 2;
        font.draw(g, songName, songX, 150, 1);

        int characterY = height / 2 - 90;

// mark the selected character beside its name
        String tuxLabel = "TUX";
        String windowsLabel = "WINDOWS";

        if (selectedCharacter == 0) {
            tuxLabel = "* TUX *";
        } else {
            windowsLabel = "* WINDOWS *";
        }

// keep both idle animations running while choosing
        drawCharacter(
                g, tuxIdle.getFrame(elapsedMs),
                tuxLabel, width / 3, characterY
        );

        drawCharacter(
                g, windowsIdle.getFrame(elapsedMs),
                windowsLabel, width * 2 / 3, characterY
        );
        // show both players' lane keys along the bottom
        String controls = "P1 A S D F    P2 J K L Ñ";
        int controlsX = (width - font.getTextWidth(controls, 1)) / 2;

        font.draw(g, controls, controlsX, height - 42, 1);
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