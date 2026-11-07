import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Locale;
import java.util.function.LongSupplier;

public final class GameHud {

    private final BitmapFont font = new BitmapFont("/images/font/font.txt");
    private final LongSupplier clock;
    private long windowStart = -1;
    private int frames;
    private int fps = -1;

    public GameHud() {
        this(System::nanoTime);
    }

    GameHud(LongSupplier clock) {
        this.clock = clock;
    }

    public void recordPaint() {
        long now = clock.getAsLong();
        if (windowStart == -1) {
            windowStart = now;
            return;
        }
        frames++;
        long elapsed = now - windowStart;
        if (elapsed >= 500_000_000L) {
            // measure completed paint intervals rather than counting timer callbacks
            fps = (int) Math.round(frames * 1_000_000_000.0 / elapsed);
            frames = 0;
            windowStart = now;
        }
    }

    public void drawFps(Graphics2D g, int width) {
        String label = fps < 0 ? "FPS --" : "FPS " + fps;
        double scale = 0.55;
        int textWidth = (int) Math.ceil(font.getTextWidth(label, 1) * scale);
        int left = width - textWidth - 22;
        g.setColor(new Color(12, 12, 20, 225));
        g.fillRoundRect(left - 7, 7, textWidth + 14, 28, 6, 6);
        drawText(g, font, label, left + textWidth / 2, 10, scale, textWidth + 2);
    }

    public static void drawStats(Graphics2D g, BitmapFont font, NoteJudge judge,
                                 int voice, int centerX, int top, int maxWidth) {
        NoteJudge.Statistics stats = judge.getStatisticsForVoice(voice);
        String accuracy = stats.hitNotes() + stats.miss() == 0 ? "--"
                : formatAccuracy(judge.getPerformanceForVoice(voice, false));
        String text = "SCORE " + stats.score() + "  ACC " + accuracy
                + "  COMBO " + judge.getComboForVoice(voice) + "  MISSES " + stats.miss();
        drawText(g, font, text, centerX, top, 0.55, maxWidth);
    }

    private static String formatAccuracy(double percentage) {
        double displayed = Math.round(percentage * 10.0) / 10.0;
        if (percentage < 100.0 - 0.000000001) {
            displayed = Math.min(displayed, 99.9);
        }
        return String.format(Locale.ROOT, "%.1f%%", displayed);
    }

    public static void drawText(Graphics2D g, BitmapFont font, String text,
                                int centerX, int top, double preferred, int maxWidth) {
        text = GameText.display(text);
        // draw the missing decimal point without changing the existing font atlas
        int dots = (int) text.chars().filter(character -> character == '.').count();
        int width = font.getTextWidth(text.replace(".", ""), 1) + dots * 6;
        double scale = Math.min(preferred, maxWidth / (double) Math.max(1, width));
        Graphics2D copy = (Graphics2D) g.create();
        try {
            copy.translate(centerX, top);
            copy.scale(scale, scale);
            int cursor = -width / 2;
            String[] parts = text.split("\\.", -1);
            for (int i = 0; i < parts.length; i++) {
                font.drawAligned(copy, parts[i], cursor, 0, 1);
                cursor += font.getTextWidth(parts[i], 1);
                if (i + 1 < parts.length) {
                    copy.setColor(new Color(195, 65, 245));
                    copy.fillRect(cursor + 1, 27, 3, 3);
                    cursor += 6;
                }
            }
        } finally {
            copy.dispose();
        }
    }
}
