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
    private final String opponentLabel;
    // use 48-pixel arrows, a 12-pixel gap and 300 pixels of movement per second
    private final NoteRenderer noteRenderer = new NoteRenderer(48, 12, 300);

    public GameScreen(
            String songFolder,
            String playerCharacter,
            String opponentCharacter,
            boolean twoPlayers
    ) {
        // load the selected song's settings and notes
        chart = SongChart.load(songFolder + "chart.txt");

        background = ImageLoader.load(
                "/images/backgrounds/fondo1_480x270.png"
        );
        font = new BitmapFont("/images/font/font.txt");

        SpriteAtlas atlas = new SpriteAtlas("/images/sprites/sprites.txt");

        // use the characters assigned on the selection screen
        playerIdle = atlas.getAnimation(playerCharacter, "idle");
        opponentIdle = atlas.getAnimation(opponentCharacter, "idle");
        kernelIdle = atlas.getAnimation("kernel", "idle");
        monitor = atlas.getAnimation("monitor", "versus").getFrame(0);

        opponentLabel = twoPlayers ? "PLAYER TWO" : "BOT";

        // load audio after the other resources are ready
        music = new MusicPlayer(songFolder + "instrumental.wav");

        System.out.println(
                "Loaded " + chart.name() + " with " + chart.notes().size() + " notes"
        );
        // check the sustained notes loaded from the chart
        for (Note note : chart.notes()) {
            if (note.durationMs() > 0) {
                System.out.println(
                        "hold time_ms=" + note.timeMs()
                                + " duration_ms=" + note.durationMs()
                );
            }
        }
    }

    public void start() {
        // begin playback after the game screen has been installed
        music.play();
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        g.drawImage(background, 0, 0, width, height, null);

        // calculate musical time from the audio position and chart settings
        long songTimeMs = music.getPositionMs();
        double beat = Math.max(
                0.0,
                (songTimeMs - chart.beatOffsetMs()) * chart.bpm() / 60_000.0
        );

        drawLabel(g, chart.name(), width / 2, 20);

        int groundY = height - 52;

        drawIdle(g, playerIdle, beat, width / 4, groundY, 2);
        drawIdle(g, opponentIdle, beat, width * 3 / 4, groundY, 2);
        // draw the chart notes using the same audio clock as the characters
        noteRenderer.render(g, chart.notes(), songTimeMs, width, height);


        // place the monitor above kernel in the middle of the stage
        g.drawImage(
                monitor, width / 2 - 48, height - 280, 96, 96, null
        );

        // enlarge the monitor while keeping its current center
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

    // account for transparent margins and place kernel on the top edge
        int kernelBottom = monitorY + 56;

    // draw kernel last so her legs appear in front of the monitor
        drawIdle(g, kernelIdle, beat, width / 2, kernelBottom, 1);

        drawLabel(g, "PLAYER ONE", width / 4, height - 42);
        drawLabel(g, opponentLabel, width * 3 / 4, height - 42);
    }

    private void drawIdle(
            Graphics2D g,
            SpriteAnimation animation,
            double beat,
            int centerX,
            int bottom,
            int scale
    ) {
        // fit one complete idle cycle into each musical beat
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
        // stop playback and release the song's audio channel
        music.close();
    }
}