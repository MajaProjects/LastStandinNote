import javax.sound.sampled.*;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongSupplier;

public final class SongPlayer implements AutoCloseable {

    private static final int FRAME_BYTES = 4;
    private static final int BLOCK_FRAMES = 1024;
    // keep about 35 ms ready instead of relying on one millisecond thread wakeups
    private static final int MAX_QUEUED_FRAMES = 1536;
    private final Object lock = new Object();
    private final PcmTrack instrumental;
    private final PcmTrack[] voices;
    private final long[] voiceOffsets;
    private final double masterGain;
    private final VocalGate[] gates = {new VocalGate(), new VocalGate()};
    private final SourceDataLine line;
    private final PlaybackClock silentClock;
    private final Thread worker;
    private final byte[] block = new byte[BLOCK_FRAMES * FRAME_BYTES];
    private long baseSongFrame;
    private long baseLineFrame;
    private long writeFrame;
    private boolean started;
    private boolean paused = true;
    private boolean closed;
    private RuntimeException failure;

    public static SongPlayer load(String folder) {
        long[] offsets = {0, 0};
        double gain = 0.75;
        String config = folder + "vocals/playback.txt";
        boolean expectsVocals = SongPlayer.class.getResource(config) != null;
        if (expectsVocals) {
            Map<String, String> settings;
            try {
                settings = TextLoader.loadSettings(config);
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("Replace " + config
                        + " with the recorded-vocal playback settings: master_gain, "
                        + "voice1_offset_ms and voice2_offset_ms. " + error.getMessage(), error);
            }
            gain = readSetting(settings, "master_gain", 0, 1);
            offsets[0] = toFrames(readSetting(settings, "voice1_offset_ms", -60_000, 60_000));
            offsets[1] = toFrames(readSetting(settings, "voice2_offset_ms", -60_000, 60_000));
        }

        validateVocalFiles(folder, expectsVocals);

        // validate small settings before reading the large audio files
        PcmTrack instrumental = PcmTrack.load(folder + "instrumental.wav", false);
        PcmTrack[] voices = {
                PcmTrack.load(folder + "vocals/voice1.wav", true),
                PcmTrack.load(folder + "vocals/voice2.wav", true)
        };
        if ((voices[0] == null) != (voices[1] == null)) {
            throw new IllegalArgumentException("Both original vocal tracks are required in " + folder + "vocals/");
        }

        AudioFormat format = new AudioFormat(PcmTrack.SAMPLE_RATE, 16, 2, true, false);
        SourceDataLine output = AudioOutput.openSongLine(format, 4096 * FRAME_BYTES);
        return new SongPlayer(instrumental, voices, offsets, gain, output, true);
    }

    static void validateVocalFiles(String folder, boolean expectsVocals) {
        String first = folder + "vocals/voice1.wav";
        String second = folder + "vocals/voice2.wav";
        boolean hasFirst = SongPlayer.class.getResource(first) != null;
        boolean hasSecond = SongPlayer.class.getResource(second) != null;
        if ((expectsVocals && (!hasFirst || !hasSecond)) || hasFirst != hasSecond) {
            StringBuilder missing = new StringBuilder("Missing recorded vocal files:");
            if (!hasFirst) {
                missing.append('\n').append(first);
            }
            if (!hasSecond) {
                missing.append('\n').append(second);
            }
            missing.append("\nCheck the exact folder, filename and .wav extension.");
            throw new IllegalArgumentException(missing.toString());
        }
    }

    SongPlayer(PcmTrack instrumental, PcmTrack[] voices, long[] offsets,
               double masterGain, SourceDataLine line, boolean threaded) {
        this(instrumental, voices, offsets, masterGain, line, threaded, System::nanoTime);
    }

    SongPlayer(PcmTrack instrumental, PcmTrack[] voices, long[] offsets,
               double masterGain, SourceDataLine line, boolean threaded, LongSupplier clock) {
        this.instrumental = instrumental;
        this.voices = voices.clone();
        voiceOffsets = offsets.clone();
        this.masterGain = masterGain;
        this.line = line;
        long durationNanos = (long) Math.ceil(instrumental.frameCount() * 1_000_000_000.0 / PcmTrack.SAMPLE_RATE);
        silentClock = line == null ? new PlaybackClock(durationNanos, clock) : null;
        worker = threaded && line != null ? new Thread(this::run, "song-audio") : null;
        if (worker != null) {
            worker.setDaemon(true);
            worker.start();
        }
    }

    public void play() {
        synchronized (lock) {
            checkUsable();
            if (line != null) {
                line.stop();
                line.flush();
                baseLineFrame = line.getLongFramePosition();
            }
            baseSongFrame = 0;
            writeFrame = 0;
            for (VocalGate gate : gates) {
                gate.reset();
            }
            started = true;
            paused = false;
            // prepare audible frames before the device starts consuming them
            if (line == null) {
                silentClock.play(false);
            } else {
                pump();
                line.start();
            }
            lock.notifyAll();
        }
    }

    public long getPositionMs() {
        synchronized (lock) {
            return getSongFrame() * 1000 / PcmTrack.SAMPLE_RATE;
        }
    }

