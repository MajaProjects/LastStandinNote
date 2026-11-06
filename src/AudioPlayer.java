import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

public final class AudioPlayer implements AutoCloseable {

    private final Clip[] clips = new Clip[4];
    private int nextClip = 0;
    private boolean closed;

    public AudioPlayer(String path) {
        URL resource = AudioPlayer.class.getResource(path);

        if (resource == null) {
            throw new IllegalArgumentException("Audio file not found: " + path);
        }

        // read the sound once and share its data between playback channels
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(resource)) {
            AudioFormat format = stream.getFormat();
            byte[] audioData = stream.readAllBytes();

            for (int i = 0; i < clips.length; i++) {
                clips[i] = AudioSystem.getClip();
                clips[i].open(format, audioData, 0, audioData.length);
            }
        } catch (UnsupportedAudioFileException
                 | IOException
                 | LineUnavailableException
                 | IllegalArgumentException error) {
            // release any channels opened before the failure
            close();

            throw new IllegalStateException(
                    "Could not load audio: " + path,
                    error
            );
        }
    }

    public synchronized void play() {
        if (closed) {
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
        // release every audio channel that was opened
        for (Clip clip : clips) {
            if (clip != null) {
                clip.close();
            }
        }
    }
}

