import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.prefs.Preferences;

public final class AudioSettings {

    public enum Channel { MUSIC, VOCALS, EFFECTS }

    private static final Preferences preferences =
            Preferences.userRoot().node("laststandinnote/audio");
    private static final AtomicIntegerArray volumes = new AtomicIntegerArray(3);
    private static final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

    static {
        for (Channel channel : Channel.values()) {
            volumes.set(channel.ordinal(), clamp(preferences.getInt(channel.name(), 100)));
        }
    }

    private AudioSettings() {}

    public static int getVolume(Channel channel) {
        return volumes.get(channel.ordinal());
    }

    public static void setVolume(Channel channel, int percent) {
        int value = clamp(percent);
        if (volumes.getAndSet(channel.ordinal(), value) == value) {
            return;
        }
        // preferences persist across normal application shutdowns and subsequent launches
        preferences.putInt(channel.name(), value);
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    static void addListener(Runnable listener) {
        listeners.add(listener);
        listener.run();
    }

    static void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
