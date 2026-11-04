import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

public final class AudioPlayer implements AutoCloseable {

    private final Clip[] clips = new Clip[4];
    private int nextClip = 0;

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

    public void play() {
        // rotate through the channels for consecutive navigation sounds
        Clip clip = clips[nextClip];
        nextClip = (nextClip + 1) % clips.length;

        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }
    @Override
    public void close() {
        // release every audio channel that was opened
        for (Clip clip : clips) {
            if (clip != null) {
                clip.close();
            }
        }
    }
}
