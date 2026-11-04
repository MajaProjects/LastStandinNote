import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

public final class AudioPlayer implements AutoCloseable {

    private final Clip clip;

    public AudioPlayer(String path) {
        URL resource = AudioPlayer.class.getResource(path);

        if (resource == null) {
            throw new IllegalArgumentException("Audio file not found: " + path);
        }

        Clip loadedClip = null;

        // load the sound once so playback can start without reading the file again
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(resource)) {
            loadedClip = AudioSystem.getClip();
            loadedClip.open(stream);
            clip = loadedClip;
        } catch (UnsupportedAudioFileException
                 | IOException
                 | LineUnavailableException error) {
            // release the audio device if loading fails
            if (loadedClip != null) {
                loadedClip.close();
            }

            throw new IllegalStateException("Could not load audio: " + path, error);
        }
    }

    public void play() {
        // restart the sound when another navigation input arrives
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    @Override
    public void close() {
        // release the audio resources when this player is discarded
        clip.close();
    }
}