    private long getSongFrame() {
        if (!started || paused || closed) {
            return baseSongFrame;
        }
        if (line == null) {
            // advance silent songs with monotonic time while preserving the audio path when available
            if (silentClock.isFinished()) return instrumental.frameCount();
            return Math.min(instrumental.frameCount(),
                    (long) (silentClock.getPositionNanos() * (PcmTrack.SAMPLE_RATE / 1_000_000_000.0)));
        }
        long played = Math.max(0, line.getLongFramePosition() - baseLineFrame);
        return Math.min(instrumental.frameCount(), baseSongFrame + played);
    }

    public boolean isFinished() {
        synchronized (lock) {
            return started && !paused && !closed && getSongFrame() >= instrumental.frameCount();
        }
    }

    public boolean isSilent() { return line == null; }

    public void pause() {
        synchronized (lock) {
            if (paused || closed) {
                return;
            }
            if (line == null) {
                silentClock.pause();
                baseSongFrame = getSongFrame();
                paused = true;
                return;
            }
            line.stop();
            long position = getSongFrame();
            line.flush();
            baseSongFrame = position;
            baseLineFrame = line.getLongFramePosition();
            writeFrame = position;
            paused = true;
        }
    }

    public void resume() {
        synchronized (lock) {
            checkUsable();
            if (!started || !paused) {
                return;
            }
            if (line == null) {
                silentClock.resume();
                paused = false;
                return;
            }
            baseLineFrame = line.getLongFramePosition();
            writeFrame = baseSongFrame;
            paused = false;
            if (baseSongFrame < instrumental.frameCount()) {
                // resume from the saved frame with the same protection against underruns
                pump();
                line.start();
                lock.notifyAll();
            }
        }
    }

    public void setVoiceAudible(int voice, boolean audible, long songTimeMs) {
        synchronized (lock) {
            if (!closed) {
                gates[voice - 1].setAudible(audible, toFrames(songTimeMs));
            }
        }
    }

    private void run() {
        try {
            while (true) {
                synchronized (lock) {
                    while (!closed && (!started || paused || baseSongFrame >= instrumental.frameCount())) {
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
            synchronized (lock) {
                pause();
                failure = error;
            }
        }
    }

    void pump() {
        synchronized (lock) {
            if (closed || paused || !started || line == null) {
                return;
            }
            // refill the target queue in this wakeup, even after a longer scheduler pause
            int maxBlocks = (MAX_QUEUED_FRAMES + BLOCK_FRAMES - 1) / BLOCK_FRAMES;
            for (int blockIndex = 0; blockIndex < maxBlocks; blockIndex++) {
                int available = line.available();
                int queued = Math.max(0, (line.getBufferSize() - available) / FRAME_BYTES);
                int frames = Math.min(BLOCK_FRAMES, Math.min(available / FRAME_BYTES,
                        MAX_QUEUED_FRAMES - queued));
                frames = (int) Math.min(frames, instrumental.frameCount() - writeFrame);
                if (frames <= 0) {
                    return;
                }
                renderFrames(writeFrame, block, frames);
                int requested = frames * FRAME_BYTES;
                // write only the space reported free so the Swing thread is not held up
                int written = line.write(block, 0, requested);
                writeFrame += written / FRAME_BYTES;
                if (written < requested) {
                    return;
                }
            }
        }
    }

    void renderFrames(long firstFrame, byte[] output, int frames) {
        // read each channel once per block while all tracks keep the same audio clock
        double musicGain = AudioSettings.getVolume(AudioSettings.Channel.MUSIC) / 100.0;
        double vocalGain = AudioSettings.getVolume(AudioSettings.Channel.VOCALS) / 100.0;
        for (int frame = 0; frame < frames; frame++) {
            long songFrame = firstFrame + frame;
            double firstGain = gates[0].gainAt(songFrame) * vocalGain;
            double secondGain = gates[1].gainAt(songFrame) * vocalGain;
            for (int channel = 0; channel < 2; channel++) {
                double value = instrumental.sample(songFrame, channel) * musicGain;
                if (voices[0] != null) {
                    value += voices[0].sample(songFrame - voiceOffsets[0], channel) * firstGain;
                }
                if (voices[1] != null) {
                    value += voices[1].sample(songFrame - voiceOffsets[1], channel) * secondGain;
                }
                int pcm = (int) Math.round(Math.max(-32768, Math.min(32767, value * masterGain)));
                int index = frame * FRAME_BYTES + channel * 2;
                output[index] = (byte) pcm;
                output[index + 1] = (byte) (pcm >> 8);
            }
        }
    }

    private static long toFrames(double ms) {
        return Math.round(ms * PcmTrack.SAMPLE_RATE / 1000.0);
    }

    private static double readSetting(Map<String, String> settings, String key, double min, double max) {
        String raw = settings.get(key);
        if (raw == null) {
            throw new IllegalArgumentException("Missing song playback setting: " + key);
        }
        double value = Double.parseDouble(raw);
        if (!Double.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException("Invalid song playback setting: " + key);
        }
        return value;
    }

    private void checkUsable() {
        if (closed) {
            throw new IllegalStateException("The song player is closed");
        }
        if (failure != null) {
            throw new IllegalStateException("Song playback failed", failure);
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (closed) {
                return;
            }
            if (line == null) {
                silentClock.pause();
                baseSongFrame = getSongFrame();
                silentClock.stop();
            } else {
                line.stop();
                baseSongFrame = getSongFrame();
                line.flush();
                line.close();
            }
            closed = true;
            paused = true;
            lock.notifyAll();
        }
        if (worker != null) {
            worker.interrupt();
        }
    }
}
