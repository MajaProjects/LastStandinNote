import java.util.function.LongSupplier;

final class PlaybackClock {

    private final long durationNanos;
    private final LongSupplier clock;
    private long savedNanos;
    private long startNanos;
    private boolean running;
    private boolean paused;
    private boolean playingOnce;

    PlaybackClock(long durationNanos) {
        this(durationNanos, System::nanoTime);
    }

    PlaybackClock(long durationNanos, LongSupplier clock) {
        if (durationNanos < 0) throw new IllegalArgumentException("Invalid playback duration");
        this.durationNanos = durationNanos;
        this.clock = java.util.Objects.requireNonNull(clock);
    }

    synchronized void play(boolean loop) {
        savedNanos = 0;
        startNanos = clock.getAsLong();
        running = true;
        paused = false;
        playingOnce = !loop;
    }

    synchronized long getPositionNanos() {
        long position = savedNanos;
        if (running) position += Math.max(0, clock.getAsLong() - startNanos);
        if (!playingOnce && durationNanos > 0) return position % durationNanos;
        return Math.min(durationNanos, position);
    }

    long getDurationMs() { return durationNanos / 1_000_000L; }

    synchronized boolean isFinished() {
        return playingOnce && running && getPositionNanos() >= durationNanos;
    }

    synchronized void pause() {
        if (!running) return;
        savedNanos = getPositionNanos();
        running = false;
        paused = true;
    }

    synchronized void resume() {
        if (!paused) return;
        startNanos = clock.getAsLong();
        running = true;
        paused = false;
    }

    synchronized void stop() {
        savedNanos = getPositionNanos();
        running = false;
        paused = false;
    }
}
