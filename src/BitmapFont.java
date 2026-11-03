import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.awt.Graphics2D;
import java.util.Locale;

public class BitmapFont {

    private final BufferedImage atlas;
    private final Map<Integer, Glyph> glyphs = new HashMap<>();

    private int lineHeight;
    private int spaceWidth;

    // keep character data inside the font that uses it
    private record Glyph(
            int x,
            int y,
            int width,
            int height,
            int advance
    ) {
    }

    public BitmapFont(String configPath) {
        String imageName = null;
        int lineNumber = 0;

        // read settings and character regions from the text file
        for (String rawLine : TextLoader.load(configPath)) {
            lineNumber++;
            String line = rawLine.trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] fields = line.split(";", -1);

            try {
                switch (fields[0]) {
                    case "IMAGEN" -> {
                        requireFields(fields, 2);
                        imageName = fields[1];
                    }

                    case "ALTO_LINEA" -> {
                        requireFields(fields, 2);
                        lineHeight = Integer.parseInt(fields[1]);
                    }

                    case "ESPACIO" -> {
                        requireFields(fields, 2);
                        spaceWidth = Integer.parseInt(fields[1]);
                    }

                    case "CARACTER" -> {
                        requireFields(fields, 7);

                        int code = Integer.parseInt(fields[1]);

                        Glyph glyph = new Glyph(
                                Integer.parseInt(fields[2]),
                                Integer.parseInt(fields[3]),
                                Integer.parseInt(fields[4]),
                                Integer.parseInt(fields[5]),
                                Integer.parseInt(fields[6])
                        );

                        if (glyphs.putIfAbsent(code, glyph) != null) {
                            throw new IllegalArgumentException(
                                    "Carácter duplicado: " + code
                            );
                        }
                    }

                    default -> throw new IllegalArgumentException(
                            "Registro desconocido: " + fields[0]
                    );
                }
            } catch (IllegalArgumentException error) {
                throw new IllegalStateException(
                        "Error en " + configPath
                                + ", línea " + lineNumber,
                        error
                );
            }
        }

        if (imageName == null || imageName.isBlank()
                || lineHeight <= 0 || spaceWidth <= 0
                || glyphs.isEmpty()) {
            throw new IllegalStateException(
                    "Configuración de fuente incompleta: " + configPath
            );
        }

        // resolve the image relative to the configuration file
        int folderEnd = configPath.lastIndexOf('/') + 1;
        String folder = configPath.substring(0, folderEnd);

        atlas = ImageLoader.load(folder + imageName);

        // reject character regions that fall outside the atlas
        for (Glyph glyph : glyphs.values()) {
            if (glyph.x() < 0 || glyph.y() < 0
                    || glyph.width() <= 0 || glyph.height() <= 0
                    || glyph.advance() <= 0
                    || glyph.width() > atlas.getWidth()
                    || glyph.height() > atlas.getHeight()
                    || glyph.x() > atlas.getWidth() - glyph.width()
                    || glyph.y() > atlas.getHeight() - glyph.height()) {
                throw new IllegalStateException(
                        "Región de carácter inválida en: " + configPath
                );
            }
        }
    }

    private static void requireFields(String[] fields, int expected) {
        if (fields.length != expected) {
            throw new IllegalArgumentException(
                    "Se esperaban " + expected + " campos"
            );
        }

    }


    private Glyph getGlyph(int code) {
        Glyph glyph = glyphs.get(code);

        if (glyph == null) {
            throw new IllegalArgumentException(
                    "La fuente no contiene el carácter Unicode: " + code
            );
        }

        return glyph;
    }

    public int getTextWidth(String text, int scale) {
        if (scale <= 0) {
            throw new IllegalArgumentException("La escala debe ser positiva");
        }

        int width = 0;
        int maxWidth = 0;

        // measure each line using the configuredimport java.awt.Graphics2D;
        for (int code : text.toUpperCase(Locale.ROOT).codePoints().toArray()) {
            if (code == '\n') {
                maxWidth = Math.max(maxWidth, width);
                width = 0;
            } else if (code == ' ') {
                width += spaceWidth;
            } else {
                width += getGlyph(code).advance();
            }
        }

        return Math.max(maxWidth, width) * scale;
    }

    public void draw(Graphics2D g, String text, int x, int y, int scale) {
        if (scale <= 0) {
            throw new IllegalArgumentException("La escala debe ser positiva");
        }

        int cursorX = x;
        int cursorY = y;

        // draw character regions directly from the shared atlas
        for (int code : text.toUpperCase(Locale.ROOT).codePoints().toArray()) {
            if (code == '\n') {
                cursorX = x;
                cursorY += lineHeight * scale;
                continue;
            }

            if (code == ' ') {
                cursorX += spaceWidth * scale;
                continue;
            }

            Glyph glyph = getGlyph(code);

            g.drawImage(
                    atlas,
                    cursorX,
                    cursorY,
                    cursorX + glyph.width() * scale,
                    cursorY + glyph.height() * scale,
                    glyph.x(),
                    glyph.y(),
                    glyph.x() + glyph.width(),
                    glyph.y() + glyph.height(),
                    null
            );

            cursorX += glyph.advance() * scale;
        }
    }
}