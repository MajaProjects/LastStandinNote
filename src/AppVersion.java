import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class AppVersion {
    private static final String version = readVersion();
    private AppVersion() { }
    public static String get() { return version; }

    private static String readVersion() {
        try (InputStream stream = AppVersion.class.getResourceAsStream("/version.txt")) {
            if (stream != null) {
                String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8).strip();
                if (text.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) return text;
            }
        } catch (java.io.IOException error) {
            System.err.println("Could not read the game version: " + error.getMessage());
        }
        return "development";
    }
}
