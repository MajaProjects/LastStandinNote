import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public final class TextLoader {

    private TextLoader() {
    }

    public static List<String> load(String path) {
        InputStream stream = TextLoader.class.getResourceAsStream(path);

        if (stream == null) {
            throw new IllegalStateException(
                    "Text file not found: " + path
            );
        }

        // read bundled text using a consistent character encoding
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        )) {
            List<String> lines = new ArrayList<>();
            String line;

            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            return lines;
        } catch (IOException error) {
            throw new IllegalStateException(
                    "Could not read text file: " + path,
                    error
            );
        }
    }

    public static Map<String, String> loadSettings(String path) {
        Map<String, String> settings = new HashMap<>();

        // reuse the same key;value format for configurable game systems
        for (String originalLine : load(path)) {
            String line = originalLine.strip();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] fields = line.split(";", -1);
            if (fields.length != 2 || fields[0].isBlank() || fields[1].isBlank()) {
                throw new IllegalArgumentException("Invalid setting in " + path + ": " + line);
            }

            String key = fields[0].strip();
            if (settings.putIfAbsent(key, fields[1].strip()) != null) {
                throw new IllegalArgumentException("Duplicate setting in " + path + ": " + key);
            }
        }

        return settings;
    }

    public static boolean getBooleanSetting(Map<String, String> settings, String key) {
        String value = settings.get(key);

        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }

        throw new IllegalArgumentException("Setting " + key + " must be true or false");
    }

    public static double getNumberSetting(
            Map<String, String> settings,
            String key,
            boolean allowZero
    ) {
        String text = settings.get(key);

        if (text == null) {
            throw new IllegalArgumentException("Missing setting: " + key);
        }

        double value;
        try {
            value = Double.parseDouble(text);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Invalid number for setting: " + key, error);
        }

        if (!Double.isFinite(value) || (allowZero ? value < 0 : value <= 0)) {
            throw new IllegalArgumentException("Invalid value for setting: " + key);
        }

        return value;
    }
}