import java.util.prefs.Preferences;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DisplaySettings {

    private static final Preferences preferences =
            Preferences.userRoot().node("laststandinnote/display");
    private static volatile boolean showFps = preferences.getBoolean("show_fps", false);
    private static volatile boolean showGameStats = preferences.getBoolean("show_game_stats", false);
    private static final int[] FPS_PRESETS = {30, 60, 120, 244};
    private static volatile int fpsMode = Math.max(0, Math.min(4, preferences.getInt("fps_mode", 1)));
    private static volatile int customPosition = Math.max(0, Math.min(261,
            preferences.getInt("custom_fps", 60)));
    private static final int[][] RESOLUTIONS = {{960, 540}, {1280, 720}, {1920, 1080}};
    private static volatile int resolution = Math.max(0, Math.min(2,
            preferences.getInt("resolution", 0)));
    private static volatile boolean fullscreen = preferences.getBoolean("fullscreen", false);
    private static final CopyOnWriteArrayList<Runnable> displayListeners = new CopyOnWriteArrayList<>();

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

    public static boolean isFullscreen() { return fullscreen; }
    public static int getWindowWidth() { return RESOLUTIONS[resolution][0]; }
    public static int getWindowHeight() { return RESOLUTIONS[resolution][1]; }
    public static String getResolutionLabel() {
        return getWindowWidth() + " X " + getWindowHeight();
    }

    public static void setFullscreen(boolean enabled) {
        if (fullscreen != enabled) {
            fullscreen = enabled;
            preferences.putBoolean("fullscreen", enabled);
            notifyDisplayListeners();
        }
    }

    public static void cycleResolution(int direction) {
        if (direction != 0) {
            resolution = Math.floorMod(resolution + Integer.signum(direction), RESOLUTIONS.length);
            preferences.putInt("resolution", resolution);
            notifyDisplayListeners();
        }
    }

    static void addDisplayListener(Runnable listener) { displayListeners.add(listener); }
    static void removeDisplayListener(Runnable listener) { displayListeners.remove(listener); }

    private static void notifyDisplayListeners() {
        for (Runnable listener : displayListeners) listener.run();
    }
}
