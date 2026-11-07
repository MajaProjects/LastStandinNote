import java.util.Arrays;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class SettingsDraft {

    private DisplaySettings.Snapshot originalDisplay = DisplaySettings.snapshot();
    private DisplaySettings.Snapshot display = originalDisplay;
    private int[] originalVolumes = readVolumes();
    private int[] volumes = originalVolumes.clone();
    private LanguageSettings.Language originalLanguage = LanguageSettings.get();
    private LanguageSettings.Language language = originalLanguage;
    private final Runnable persist;

    public SettingsDraft() {
        this(SettingsDraft::flushPreferences);
    }

    SettingsDraft(Runnable persist) {
        this.persist = persist;
    }

    public DisplaySettings.Snapshot display() { return display; }
    public LanguageSettings.Language language() { return language; }
    public void cycleLanguage(int direction) {
        LanguageSettings.Language[] values = LanguageSettings.Language.values();
        language = values[Math.floorMod(language.ordinal() + Integer.signum(direction), values.length)];
    }
    public int getVolume(AudioSettings.Channel channel) { return volumes[channel.ordinal()]; }
    public void setVolume(AudioSettings.Channel channel, int value) {
        volumes[channel.ordinal()] = Math.max(0, Math.min(100, value));
    }

    public boolean hasChanges() {
        return !display.equals(originalDisplay) || !Arrays.equals(volumes, originalVolumes)
                || language != originalLanguage;
    }

    public void cycleResolution(int direction) {
        int value = Math.floorMod(display.resolution() + Integer.signum(direction),
                DisplaySettings.resolutionCount());
        display = new DisplaySettings.Snapshot(value, display.fullscreen(), display.fpsMode(),
                display.customPosition(), display.showFps(), display.showGameStats());
    }

    public void setFullscreen(boolean value) {
        display = new DisplaySettings.Snapshot(display.resolution(), value, display.fpsMode(),
                display.customPosition(), display.showFps(), display.showGameStats());
    }

    public void cycleFpsMode(int direction) {
        int value = Math.floorMod(display.fpsMode() + Integer.signum(direction), 5);
        display = new DisplaySettings.Snapshot(display.resolution(), display.fullscreen(), value,
                display.customPosition(), display.showFps(), display.showGameStats());
    }

    public void setCustomPosition(int value) {
        display = new DisplaySettings.Snapshot(display.resolution(), display.fullscreen(), display.fpsMode(),
                Math.max(0, Math.min(261, value)), display.showFps(), display.showGameStats());
    }

    public void setShowFps(boolean value) {
        display = new DisplaySettings.Snapshot(display.resolution(), display.fullscreen(), display.fpsMode(),
                display.customPosition(), value, display.showGameStats());
    }

    public void setShowGameStats(boolean value) {
        display = new DisplaySettings.Snapshot(display.resolution(), display.fullscreen(), display.fpsMode(),
                display.customPosition(), display.showFps(), value);
    }

    public void discard() {
        display = originalDisplay;
        volumes = originalVolumes.clone();
        language = originalLanguage;
    }

    public void apply() {
        DisplaySettings.Snapshot beforeDisplay = DisplaySettings.snapshot();
        int[] beforeVolumes = readVolumes();
        LanguageSettings.Language beforeLanguage = LanguageSettings.get();
        try {
            applyVolumes(volumes);
            DisplaySettings.apply(display);
            LanguageSettings.set(language);
            persist.run();
        } catch (RuntimeException error) {
            // retain the draft and restore the applied state when saving fails
            try {
                applyVolumes(beforeVolumes);
                DisplaySettings.apply(beforeDisplay);
                LanguageSettings.set(beforeLanguage);
                persist.run();
            } catch (RuntimeException rollbackError) {
                error.addSuppressed(rollbackError);
            }
            throw new IllegalStateException("Could not save settings", error);
        }
        originalDisplay = display;
        originalVolumes = volumes.clone();
        originalLanguage = language;
    }

    private static int[] readVolumes() {
        return Arrays.stream(AudioSettings.Channel.values()).mapToInt(AudioSettings::getVolume).toArray();
    }

    private static void applyVolumes(int[] values) {
        for (AudioSettings.Channel channel : AudioSettings.Channel.values()) {
            AudioSettings.setVolume(channel, values[channel.ordinal()]);
        }
    }

    private static void flushPreferences() {
        try {
            Preferences.userRoot().node("laststandinnote").flush();
        } catch (BackingStoreException error) {
            throw new IllegalStateException("Could not persist settings", error);
        }
    }
}
