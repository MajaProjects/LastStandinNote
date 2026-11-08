import java.util.prefs.Preferences;

public final class TimingSettings {
    public static final int MIN_OFFSET_MS = -200;
    public static final int MAX_OFFSET_MS = 200;
    private static final Preferences preferences = Preferences.userRoot().node("laststandinnote/timing");
    private static volatile int offsetMs = clamp(preferences.getInt("audioOffsetMs", 0));

    private TimingSettings() { }

    public static int getOffsetMs() { return offsetMs; }

    public static void setOffsetMs(int value) {
        int next = clamp(value);
        if (offsetMs != next) {
            preferences.putInt("audioOffsetMs", next);
            offsetMs = next;
        }
    }

    public static int clamp(int value) {
        return Math.max(MIN_OFFSET_MS, Math.min(MAX_OFFSET_MS, value));
    }
}
