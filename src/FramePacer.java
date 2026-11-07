import javax.swing.SwingUtilities;

final class FramePacer implements AutoCloseable {

    @FunctionalInterface
    interface Sleeper {
        void sleep(long milliseconds) throws InterruptedException;
    }

    private final Object lock = new Object();
    private final Runnable frameReady;
    private final Thread worker;
    private final Sleeper sleeper;
    private volatile long lastPaint = Long.MIN_VALUE;
    private volatile long deadline;
    private volatile boolean requested;
    private volatile boolean closed;
    private boolean deliveryPending;

    FramePacer() {
        frameReady = null;
        worker = null;
        sleeper = Thread::sleep;
    }

    FramePacer(Runnable frameReady) {
        this(frameReady, Thread::sleep);
    }

    FramePacer(Runnable frameReady, Sleeper sleeper) {
        this.frameReady = frameReady;
        this.sleeper = sleeper;
        worker = new Thread(this::run, "frame-pacer");
        worker.setDaemon(true);
        worker.start();
    }

    void recordPaint(long now) {
        lastPaint = now;
    }

    long delayNanos(int limit, long now) {
        if (limit == 0 || lastPaint == Long.MIN_VALUE) {
            return 0;
        }
        long interval = (1_000_000_000L + limit - 1) / limit;
        return Math.max(0, interval - (now - lastPaint));
    }

    void request(int limit) {
        long now = System.nanoTime();
        long target = now + delayNanos(limit, now);
        synchronized (lock) {
            if (closed) {
                return;
            }
            boolean earlier = requested && target < deadline;
            deadline = target;
            requested = true;
            lock.notifyAll();
            if (earlier && worker != null) {
                worker.interrupt();
            }
        }
    }

    private void run() {
        long sleepMargin = 250_000L;
        while (!closed) {
            try {
                long target;
                synchronized (lock) {
                    while (!closed && (!requested || deliveryPending)) {
                        lock.wait();
                    }
                    if (closed) {
                        return;
                    }
                    target = deadline;
                }
                while (!closed && requested && deadline == target) {
                    long remaining = target - System.nanoTime();
                    if (remaining <= 0) {
                        break;
                    }
                    if (remaining > sleepMargin + 1_000_000L) {
                        // learn oversleep so coarse platform timers cannot impose a hidden FPS cap
                        long milliseconds = (remaining - sleepMargin) / 1_000_000L;
                        long before = System.nanoTime();
                        sleeper.sleep(milliseconds);
                        long oversleep = Math.max(0,
                                System.nanoTime() - before - milliseconds * 1_000_000L);
                        long observedMargin = Math.min(20_000_000L, oversleep + 250_000L);
                        sleepMargin = Math.max(250_000L, (sleepMargin * 3 + observedMargin) / 4);
                    } else {
                        Thread.onSpinWait();
                    }
                }
                synchronized (lock) {
                    if (closed || !requested || deadline != target) {
                        continue;
                    }
                    requested = false;
                    deliveryPending = true;
                }
                // painting and every gameplay callback remain on the event dispatch thread
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (!closed) {
                            frameReady.run();
                        }
                    } finally {
                        synchronized (lock) {
                            deliveryPending = false;
                            lock.notifyAll();
                        }
                    }
                });
            } catch (InterruptedException ignored) {
                // a changed deadline or shutdown wakes the worker without delaying menu input
            }
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            closed = true;
            requested = false;
            lock.notifyAll();
        }
        if (worker != null) {
            worker.interrupt();
        }
    }
}
