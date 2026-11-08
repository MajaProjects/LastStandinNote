import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class TitleScreen implements Screen {

    private final BufferedImage fondoInicio;
    private final BitmapFont font;
    private final SpriteAnimation windows;
    private final SpriteAnimation tux;
    private final SpriteAnimation kernel;
    private final BufferedImage monitor;
    private static final long CHARACTER_INTERVAL_MS = 5_000;
    private final long animationStart = System.nanoTime();

    public TitleScreen() {
        // load the start background through the shared image loader
        fondoInicio = ImageLoader.load(
                "/images/backgrounds/circuito_480x270.png"
        );

        // load the font atlas and its character configuration
        font = new BitmapFont("/images/font/font.txt");
        SpriteAtlas atlas = new SpriteAtlas("/images/sprites/sprites.txt");
        windows = atlas.getAnimation("windows", "idle");
        tux = atlas.getAnimation("tux", "idle");
        kernel = atlas.getAnimation("kernel", "idle");
        monitor = atlas.getAnimation("monitor", "versus").getFrame(0);
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        // draw the background across the available area
        g.drawImage(fondoInicio, 0, 0, width, height, null);
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRect(0, 0, width, height);

        // draw a larger title with a continuous size pulse
        double seconds = (System.nanoTime() - animationStart)
                / 1_000_000_000.0;
        long elapsedMs = (long) (seconds * 1000);
        // show one animated character at a time, cycling every five seconds
        int characterIndex = (int) ((elapsedMs / CHARACTER_INTERVAL_MS) % 3);
        SpriteAnimation character = switch (characterIndex) {
            case 0 -> windows;
            case 1 -> tux;
            default -> kernel;
        };
        BufferedImage frame = character.getFrame(elapsedMs % CHARACTER_INTERVAL_MS);
        if (characterIndex == 2) {
            // keep kernel seated on her monitor during her turn on the title screen
            int monitorSize = 192;
            int monitorTop = height - 120 - monitorSize;
            drawCharacter(g, monitor, width / 2, height - 120,
                    monitorSize / (double) monitor.getWidth());
            drawCharacter(g, frame, width / 2, monitorTop + 56, 1.0);
        } else {
            drawCharacter(g, frame, width / 2, height - 120, 2.0);
        }

        font.drawPulsingCentered(
                g,
                "LAST STANDIN NOTE",
                width / 2,
                90,
                2,
                1.15,
                seconds
        );

        // show the key used to open the main menu
        String prompt = GameText.display("PULSA ENTER");
        int promptX = (width - font.getTextWidth(prompt, 1)) / 2;

        font.draw(g, prompt, promptX, height - 90, 1);
        GameHud.drawText(g, font, "v" + AppVersion.get(), width - 90, height - 34, 0.65, 150);
    }

    private void drawCharacter(Graphics2D g, BufferedImage frame, int centerX, int bottom, double scale) {
        int width = (int) Math.round(frame.getWidth() * scale);
        int height = (int) Math.round(frame.getHeight() * scale);
        Graphics2D character = (Graphics2D) g.create();
        try {
            character.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            character.drawImage(frame, centerX - width / 2, bottom - height, width, height, null);
        } finally {
            character.dispose();
        }
    }
}
