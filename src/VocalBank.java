import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class VocalBank {

    public static final int SAMPLE_RATE = 44100;
    private final Map<String, Sample> samples = new HashMap<>();
    private final double[] baseSemitones = new double[2];
    private final double[] voiceGains = new double[2];
    private double masterGain = 0.7;
    private double tapMs = 180;
    private double attackMs = 5;
    private double releaseMs = 35;

    public static VocalBank load(String folder) {
        String config = folder + "vocals/voices.txt";
        if (VocalBank.class.getResource(config) == null) {
            // songs without a vocal bank can still play their instrumental
            return null;
        }

        VocalBank bank = new VocalBank();
        boolean[] loadedVoices = new boolean[2];
        for (String original : TextLoader.load(config)) {
            String line = original.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] fields = line.split(";", -1);
            for (int i = 0; i < fields.length; i++) {
                fields[i] = fields[i].strip();
            }

            switch (fields[0]) {
                case "master_gain", "tap_ms", "attack_ms", "release_ms" -> {
                    requireFields(fields, 2);
                    double value = readNumber(fields[1]);
                    switch (fields[0]) {
                        case "master_gain" -> bank.masterGain = bounded(value, 0, 1);
                        case "tap_ms" -> bank.tapMs = bounded(value, 20, 1000);
                        case "attack_ms" -> bank.attackMs = bounded(value, 1, 100);
                        case "release_ms" -> bank.releaseMs = bounded(value, 1, 500);
                    }
                }
                case "voice" -> {
                    requireFields(fields, 4);
                    int side = readVoice(fields[1]) - 1;
                    bank.baseSemitones[side] = bounded(readNumber(fields[2]), -48, 48);
                    bank.voiceGains[side] = bounded(readNumber(fields[3]), 0, 1);
                    loadedVoices[side] = true;
                }
                case "sample" -> {
                    requireFields(fields, 4);
                    int voice = readVoice(fields[1]);
                    String vowel = fields[2].toUpperCase(Locale.ROOT);
                    if (!vowel.matches("[AEIOU]")
                            || !fields[3].matches("[a-zA-Z0-9_]+\\.wav")) {
                        throw new IllegalArgumentException("Invalid vocal sample: " + line);
                    }
                    String key = voice + ":" + vowel;
                    if (bank.samples.putIfAbsent(key,
                            loadSample(folder + "vocals/" + fields[3])) != null) {
                        throw new IllegalArgumentException("Duplicate vocal sample: " + key);
                    }
                }
                default -> throw new IllegalArgumentException("Unknown vocal setting: " + fields[0]);
            }
        }
        if (!loadedVoices[0] || !loadedVoices[1]) {
            throw new IllegalArgumentException("Both vocal voices need settings in " + config);
        }
        return bank;
    }

    public void validateNotes(List<Note> notes) {
        for (Note note : notes) {
            sample(note.voice(), note.vowel());
            double pitch = note.semitones() + baseSemitones[note.voice() - 1];
            if (!Double.isFinite(pitch) || Math.abs(pitch) > 72) {
                throw new IllegalArgumentException("Vocal pitch is outside the supported range");
            }
        }
    }

    Sample sample(int voice, String vowel) {
        Sample sample = samples.get(voice + ":" + vowel.toUpperCase(Locale.ROOT));
        if (sample == null) {
            throw new IllegalArgumentException("Missing vowel " + vowel + " for voice " + voice);
        }
        return sample;
    }

    double pitchRate(Note note) {
        return Math.pow(2.0, (baseSemitones[note.voice() - 1] + note.semitones()) / 12.0);
    }

    double gain(int voice) { return masterGain * voiceGains[voice - 1]; }
    double tapMs() { return tapMs; }
    double attackMs() { return attackMs; }
    double releaseMs() { return releaseMs; }

    private static Sample loadSample(String path) {
        URL resource = VocalBank.class.getResource(path);
        if (resource == null) {
            throw new IllegalArgumentException("Vocal sample not found: " + path);
        }
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(resource)) {
            AudioFormat format = stream.getFormat();
            if (!format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)
                    || format.getSampleRate() != SAMPLE_RATE || format.getChannels() != 1
                    || format.getSampleSizeInBits() != 16 || format.isBigEndian()) {
                throw new IllegalArgumentException("Vocal samples need mono 16-bit PCM at 44100 Hz: " + path);
            }
            byte[] bytes = stream.readAllBytes();
            if (bytes.length < 200 || bytes.length % 2 != 0) {
                throw new IllegalArgumentException("Invalid vocal sample length: " + path);
            }
            float[] frames = new float[bytes.length / 2];
            for (int i = 0; i < frames.length; i++) {
                short value = (short) ((bytes[2 * i] & 255) | (bytes[2 * i + 1] << 8));
                frames[i] = value / 32768.0f;
            }
            return new Sample(frames);
        } catch (UnsupportedAudioFileException | IOException error) {
            throw new IllegalStateException("Could not load vocal sample: " + path, error);
        }
    }

    private static void requireFields(String[] fields, int expected) {
        if (fields.length != expected) {
            throw new IllegalArgumentException("Invalid vocal configuration line");
        }
    }

    private static int readVoice(String value) {
        int voice = Integer.parseInt(value);
        if (voice < 1 || voice > 2) {
            throw new IllegalArgumentException("Vocal voice must be 1 or 2");
        }
        return voice;
    }

    private static double readNumber(String value) {
        double number = Double.parseDouble(value);
        if (!Double.isFinite(number)) {
            throw new IllegalArgumentException("Vocal settings must be finite numbers");
        }
        return number;
    }

    private static double bounded(double value, double minimum, double maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException("Vocal setting outside its supported range");
        }
        return value;
    }

    static final class Sample {
        private final float[] frames;
        private final int loopStart;
        private final int loopEnd;
        private final int crossfade;

        Sample(float[] frames) {
            this.frames = frames;
            loopStart = frames.length / 4;
            loopEnd = frames.length * 3 / 4;
            crossfade = Math.max(1, Math.min(SAMPLE_RATE / 100, (loopEnd - loopStart) / 4));
        }

        double at(double position) {
            // repeat the middle of the vowel with a short crossfade for long holds
            if (position >= loopEnd) {
                position = loopStart + crossfade
                        + (position - loopEnd) % (loopEnd - loopStart - crossfade);
            }
            double value = interpolate(position);
            if (position >= loopEnd - crossfade) {
                double blend = (position - loopEnd + crossfade) / crossfade;
                double beginning = interpolate(loopStart + position - loopEnd + crossfade);
                value += (beginning - value) * blend;
            }
            return value;
        }

        private double interpolate(double position) {
            int left = (int) position;
            int right = Math.min(left + 1, frames.length - 1);
            return frames[left] + (frames[right] - frames[left]) * (position - left);
        }
    }
}