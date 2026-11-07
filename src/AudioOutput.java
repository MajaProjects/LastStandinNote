import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.atomic.AtomicBoolean;

final class AudioOutput {

    private static final AtomicBoolean reportedUnavailable = new AtomicBoolean();

    private AudioOutput() { }

    static Clip openClip(AudioFormat format, byte[] data) {
        Clip clip = null;
        try {
            clip = AudioSystem.getClip();
            clip.open(format, data, 0, data.length);
            return clip;
        } catch (LineUnavailableException | IllegalArgumentException error) {
            if (clip != null) clip.close();
            reportUnavailable(error);
            return null;
        }
    }

    static SourceDataLine openSongLine(AudioFormat format, int bufferBytes) {
        SourceDataLine line = null;
        try {
            line = AudioSystem.getSourceDataLine(format);
            line.open(format, bufferBytes);
            return line;
        } catch (LineUnavailableException | IllegalArgumentException error) {
            if (line != null) line.close();
            reportUnavailable(error);
            return null;
        }
    }

    private static void reportUnavailable(Exception error) {
        // report device failures once without hiding missing or unreadable resources
        if (reportedUnavailable.compareAndSet(false, true)) {
            System.err.println("Audio output unavailable; continuing without sound. " + error.getMessage());
        }
    }
}
