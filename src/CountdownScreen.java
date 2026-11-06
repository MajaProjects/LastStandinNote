import java.awt.Color;
import java.awt.Graphics2D;
import java.util.function.LongSupplier;

public final class CountdownScreen implements Screen {

    // show each cue for 600 ms while the song remains frozen
    private static final long STEP_NANOS = 600_000_000L;
    private static final String[] CUES = {"3", "2", "1", "GO"};

    private final GameScreen game;
    private final BitmapFont font = new BitmapFont("/images/font/font.txt");
    private final LongSupplier clock;
    private final long startNanos;

    public CountdownScreen(GameScreen game) {
        this(game, System::nanoTime);
    }

    CountdownScreen(GameScreen game, LongSupplier clock) {
        this.game = game;
        this.clock = clock;
        startNanos = clock.getAsLong();
    }

    public GameScreen getGame() {
        return game;
    }

    private long getElapsedNanos() {
        // use elapsed wall time only while the audio clock is stopped
        return Math.max(0, clock.getAsLong() - startNanos);
    }

    public boolean isFinished() {
        return getElapsedNanos() >= STEP_NANOS * CUES.length;
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        game.render(g, width, height);
        long elapsed = getElapsedNanos();
        int step = (int) Math.min(elapsed / STEP_NANOS, CUES.length - 1);
        double progress = Math.min((elapsed % STEP_NANOS) / 180_000_000.0, 1.0);
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
        int x = centerX - font.getTextWidth(text, scale) / 2;
        font.draw(g, text, x, y, scale);
    }
}