import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class GameScreen implements Screen, AutoCloseable {

    private final SongChart chart;
    private final MusicPlayer music;
    private final BitmapFont font;
    private final BufferedImage background;
    private final BufferedImage monitor;

    private final SpriteAnimation playerIdle;
    private final SpriteAnimation opponentIdle;
    private final SpriteAnimation kernelIdle;

    private final boolean twoPlayers;
    private final String opponentLabel;

    // store the four held lanes for each player
    private final boolean[][] heldKeys = new boolean[2][4];

    private final NoteRenderer noteRenderer = new NoteRenderer(48, 12, 300);

    public GameScreen(
            String songFolder,
            String playerCharacter,
            String opponentCharacter,
            boolean twoPlayers
    ) {
        this.twoPlayers = twoPlayers;

        // load the selected song's settings and notes
        chart = SongChart.load(songFolder + "chart.txt");

        background = ImageLoader.load(
                "/images/backgrounds/fondo1_480x270.png"
        );
        font = new BitmapFont("/images/font/font.txt");

        SpriteAtlas atlas = new SpriteAtlas("/images/sprites/sprites.txt");

        // load the characters assigned on the selection screen
        playerIdle = atlas.getAnimation(playerCharacter, "idle");
        opponentIdle = atlas.getAnimation(opponentCharacter, "idle");
        kernelIdle = atlas.getAnimation("kernel", "idle");
        monitor = atlas.getAnimation("monitor", "versus").getFrame(0);

        opponentLabel = twoPlayers ? "PLAYER TWO" : "BOT";

        music = new MusicPlayer(songFolder + "instrumental.wav");

        System.out.println(
                "Loaded " + chart.name() + " with " + chart.notes().size() + " notes"
        );
    }

    public void start() {
        // start the song after the screen has been installed
        music.play();
    }

    public void setLanePressed(int voice, int lane, boolean pressed) {
        // ignore the second player's keyboard in single-player mode
        if (voice == 2 && !twoPlayers) {
            return;
        }

        heldKeys[voice - 1][lane] = pressed;
    }

    public void releaseKeys() {
        // clear all held lanes when the game loses window focus
        for (boolean[] playerKeys : heldKeys) {
            for (int lane = 0; lane < playerKeys.length; lane++) {
                playerKeys[lane] = false;
            }
        }
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        g.drawImage(background, 0, 0, width, height, null);

        // derive musical time from the audio position
        long songTimeMs = music.getPositionMs();

        double beat = Math.max(
                0.0,
                (songTimeMs - chart.beatOffsetMs()) * chart.bpm() / 60_000.0
        );

        drawLabel(g, chart.name(), width / 2, 20);

        int groundY = height - 52;

        drawIdle(g, playerIdle, beat, width / 4, groundY, 2);
        drawIdle(g, opponentIdle, beat, width * 3 / 4, groundY, 2);

        // enlarge the monitor while preserving its center
        int monitorSize = 192;
        int monitorCenterY = height - 232;
        int monitorX = width / 2 - monitorSize / 2;
        int monitorY = monitorCenterY - monitorSize / 2;

        g.drawImage(
                monitor,
                monitorX,
                monitorY,
                monitorSize,
                monitorSize,
                null
        );

        // position kernel on the monitor and draw her legs in front
        int kernelBottom = monitorY + 56;
        drawIdle(g, kernelIdle, beat, width / 2, kernelBottom, 1);

        drawLabel(g, "PLAYER ONE", width / 4, height - 42);
        drawLabel(g, opponentLabel, width * 3 / 4, height - 42);

        // share the audio clock and held inputs with the note renderer
        noteRenderer.render(
                g, chart.notes(), songTimeMs, width, height, heldKeys
        );
    }

    private void drawIdle(
            Graphics2D g,
            SpriteAnimation animation,
            double beat,
            int centerX,
            int bottom,
            int scale
    ) {
        // fit a complete idle animation into each musical beat
        long animationTimeMs = (long) (beat * animation.getDurationMs());
        BufferedImage frame = animation.getFrame(animationTimeMs);

        int width = frame.getWidth() * scale;
        int height = frame.getHeight() * scale;

        g.drawImage(
                frame,
                centerX - width / 2,
                bottom - height,
                width,
                height,
                null
        );
    }

    private void drawLabel(
            Graphics2D g,
            String text,
            int centerX,
            int y
    ) {
        int x = centerX - font.getTextWidth(text, 1) / 2;
        font.draw(g, text, x, y, 1);
    }

    @Override
    public void close() {
        // release the song's audio resources when leaving the stage
        music.close();
    }
}