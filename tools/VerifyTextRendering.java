// SPDX-License-Identifier: GPL-3.0-only
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class VerifyTextRendering {
    private static final String FONT = "/images/font/font.txt";

    private static final class RecordingFont extends BitmapFont {
        final List<String> text = new ArrayList<>();
        RecordingFont() { super(FONT); }
        @Override public void draw(Graphics2D g, String value, int x, int y, int scale) {
            text.add(value);
            super.draw(g, value, x, y, scale);
        }
        @Override public void drawAligned(Graphics2D g, String value, int x, int y, int scale) {
            text.add(value);
            super.drawAligned(g, value, x, y, scale);
        }
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field result = type.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static int[] pixels(BitmapFont font, String text, boolean aligned) {
        BufferedImage image = new BufferedImage(400, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            if (aligned) font.drawAligned(g, text, 20, 20, 2);
            else font.draw(g, text, 20, 20, 2);
        } finally { g.dispose(); }
        return image.getRGB(0, 0, 400, 100, null, 0, 400);
    }

    private static void fallback() throws Exception {
        BitmapFont font = new BitmapFont(FONT);
        for (int code : "’/'\"ÈÇÖ🦊".codePoints().toArray()) {
            String text = new String(Character.toChars(code));
            require(font.getTextWidth(text, 2) == font.getTextWidth("?", 2), "Fallback width U+" + code);
            for (boolean aligned : new boolean[]{false, true}) {
                require(Arrays.equals(pixels(font, text, aligned), pixels(font, "?", aligned)),
                        "Fallback pixels U+" + code);
            }
        }
        require(font.getTextWidth("Don’t buy something", 1) == font.getTextWidth("Don?t buy something", 1),
                "Song title measurement differs from fallback drawing");
        require(font.getTextWidth("ÁÉÍÓÚÜÑ¿¡", 1) == font.getTextWidth("AEIOUUN?!", 1),
                "Composed Spanish glyph metrics changed");
        // reject a broken font configuration at load time, instead of a later null dereference
        Path classes = Path.of(VerifyTextRendering.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path invalid = classes.resolve("font-without-fallback.txt");
        try {
            Files.write(invalid, TextLoader.load(FONT).stream()
                    .filter(line -> !line.startsWith("CARACTER;63;")).toList());
            try {
                new BitmapFont("/font-without-fallback.txt");
                throw new AssertionError("Missing fallback was accepted");
            } catch (IllegalStateException expected) {
                require(expected.getMessage().contains("'?' fallback"), "Unclear fallback configuration error");
            }
        } finally { Files.deleteIfExists(invalid); }
    }

    private static RecordingFont record(Object screen) throws Exception {
        RecordingFont font = new RecordingFont();
        field(screen.getClass(), "font").set(screen, font);
        return font;
    }

    private static void render(Screen screen, RecordingFont font, String expected) {
        font.text.clear();
        BufferedImage image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try { screen.render(g, 960, 540); } finally { g.dispose(); }
        require(font.text.contains(expected), screen.getClass().getSimpleName()
                + " changed title " + expected + ": " + font.text);
    }

    @SuppressWarnings("unchecked")
    private static void unusualCredits() throws Exception {
        CreditsScreen credits = new CreditsScreen(new MenuScreen());
        var entry = Class.forName("CreditsScreen$Entry").getDeclaredConstructor(
                String.class, String.class, String.class, String.class);
        entry.setAccessible(true);
        String creator = "Creator’Name / \"ÈÇÖ🦊\"";
        var page = Class.forName("CreditsScreen$Page").getDeclaredConstructor(String.class, List.class);
        page.setAccessible(true);
        List<Object> pages = (List<Object>) field(CreditsScreen.class, "pages").get(credits);
        pages.clear();
        pages.add(page.newInstance("MUSIC", List.of(entry.newInstance(creator, "GO", "Menu music", "CC0 1.0"))));
        RecordingFont font = record(credits);
        render(credits, font, creator);
        require(font.text.contains("GO"), "Third-party work title was translated");
    }

    @SuppressWarnings("unchecked")
    private static void songNames() throws Exception {
        LanguageSettings.Language saved = LanguageSettings.get();
        try (GameScreen game = new GameScreen("/songs/puzzle/", "/songs/puzzle/chart_admin.txt",
                "tux", "windows", false, Difficulty.ADMIN)) {
            Field chartField = field(GameScreen.class, "chart");
            SongChart original = (SongChart) chartField.get(game);
            Object health = field(GameScreen.class, "health").get(game);
            field(HealthState.class, "tuxHealth").setDouble(health, 0.0);
            RecordingFont gameplay = record(game);
            for (LanguageSettings.Language language : LanguageSettings.Language.values()) {
                LanguageSettings.set(language);
                for (String name : List.of("GO", "MUSIC", "DRAW", "WE LOST", "WE WINS", "WE NOTES",
                        "GO - 2 NOTES", "Don’t buy something", "Song/Title \"ÈÇÖ🦊\"")) {
                    chartField.set(game, new SongChart(name, original.durationMs(), original.bpm(),
                            original.beatOffsetMs(), original.notes()));
                    render(game, gameplay, name);
                    CharacterSelectScreen characters = new CharacterSelectScreen(name, false);
                    render(characters, record(characters), name);
                    SongSelectScreen songs = new SongSelectScreen(Difficulty.ADMIN, false);
                    ((List<String>) field(SongSelectScreen.class, "songs").get(songs)).set(0, name);
                    render(songs, record(songs), name);
                    GameOverScreen over = new GameOverScreen(game);
                    field(GameOverScreen.class, "startNanos").setLong(over, System.nanoTime() - 30_000_000_000L);
                    render(over, record(over), name);
                    ResultsScreen results = new ResultsScreen(game);
                    render(results, record(results), name + " - "
                            + game.getStatisticsForVoice(game.getPlayerVoice()).totalNotes()
                            + (language == LanguageSettings.Language.SPANISH ? " NOTAS" : " NOTES"));
                }
                unusualCredits();
            }
        } finally { LanguageSettings.set(saved); }
    }

    public static void main(String[] args) throws Exception {
        fallback();
        songNames();
        try {
            ImageLoader.load("/missing-image-test.png");
            throw new AssertionError("Missing image did not report an error");
        }
        catch (IllegalStateException expected) {
            require(expected.getMessage().startsWith("Image not found:"), "Image log is not English");
        }
        System.out.println("Text rendering checks passed: fallback metrics/pixels, missing glyph configuration,"
                + " unusual credits, and 90 title renders in both languages.");
    }
}
