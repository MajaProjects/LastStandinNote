import java.util.Objects;
import java.util.prefs.Preferences;

public final class LanguageSettings {

    public enum Language {
        ENGLISH("English"), SPANISH("Español");

        private final String displayName;
        Language(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }

    private static final Preferences preferences =
            Preferences.userRoot().node("laststandinnote/general");
    private static volatile Language language = readLanguage();

    private LanguageSettings() { }

    public static Language get() { return language; }

    public static void set(Language value) {
        Objects.requireNonNull(value, "Language is required");
        if (language != value) {
            language = value;
            preferences.put("language", value.name());
        }
    }

    private static Language readLanguage() {
        try {
            return Language.valueOf(preferences.get("language", Language.ENGLISH.name()));
        } catch (IllegalArgumentException error) {
            return Language.ENGLISH;
        }
    }
}
