import java.awt.Color;
import java.awt.Graphics2D;
import java.util.function.LongSupplier;

public final class CountdownScreen implements Screen {

    // match the supplied recording at twice its original tempo, without changing pitch
    private static final long[] CUE_TIMES_MS = {0, 540, 1085, 1640};
    private static final long SILENT_DURATION_MS = 2582;
    private static final String[] CUES = {"3", "2", "1", "GO"};

    private final GameScreen game;
    private final BitmapFont font = new BitmapFont("/images/font/font.txt");
    private final LongSupplier clock;
    private final long startNanos;
    private final MusicPlayer sound;
    private final boolean ownsSound;
    private boolean stopped;

    public CountdownScreen(GameScreen game) {
        this(game, new MusicPlayer("/audio/countdown.wav", AudioSettings.Channel.EFFECTS), true);
    }

    public CountdownScreen(GameScreen game, MusicPlayer sound) {
        this(game, sound, false);
    }

    private CountdownScreen(GameScreen game, MusicPlayer sound, boolean ownsSound) {
        this.game = game;
        this.sound = sound;
        this.ownsSound = ownsSound;
        clock = System::nanoTime;
        startNanos = clock.getAsLong();
        sound.play();
    }

    CountdownScreen(GameScreen game, LongSupplier clock) {
        this.game = game;
        this.clock = clock;
        sound = null;
        ownsSound = false;
        startNanos = clock.getAsLong();
    }

    public GameScreen getGame() {
        return game;
    }

    private long getElapsedNanos() {
        if (sound != null) {
            // use the recording's own position so sound and animation cannot drift apart
            return sound.getPositionMs() * 1_000_000L;
        }
        return Math.max(0, clock.getAsLong() - startNanos);
    }

    public boolean isFinished() {
        return !stopped && (sound == null ? getElapsedNanos() >= SILENT_DURATION_MS * 1_000_000L
                : sound.isFinished());
    }

    public void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        if (sound != null) {
            sound.stop();
            if (ownsSound) {
                sound.close();
            }
        }
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        game.render(g, width, height);
        long elapsedMs = getElapsedNanos() / 1_000_000L;
        int step = 0;
        while (step + 1 < CUES.length && elapsedMs >= CUE_TIMES_MS[step + 1]) {
            step++;
        }
        double progress = Math.min((elapsedMs - CUE_TIMES_MS[step]) / 180.0, 1.0);
        double pulse = 1.0 + 0.18 * (1.0 - progress);

        Graphics2D overlay = (Graphics2D) g.create();
        try {
            // dim the frozen stage while keeping the note field visible
            overlay.setColor(new Color(0, 0, 0, 125));
            overlay.fillRect(0, 0, width, height);

            int panelWidth = Math.min(440, width - 40);
            int panelHeight = 260;
            int left = (width - panelWidth) / 2;
            int top = (height - panelHeight) / 2;

            overlay.setColor(new Color(12, 12, 20, 235));
            overlay.fillRoundRect(left, top, panelWidth, panelHeight, 18, 18);
            overlay.setColor(new Color(165, 95, 220));
            overlay.drawRoundRect(left, top, panelWidth, panelHeight, 18, 18);
            drawCentered(overlay, "GET READY", width / 2, top + 22, 1);

            // restart the small pop animation for every countdown cue
            Graphics2D cue = (Graphics2D) overlay.create();
            try {
                cue.translate(width / 2, top + 65);
                cue.scale(pulse, pulse);
                drawCentered(cue, CUES[step], 0, 0, 3);
            } finally {
                cue.dispose();
            }

            int segmentWidth = 32;
            int gap = 10;
            int meterLeft = width / 2 - (CUES.length * (segmentWidth + gap) - gap) / 2;
            for (int i = 0; i < CUES.length; i++) {
                overlay.setColor(i <= step
                        ? new Color(170, 80, 230) : new Color(45, 35, 55));
                overlay.fillRect(meterLeft + i * (segmentWidth + gap), top + 190,
                        segmentWidth, 8);
            }
            drawCentered(overlay, "P TO PAUSE", width / 2, top + 217, 1);
        } finally {
            overlay.dispose();
        }
    }

    private void drawCentered(Graphics2D g, String text, int centerX, int y, int scale) {
        text = GameText.display(text);
        int x = centerX - font.getTextWidth(text, scale) / 2;
        font.draw(g, text, x, y, scale);
    }
}
