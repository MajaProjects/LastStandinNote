import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.awt.Graphics2D;
import java.util.Locale;

public class BitmapFont {

    private final BufferedImage atlas;
    private final Map<Integer, Glyph> glyphs = new HashMap<>();
    private final Map<Glyph, Integer> inkBottoms = new HashMap<>();
    private int numberBaseline;

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
            // cache the last visible row rather than using differently padded rectangles
            int bottom = glyph.height();
            search:
            for (int row = glyph.height() - 1; row >= 0; row--) {
                for (int column = 0; column < glyph.width(); column++) {
                    if ((atlas.getRGB(glyph.x() + column, glyph.y() + row) >>> 24) != 0) {
                        bottom = row + 1;
                        break search;
                    }
                }
            }
            inkBottoms.put(glyph, bottom);
        }
        Glyph reference = glyphs.get((int) 'A');
        numberBaseline = reference == null ? lineHeight : inkBottoms.get(reference);
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
        drawInternal(g, text, x, y, scale, false);
    }

    public void drawAligned(Graphics2D g, String text, int x, int y, int scale) {
        drawInternal(g, text, x, y, scale, true);
    }

    private void drawInternal(Graphics2D g, String text, int x, int y, int scale,
                              boolean alignNumbers) {
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
            int top = cursorY;
            if (alignNumbers && (Character.isDigit(code) || code == '%')) {
                top += (numberBaseline - inkBottoms.get(glyph)) * scale;
            }

            g.drawImage(
                    atlas,
                    cursorX,
                    top,
                    cursorX + glyph.width() * scale,
                    top + glyph.height() * scale,
                    glyph.x(),
                    glyph.y(),
                    glyph.x() + glyph.width(),
                    glyph.y() + glyph.height(),
                    null
            );

            cursorX += glyph.advance() * scale;
        }
    }
    public void drawPulsingCentered(
            Graphics2D g,
            String text,
            int centerX,
            int y,
            int scale,
            double sizeMultiplier,
            double seconds
    ) {
        // repeat a smooth size pulse every second
        double pulse = 1.0 + 0.025
                * (1.0 - Math.cos(seconds * Math.PI * 2.0));

        double zoom = sizeMultiplier * pulse;
        int textWidth = getTextWidth(text, scale);

        Graphics2D animated = (Graphics2D) g.create();

        try {
            // keep the horizontal center fixed during the animation
            animated.translate(centerX, y);
            animated.scale(zoom, zoom);

            draw(animated, text, -textWidth / 2, 0, scale);
        } finally {
            animated.dispose();
        }
    }
}
