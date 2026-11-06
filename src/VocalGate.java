import java.util.ArrayList;
import java.util.List;

final class VocalGate {

    private static final long FADE_FRAMES = PcmTrack.SAMPLE_RATE * 8L / 1000;
    private final List<Change> changes = new ArrayList<>();
    private boolean audible = true;

    void reset() {
        changes.clear();
        audible = true;
    }

    void setAudible(boolean value, long frame) {
        if (value == audible) {
            return;
        }
        // events arrive on the song clock; retain them so pause can rewind queued frames
        long at = Math.max(0, frame);
        if (!changes.isEmpty()) {
            at = Math.max(at, changes.get(changes.size() - 1).frame());
        }
        double from = gainAt(at);
        changes.add(new Change(at, from, value ? 1 : 0));
        audible = value;
    }

    double gainAt(long frame) {
        int low = 0;
        int high = changes.size() - 1;
        int index = -1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            if (changes.get(middle).frame() <= frame) {
                index = middle;
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        if (index < 0) {
            return 1;
        }
        Change change = changes.get(index);
        double progress = Math.min(1, (frame - change.frame()) / (double) FADE_FRAMES);
        return change.from() + (change.to() - change.from()) * progress;
    }

    private record Change(long frame, double from, double to) {}
}