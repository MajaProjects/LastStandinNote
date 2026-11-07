import java.util.prefs.Preferences;

public final class DisplaySettings {

    private static final Preferences preferences =
            Preferences.userRoot().node("laststandinnote/display");
    private static volatile boolean showFps = preferences.getBoolean("show_fps", false);
    private static volatile boolean showGameStats = preferences.getBoolean("show_game_stats", false);
    private static final int[] FPS_PRESETS = {30, 60, 120, 244};
    private static volatile int fpsMode = Math.max(0, Math.min(4, preferences.getInt("fps_mode", 1)));
    private static volatile int customPosition = Math.max(0, Math.min(261,
            preferences.getInt("custom_fps", 60)));

    private DisplaySettings() {}

    public static boolean isShowFps() {
        return showFps;
    }

    public static void setShowFps(boolean enabled) {
        showFps = enabled;
        preferences.putBoolean("show_fps", enabled);
    }

    public static boolean isShowGameStats() {
        return showGameStats;
    }

    public static void setShowGameStats(boolean enabled) {
        showGameStats = enabled;
        preferences.putBoolean("show_game_stats", enabled);
    }

    public static int getFpsMode() {
        return fpsMode;
    }

    public static void cycleFpsMode(int direction) {
        fpsMode = Math.floorMod(fpsMode + Integer.signum(direction), 5);
        preferences.putInt("fps_mode", fpsMode);
    }

    public static String getFpsModeLabel() {
        return fpsMode == 4 ? "CUSTOM" : FPS_PRESETS[fpsMode] + " FPS";
    }

    public static int getCustomPosition() {
        return customPosition;
    }

    public static void setCustomPosition(int position) {
        customPosition = Math.max(0, Math.min(261, position));
        preferences.putInt("custom_fps", customPosition);
    }

    public static String getCustomLabel() {
        return customPosition == 261 ? "UNLIMITED" : Math.max(1, customPosition) + " FPS";
    }

    public static int getFpsLimit() {
        if (fpsMode < FPS_PRESETS.length) {
            return FPS_PRESETS[fpsMode];
        }
        // zero is the internal unlimited sentinel; the slider's zero end stays drawable
        return customPosition == 261 ? 0 : Math.max(1, customPosition);
    }
}
