import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

public final class AudioPlayer implements AutoCloseable {

    private final Clip[] clips;
    private int nextClip = 0;
    private boolean closed;
    private final Runnable volumeListener = this::applyVolume;

    public AudioPlayer(String path) {
        URL resource = AudioPlayer.class.getResource(path);

        if (resource == null) {
            throw new IllegalArgumentException("Audio file not found: " + path);
        }

        Clip[] loaded = new Clip[4];
        int count = 0;
        // read the sound once and share its data between playback channels
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(resource)) {
            AudioFormat format = stream.getFormat();
            byte[] audioData = stream.readAllBytes();

            for (int i = 0; i < loaded.length; i++) {
                Clip clip = AudioOutput.openClip(format, audioData);
                if (clip == null) break;
                loaded[count++] = clip;
            }
        } catch (UnsupportedAudioFileException
                 | IOException
                 | IllegalArgumentException error) {
            // release any channels opened before the failure
            for (Clip clip : loaded) {
                if (clip != null) clip.close();
            }

            throw new IllegalStateException(
                    "Could not load audio: " + path,
                    error
            );
        }
        clips = java.util.Arrays.copyOf(loaded, count);
        if (clips.length > 0) AudioSettings.addListener(volumeListener);
    }

    private synchronized void applyVolume() {
        if (!closed) {
            for (Clip clip : clips) {
                AudioVolume.apply(clip, AudioSettings.getVolume(AudioSettings.Channel.EFFECTS));
            }
        }
    }

    public synchronized void play() {
        if (closed || clips.length == 0) {
            return;
        }

        // prefer a free channel so quick presses can overlap without cutting each other
        int channel = nextClip;
        for (int offset = 0; offset < clips.length; offset++) {
            int candidate = (nextClip + offset) % clips.length;
            if (!clips[candidate].isRunning() && !clips[candidate].isActive()) {
                channel = candidate;
                break;
            }
        }

        Clip clip = clips[channel];
        nextClip = (channel + 1) % clips.length;

        // reuse a busy channel only when every channel is occupied
        if (clip.isRunning() || clip.isActive()) {
            clip.stop();
        }
        if (clip.getFramePosition() != 0) {
            clip.setFramePosition(0);
        }
        clip.start();
    }
    @Override
    public synchronized void close() {
        closed = true;
        AudioSettings.removeListener(volumeListener);
        // release every audio channel that was opened
        for (Clip clip : clips) {
            if (clip != null) {
                clip.close();
            }
        }
    }
}


