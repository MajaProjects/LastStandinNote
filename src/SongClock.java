import java.util.function.LongSupplier;

// share one calibrated time between input, notes, bot and character feedback
final class SongClock {
    private final int offsetMs;
    private final LongSupplier clock;
    private boolean paused = true;
    private long frozenTimeMs;
    private long tailStartNanos = -1;
    private long savedTailNanos;

    SongClock(int offsetMs) { this(offsetMs, System::nanoTime); }

    SongClock(int offsetMs, LongSupplier clock) {
        this.offsetMs = TimingSettings.clamp(offsetMs);
        this.clock = clock;
        frozenTimeMs = this.offsetMs;
    }

    void start() {
        paused = false;
        savedTailNanos = 0;
        tailStartNanos = -1;
    }

    long timeMs(long audioTimeMs, boolean audioFinished) {
        if (paused) return frozenTimeMs;
        long tailNanos = savedTailNanos;
        if (audioFinished) {
            if (tailStartNanos < 0) tailStartNanos = clock.getAsLong();
            tailNanos += Math.max(0, clock.getAsLong() - tailStartNanos);
        }
        // allow the final calibrated notes to finish after the audio device stops
        return audioTimeMs + offsetMs + tailNanos / 1_000_000L;
    }

    void pause(long audioTimeMs, boolean audioFinished) {
        if (paused) return;
        frozenTimeMs = timeMs(audioTimeMs, audioFinished);
        if (tailStartNanos >= 0) {
            savedTailNanos += Math.max(0, clock.getAsLong() - tailStartNanos);
            tailStartNanos = -1;
        }
        paused = true;
    }

    void resume() { paused = false; }
}
