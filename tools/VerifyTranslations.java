// SPDX-License-Identifier: GPL-3.0-only
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

public final class VerifyTranslations {
    private static final Path output = Path.of("build/translation-previews");
    private static int renders;

    private static final class RecordingFont extends BitmapFont {
        final List<String> text = new ArrayList<>();
        RecordingFont() { super("/images/font/font.txt"); }
        @Override public void drawAligned(Graphics2D g, String value, int x, int y, int scale) {
            text.add(value);
            super.drawAligned(g, value, x, y, scale);
        }
        String content() { return String.join(" ", text).toUpperCase(Locale.ROOT).replaceAll("\\s+", " "); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void equal(String expected, String actual) {
        require(expected.equals(actual), "Expected " + expected + ", got " + actual);
    }

    private static BufferedImage render(Screen screen) {
        BufferedImage image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try { screen.render(g, 960, 540); } finally { g.dispose(); }
        renders++;
        return image;
    }

    private static void credits(LanguageSettings.Language language) throws Exception {
        if (language == LanguageSettings.Language.SPANISH) {
            for (String line : TextLoader.load("/CREDITS.txt")) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] fields = line.split(";", -1);
                require(!GameText.display(fields[0]).equals(fields[0]), "Untranslated category: " + fields[0]);
                if (!fields[3].isBlank()) require(!GameText.display(fields[3]).equals(fields[3]),
                        "Untranslated credit role: " + fields[3]);
                if (fields[0].equals("PROJECT TEAM") || fields[0].startsWith("ORIGINAL ")) {
                    require(!GameText.display(fields[2]).equals(fields[2]), "Untranslated original-work description: " + fields[2]);
                }
                if (fields[4].equals("Custom permission")) equal("Permiso del autor", GameText.display(fields[4]));
            }
        }
        CreditsScreen credits = new CreditsScreen(new MenuScreen());
        RecordingFont font = new RecordingFont();
        Field field = CreditsScreen.class.getDeclaredField("font");
        field.setAccessible(true);
        field.set(credits, font);
        Field pagesField = CreditsScreen.class.getDeclaredField("pages");
        pagesField.setAccessible(true);
        int pages = ((List<?>) pagesField.get(credits)).size();
        for (int page = 0; page < pages; page++) {
            font.text.clear();
            BufferedImage image = render(credits);
            String content = font.content();
            if (language == LanguageSettings.Language.SPANISH) {
                require(content.contains("CRÉDITOS"), "Missing Spanish credits heading");
                require(!content.contains("PROGRAMMING") && !content.contains("PROJECT TEAM")
                        && !content.contains("ORIGINAL CHARTS") && !content.contains("INSTRUMENTAL -")
                        && !content.contains("CUSTOM PERMISSION"), "Mixed credits page " + page);
                if (page == 0) {
                    require(content.contains("JACOBO PARADA URRIAGO"), "Jacobo attribution changed");
                    require(content.contains("MANUELA GUTIERREZ DIAZ"), "Manuela attribution changed");
                    require(content.contains("PROGRAMACIÓN Y APORTES MUSICALES"), "Team role untranslated");
                }
                if (page == 3) require(content.contains("MAPAS USER ADMIN Y ROOT"), "Chart role untranslated");
                if (page == 4) {
                    require(content.contains("PUZZLE MENU") && content.contains("MELANCHOLY RPG"),
                            "Original instrumental titles changed");
                    require(content.contains("INSTRUMENTAL DE PUZZLE"), "Instrumental role untranslated");
                }
            } else {
                require(content.contains("CREDITS"), "Missing English credits heading");
                require(!content.contains("PROGRAMACIÓN") && !content.contains("PÁGINA")
                        && !content.contains("PERMISO DEL AUTOR"), "Spanish leaked into English credits");
            }
            if (page == 0 || page == 4 || page == pages - 1) {
                ImageIO.write(image, "png", output.resolve(language.name() + "-credits-" + page + ".png").toFile());
            }
            credits.movePage(1);
        }
        // work titles and creator names are literal even when they happen to match a ui key
        Method literal = CreditsScreen.class.getDeclaredMethod("drawLiteral", Graphics2D.class,
                String.class, int.class, int.class, double.class, int.class);
        literal.setAccessible(true);
        font.text.clear();
        Graphics2D g = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try { literal.invoke(credits, g, "MUSIC", 480, 0, 1.0, 800); } finally { g.dispose(); }
        equal("MUSIC", font.content());
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(output);
        LanguageSettings.Language saved = LanguageSettings.get();
        Locale originalLocale = Locale.getDefault();
        try {
            for (LanguageSettings.Language language : LanguageSettings.Language.values()) {
                // the selected language must win over the operating system locale
                Locale.setDefault(language == LanguageSettings.Language.ENGLISH
                        ? Locale.forLanguageTag("es-CO") : Locale.US);
                LanguageSettings.set(language);
                boolean es = language == LanguageSettings.Language.SPANISH;
                equal(es ? "PÁGINA 2 DE 10" : "PAGE 2 OF 10", GameText.display("PAGE 2 OF 10"));
                equal(es ? "PUZZLE - 88 NOTAS" : "PUZZLE - 88 NOTES", GameText.display("PUZZLE - 88 NOTES"));
                equal(es ? "GANA TUX" : "TUX WINS", GameText.display("TUX WINS"));
                equal(es ? "JUGADOR UNO PERDIÓ" : "PLAYER ONE LOST", GameText.display("PLAYER ONE LOST"));
                equal(es ? "MANTÉN DFJK PARA CONTINUAR" : "HOLD DFJK TO RESUME",
                        GameText.display("HOLD DFJK TO RESUME"));
                equal(es ? "PUNTOS 123  PREC 97.5%  COMBO 4  FALLOS 1"
                        : "SCORE 123  ACC 97.5%  COMBO 4  MISSES 1",
                        GameText.display("SCORE 123  ACC 97.5%  COMBO 4  MISSES 1"));
                String[] ratings = {"SICK", "GOOD", "BAD", "SHIT", "MISS"};
                String[] spanish = {"INCREÍBLE", "BIEN", "MAL", "MUY MAL", "FALLO"};
                for (int i = 0; i < ratings.length; i++) equal(es ? spanish[i] : ratings[i], GameText.display(ratings[i]));
                equal(es ? "ACEPTAR" : "OK", GameText.display("OK"));
                String missing = GameText.errorDetail(new IllegalStateException(
                        "Missing song files:\n/songs/puzzle/chart_root.txt\n/songs/puzzle/instrumental.wav"));
                require(missing.startsWith(es ? "Faltan archivos" : "Missing song files"), "Wrong error language");
                require(missing.contains("/songs/puzzle/chart_root.txt"), "Error path changed");
                String imageError = GameText.errorDetail(new IllegalStateException("No se encontró la imagen: /images/test.png"));
                require(imageError.startsWith(es ? "No se encontró" : "Image not found"), "Image error language");
                String unknown = GameText.errorDetail(new IllegalStateException("Some internal English exception"));
                require(!unknown.contains("Some internal"), "Technical message leaked into dialog");
                require(GameLog.failureMessage(null).startsWith(es ? "El juego encontró" : "The game encountered"),
                        "Fatal error used the system language");
                credits(language);
                MenuScreen menu = new MenuScreen();
                render(new TitleScreen()); render(menu); render(new ModeSelectScreen());
                render(new CharacterSelectScreen("Puzzle", true)); render(new LoadingScreen(menu));
                for (Difficulty difficulty : Difficulty.values()) {
                    render(new SongSelectScreen(difficulty, false)); render(new SongSelectScreen(difficulty, true));
                }
                OptionsScreen options = new OptionsScreen(menu);
                for (int row = 0; row < 11; row++) { render(options); options.moveSelection(1); }
                render(new SaveChangesScreen(options, false)); render(new SaveChangesScreen(options, true));
                render(new ExitConfirmScreen(menu, null, true));
                equal("JUGAR", menu.getSelectedOption());
                try (GameScreen game = new GameScreen("/songs/puzzle/", "/songs/puzzle/chart_admin.txt",
                        "tux", "windows", false, Difficulty.ADMIN)) {
                    render(game); render(new PauseScreen(game)); render(new ResultsScreen(game));
                    render(new CountdownScreen(game, () -> 0L));
                    render(new ExitConfirmScreen(game, game, false));
                    // exercise the real judgement renderer while preserving internal scoring identifiers
                    Field judgeField = GameScreen.class.getDeclaredField("noteJudge");
                    judgeField.setAccessible(true);
                    NoteJudge judge = (NoteJudge) judgeField.get(game);
                    Field ratingsField = NoteJudge.class.getDeclaredField("lastJudgements");
                    ratingsField.setAccessible(true);
                    Field timesField = NoteJudge.class.getDeclaredField("judgementTimesMs");
                    timesField.setAccessible(true);
                    Method draw = GameScreen.class.getDeclaredMethod("drawJudgement", Graphics2D.class,
                            int.class, int.class, int.class, long.class);
                    draw.setAccessible(true);
                    for (String rating : ratings) {
                        ((String[]) ratingsField.get(judge))[1] = rating;
                        ((long[]) timesField.get(judge))[1] = 0;
                        Graphics2D g = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB).createGraphics();
                        try { draw.invoke(game, g, 2, 720, 60, 10L); } finally { g.dispose(); }
                        equal(rating, judge.getVisibleJudgement(2, 10));
                    }
                    Field healthField = GameScreen.class.getDeclaredField("health");
                    healthField.setAccessible(true);
                    Field balance = HealthState.class.getDeclaredField("tuxHealth");
                    balance.setAccessible(true);
                    balance.setDouble(healthField.get(game), 0.0);
                    GameOverScreen over = new GameOverScreen(game);
                    render(over);
                    Field start = GameOverScreen.class.getDeclaredField("startNanos");
                    start.setAccessible(true);
                    start.setLong(over, System.nanoTime() - 30_000_000_000L);
                    render(over);
                }
            }
        } finally { LanguageSettings.set(saved); Locale.setDefault(originalLocale); }
        System.out.println("Translation checks passed: " + renders + " screen renders, credits, dynamic text and errors in both languages.");
    }
}
