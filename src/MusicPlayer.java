import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

public final class MusicPlayer implements AutoCloseable {

    private final Clip clip;
    private final PlaybackClock silentClock;
    private final AudioSettings.Channel volumeChannel;
    private boolean paused;
    private int pausedFrame;
    private long pausedPositionMs;
    private boolean playingOnce;
    private boolean closed;
    private final Runnable volumeListener = this::applyVolume;

    public MusicPlayer(String path) {
        this(path, AudioSettings.Channel.MUSIC);
    }

    public MusicPlayer(String path, AudioSettings.Channel volumeChannel) {
        this.volumeChannel = volumeChannel;
        URL resource = MusicPlayer.class.getResource(path);

        if (resource == null) {
            throw new IllegalArgumentException("Music file not found: " + path);
        }

        Clip loadedClip = null;
        long durationNanos;

        // load one playback channel for this music track
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(resource)) {
            AudioFormat format = stream.getFormat();
            byte[] data = stream.readAllBytes();
            if (format.getFrameSize() <= 0 || format.getFrameRate() <= 0
                    || !Float.isFinite(format.getFrameRate()) || data.length % format.getFrameSize() != 0) {
                throw new IllegalArgumentException("Invalid music format: " + path);
            }
            long frames = data.length / format.getFrameSize();
            durationNanos = (long) Math.ceil(frames * 1_000_000_000.0 / format.getFrameRate());
            loadedClip = AudioOutput.openClip(format, data);
        } catch (UnsupportedAudioFileException
                 | IOException
                 | IllegalArgumentException error) {
            if (loadedClip != null) {
                loadedClip.close();
            }

            throw new IllegalStateException(
                    "Could not load music: " + path,
                    error
            );
        }

        clip = loadedClip;
        silentClock = clip == null ? new PlaybackClock(durationNanos) : null;
        if (clip != null) AudioSettings.addListener(volumeListener);
    }

    private synchronized void applyVolume() {
        if (!closed && clip != null) {
            AudioVolume.apply(clip, AudioSettings.getVolume(volumeChannel));
        }
    }

    public void playLoop() {
        if (closed) return;
        if (clip == null) {
            paused = false;
            playingOnce = false;
            silentClock.play(true);
            return;
        }
        // start from the beginning and repeat until stopped
        clip.stop();
        paused = false;
        playingOnce = false;
        clip.setFramePosition(0);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }
    public void play() {
        if (closed) return;
        if (clip == null) {
            paused = false;
            playingOnce = true;
            silentClock.play(false);
            return;
        }
        // start the song from the beginning and play it once
        clip.stop();
        paused = false;
        playingOnce = true;
        clip.setFramePosition(0);
        clip.start();
    }

    public long getPositionMs() {
        if (clip == null) return silentClock.getPositionNanos() / 1_000_000L;
        // use the audio position as the clock for gameplay
        return paused ? pausedPositionMs : clip.getMicrosecondPosition() / 1000;
    }

    public long getDurationMs() {
        if (clip == null) return silentClock.getDurationMs();
        return clip.getMicrosecondLength() / 1000;
    }

    public boolean isFinished() {
        // paused, stopped and looping tracks are not completed songs
        return !closed && playingOnce && !paused
                && (clip == null ? silentClock.isFinished() : clip.getFrameLength() > 0
                && clip.getFramePosition() >= clip.getFrameLength());
    }

    public boolean isSilent() { return clip == null; }

    public void pause() {
        if (closed || paused) {
            return;
        }

        if (clip == null) {
            silentClock.pause();
            paused = true;
            return;
        }

        // remember the stopped audio frame without rewinding the track
        clip.stop();
        pausedFrame = clip.getFramePosition();
        pausedPositionMs = clip.getMicrosecondPosition() / 1000;
        paused = true;
    }

    public void resume() {
        if (closed || !paused) {
            return;
        }

        if (clip == null) {
            silentClock.resume();
            paused = false;
            return;
        }

        clip.setFramePosition(pausedFrame);
        paused = false;

        // an already finished track must not restart from the beginning
        if (pausedFrame < clip.getFrameLength()) {
            clip.start();
        }
    }

    public void stop() {
        if (closed) return;
        // stop playback when another track takes over
        if (clip == null) silentClock.stop();
        else clip.stop();
        paused = false;
        playingOnce = false;
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        AudioSettings.removeListener(volumeListener);
        // release the audio channel when this player is discarded
        if (clip == null) silentClock.stop();
        else clip.close();
        paused = false;
        playingOnce = false;
    }
}

