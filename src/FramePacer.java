final class FramePacer {

    private long lastPaint = Long.MIN_VALUE;

    void recordPaint(long now) {
        lastPaint = now;
    }

    long delayNanos(int limit, long now) {
        if (limit == 0 || lastPaint == Long.MIN_VALUE) {
            return 0;
        }
        // round upward so fractional frame periods cannot exceed the requested cap
        long interval = (1_000_000_000L + limit - 1) / limit;
        return Math.max(0, interval - (now - lastPaint));
    }
}
