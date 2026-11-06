import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

final class PcmTrack {

    static final int SAMPLE_RATE = 44100;
    // reuse decoded PCM on restart while allowing the JVM to reclaim it if needed
    private static final Map<String, SoftReference<PcmTrack>> cache = new HashMap<>();
    private final byte[] pcm;
    private final int channels;
    private final int frameCount;

    private PcmTrack(byte[] pcm, int channels) {
        this.pcm = pcm;
        this.channels = channels;
        frameCount = pcm.length / (channels * 2);
    }

    static synchronized PcmTrack load(String path, boolean optional) {
        SoftReference<PcmTrack> reference = cache.get(path);
        PcmTrack cached = reference == null ? null : reference.get();
        if (cached != null) {
            return cached;
        }
        URL resource = PcmTrack.class.getResource(path);
        if (resource == null) {
            if (optional) {
                return null;
            }
            throw new IllegalArgumentException("Audio track not found: " + path);
        }
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(resource)) {
            AudioFormat format = stream.getFormat();
            if (!format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)
                    || format.getSampleRate() != SAMPLE_RATE || format.getSampleSizeInBits() != 16
                    || format.isBigEndian() || (format.getChannels() != 1 && format.getChannels() != 2)) {
                throw new IllegalArgumentException("Tracks need 16-bit PCM at 44100 Hz: " + path);
            }
            long frames = stream.getFrameLength();
            byte[] data;
            if (frames != AudioSystem.NOT_SPECIFIED) {
                long byteCount = Math.multiplyExact(frames, (long) format.getFrameSize());
                if (byteCount <= 0 || byteCount > Integer.MAX_VALUE - 8L) {
                    throw new IllegalArgumentException("Unsupported audio track size: " + path);
                }
                // allocate the final buffer once instead of copying a large WAV twice
                data = new byte[(int) byteCount];
                if (stream.readNBytes(data, 0, data.length) != data.length) {
                    throw new IllegalArgumentException("Truncated audio track: " + path);
                }
            } else {
                data = stream.readAllBytes();
            }
            if (data.length == 0 || data.length % format.getFrameSize() != 0) {
                throw new IllegalArgumentException("Invalid audio track length: " + path);
            }
            PcmTrack track = new PcmTrack(data, format.getChannels());
            cache.put(path, new SoftReference<>(track));
            return track;
        } catch (IOException | UnsupportedAudioFileException error) {
            throw new IllegalStateException("Could not load audio track: " + path, error);
        }
    }

    int frameCount() {
        return frameCount;
    }

    int sample(long frame, int channel) {
        if (frame < 0 || frame >= frameCount) {
            return 0;
        }
        int position = ((int) frame * channels + (channels == 1 ? 0 : channel)) * 2;
        return (short) ((pcm[position] & 255) | (pcm[position + 1] << 8));
    }
}