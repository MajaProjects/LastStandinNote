import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class TextLoader {

    private TextLoader() {
    }

    public static List<String> load(String path) {
        InputStream stream = TextLoader.class.getResourceAsStream(path);

        if (stream == null) {
            throw new IllegalStateException(
                    "No se encontró el archivo de texto: " + path
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
                    "No se pudo leer el archivo de texto: " + path,
                    error
            );
        }
    }
}