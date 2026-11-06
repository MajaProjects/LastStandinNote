import java.util.Arrays;
import java.util.List;

public final class VocalMixer {

    private static final double SWITCH_FADE_MS = 8;
    private final List<Note> notes;
    private final VocalBank bank;
    private final SingingNote[] singing;

    public VocalMixer(VocalBank bank, List<Note> notes) {
        bank.validateNotes(notes);
        this.bank = bank;
        this.notes = List.copyOf(notes);
        singing = new SingingNote[notes.size()];
    }

    public void startNote(int index, long songTimeMs) {
        if (singing[index] != null) {
            return;
        }
        Note note = notes.get(index);
        double end = note.durationMs() == 0 ? songTimeMs + bank.tapMs()
                : Math.max(note.timeMs() + note.durationMs(), songTimeMs + 40);
        singing[index] = new SingingNote(note, songTimeMs, end,
                bank.sample(note.voice(), note.vowel()), bank.pitchRate(note));
    }

    public void setHeld(int index, boolean held, long songTimeMs) {
        SingingNote note = singing[index];
        if (note == null || note.note.durationMs() == 0 || note.held == held) {
            return;
        }
        // keep a separate gate for each overlapping note
        note.gateFrom = note.gateAt(songTimeMs);
        note.gateTarget = held ? 1 : 0;
        note.gateTime = songTimeMs;
        note.held = held;
    }

    public void stopNote(int index, long songTimeMs) {
        SingingNote note = singing[index];
        if (note != null) {
            note.cutTime = Math.min(note.cutTime, songTimeMs);
        }
    }

    public void reset() {
        Arrays.fill(singing, null);
    }

    public void render(double songStartMs, float[] stereo, int frameCount) {
        Arrays.fill(stereo, 0, frameCount * 2, 0);
        for (SingingNote note : singing) {
            if (note == null || songStartMs >= Math.min(note.end, note.cutTime + SWITCH_FADE_MS)) {
                continue;
            }
            double gain = bank.gain(note.note.voice());
            double leftGain = note.note.voice() == 1 ? 1.0 : 0.8;
            double rightGain = note.note.voice() == 2 ? 1.0 : 0.8;
            for (int frame = 0; frame < frameCount; frame++) {
                double songMs = songStartMs + frame * 1000.0 / VocalBank.SAMPLE_RATE;
                double elapsed = songMs - note.start;
                if (elapsed < 0 || songMs >= note.end) {
                    continue;
                }

                double envelope = Math.min(1.0, elapsed / bank.attackMs())
                        * Math.min(1.0, (note.end - songMs) / bank.releaseMs());
                double cut = Math.max(0, 1.0 - Math.max(0, songMs - note.cutTime) / SWITCH_FADE_MS);
                double position = elapsed * VocalBank.SAMPLE_RATE / 1000.0 * note.pitchRate;
                double value = note.sample.at(position) * gain * envelope * cut * note.gateAt(songMs);
                stereo[2 * frame] += (float) (value * leftGain);
                stereo[2 * frame + 1] += (float) (value * rightGain);
            }
        }
    }

    private static final class SingingNote {
        private final Note note;
        private final double start;
        private final double end;
        private final VocalBank.Sample sample;
        private final double pitchRate;
        private double cutTime = Double.POSITIVE_INFINITY;
        private boolean held = true;
        private double gateFrom = 1;
        private double gateTarget = 1;
        private double gateTime;

        SingingNote(Note note, double start, double end, VocalBank.Sample sample, double pitchRate) {
            this.note = note;
            this.start = start;
            this.end = end;
            this.sample = sample;
            this.pitchRate = pitchRate;
        }

        double gateAt(double songMs) {
            double progress = Math.max(0, Math.min(1, (songMs - gateTime) / SWITCH_FADE_MS));
            return gateFrom + (gateTarget - gateFrom) * progress;
        }
    }
}