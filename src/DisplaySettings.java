import java.util.prefs.Preferences;

public final class DisplaySettings {

    private static final Preferences preferences =
            Preferences.userRoot().node("laststandinnote/display");
    private static volatile boolean showFps = preferences.getBoolean("show_fps", false);
    private static volatile boolean showGameStats = preferences.getBoolean("show_game_stats", false);

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
}
