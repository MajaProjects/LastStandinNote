import javax.sound.sampled.*;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongSupplier;

public final class VocalPlayer implements AutoCloseable {

    private static final int FRAME_BYTES = 4;
    private static final int BLOCK_FRAMES = 256;
    private static final int MAX_QUEUED_FRAMES = 512;
    private final Object lock = new Object();
    private final VocalMixer mixer;
    private final LongSupplier songClock;
    private final SourceDataLine line;
    private final float[] mixed = new float[BLOCK_FRAMES * 2];
    private final byte[] pcm = new byte[BLOCK_FRAMES * FRAME_BYTES];
    private final Thread worker;
    private boolean playing;
    private boolean closed;
    private volatile RuntimeException failure;

    public static VocalPlayer load(String folder, List<Note> notes, LongSupplier songClock) {
        VocalBank bank = VocalBank.load(folder);
        if (bank == null) {
            return new VocalPlayer(null, songClock, null, false);
        }

        VocalMixer mixer = new VocalMixer(bank, notes);
        SourceDataLine line = null;
        try {
            AudioFormat format = new AudioFormat(VocalBank.SAMPLE_RATE, 16, 2, true, false);
            line = AudioSystem.getSourceDataLine(format);
            line.open(format, 1024 * FRAME_BYTES);
            return new VocalPlayer(mixer, songClock, line, true);
        } catch (LineUnavailableException | IllegalArgumentException error) {
            if (line != null) {
                line.close();
            }
            throw new IllegalStateException("Could not open character vocal audio", error);
        }
    }

    VocalPlayer(VocalMixer mixer, LongSupplier songClock, SourceDataLine line, boolean threaded) {
        this.mixer = mixer;
        this.songClock = songClock;
        this.line = line;
        worker = threaded ? new Thread(this::run, "character-vocals") : null;
        if (worker != null) {
            worker.setDaemon(true);
            worker.start();
        }
    }

    public void startNote(int index, long songTimeMs) {
        synchronized (lock) {
            checkFailure();
            if (!closed && mixer != null) {
                mixer.startNote(index, songTimeMs);
            }
        }
    }

    public void setHeld(int index, boolean held, long songTimeMs) {
        synchronized (lock) {
            if (!closed && mixer != null) {
                mixer.setHeld(index, held, songTimeMs);
            }
        }
    }

    public void stopNote(int index, long songTimeMs) {
        synchronized (lock) {
            if (!closed && mixer != null) {
                mixer.stopNote(index, songTimeMs);
            }
        }
    }

    public void reset() {
        synchronized (lock) {
            if (!closed && mixer != null) {
                mixer.reset();
                line.flush();
            }
        }
    }

    public void resume() {
        synchronized (lock) {
            checkFailure();
            if (!closed && line != null) {
                playing = true;
                line.start();
                lock.notifyAll();
            }
        }
    }

    public void pause() {
        synchronized (lock) {
            playing = false;
            if (!closed && line != null) {
                // discard queued vocals so none leak into pause or countdown
                line.stop();
                line.flush();
            }
        }
    }

    private void run() {
        try {
            while (true) {
                synchronized (lock) {
                    while (!closed && !playing) {
                        lock.wait();
                    }
                    if (closed) {
                        return;
                    }
                }
                pump();
                LockSupport.parkNanos(1_000_000L);
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException error) {
            failure = error;
            pause();
        }
    }

    void pump() {
        synchronized (lock) {
            if (closed || !playing || line == null) {
                return;
            }

            int available = line.available();
            int queuedFrames = Math.max(0, (line.getBufferSize() - available) / FRAME_BYTES);
            int frames = Math.min(BLOCK_FRAMES,
                    Math.min(available / FRAME_BYTES, MAX_QUEUED_FRAMES - queuedFrames));
            if (frames <= 0) {
                return;
            }

            // anchor each small buffer to the instrumental's playback position
            double startMs = songClock.getAsLong() + queuedFrames * 1000.0 / VocalBank.SAMPLE_RATE;
            mixer.render(startMs, mixed, frames);
            for (int i = 0; i < frames * 2; i++) {
                double value = Math.max(-1.0, Math.min(1.0, mixed[i]));
                int sample = (int) Math.round(value * 32767);
                pcm[2 * i] = (byte) sample;
                pcm[2 * i + 1] = (byte) (sample >> 8);
            }

            // this write cannot block because it never exceeds available bytes
            line.write(pcm, 0, frames * FRAME_BYTES);
        }
    }

    private void checkFailure() {
        if (failure != null) {
            throw new IllegalStateException("Character vocal playback failed", failure);
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            playing = false;
            if (line != null) {
                line.stop();
                line.flush();
                line.close();
            }
            lock.notifyAll();
        }
        if (worker != null) {
            worker.interrupt();
        }
    }
}