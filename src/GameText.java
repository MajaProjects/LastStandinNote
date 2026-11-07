import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GameText {

    private static final Map<String, String> spanish = new HashMap<>();
    private static final Map<String, String> aliases = Map.ofEntries(
            Map.entry("JUGAR", "PLAY"), Map.entry("OPCIONES", "OPTIONS"),
            Map.entry("CREDITOS", "CREDITS"), Map.entry("SALIR", "QUIT"),
            Map.entry("MENU PRINCIPAL", "MAIN MENU"), Map.entry("INDIVIDUAL", "SINGLE PLAYER"),
            Map.entry("DOS JUGADORES", "TWO PLAYERS"), Map.entry("SELECCIONA EL MODO", "SELECT MODE"),
            Map.entry("ESC PARA VOLVER", "ESC TO RETURN"), Map.entry("PULSA ENTER", "PRESS ENTER"));
    private static final Pattern page = Pattern.compile("PAGE (\\d+) OF (\\d+)");
    private static final Pattern noteCount = Pattern.compile("(.+ - \\d+) NOTES");

    static {
        add("PLAY", "JUGAR"); add("OPTIONS", "OPCIONES"); add("CREDITS", "CRÉDITOS");
        add("QUIT", "SALIR"); add("MAIN MENU", "MENÚ PRINCIPAL"); add("PRESS ENTER", "PULSA ENTER");
        add("SELECT MODE", "ELIGE EL MODO"); add("SINGLE PLAYER", "UN JUGADOR");
        add("TWO PLAYERS", "DOS JUGADORES"); add("ESC TO RETURN", "ESC PARA VOLVER");
        add("SELECT A SONG", "ELIGE UNA CANCIÓN"); add("DIFFICULTY", "DIFICULTAD");
        add("PICK A CHARACTER", "ELIGE UN PERSONAJE"); add("PLAYER ONE", "JUGADOR UNO");
        add("PLAYER TWO", "JUGADOR DOS"); add("LOADING SONG", "CARGANDO CANCIÓN");
        add("Ñ OR ;", "Ñ O ;");
        add("PLEASE WAIT", "ESPERA UN MOMENTO"); add("ESC TO CANCEL", "ESC PARA CANCELAR");
        add("PAUSED", "PAUSA"); add("RESUME", "CONTINUAR"); add("RESTART", "REINICIAR");
        add("SONG SELECT", "ELEGIR CANCIÓN"); add("UP DOWN ENTER", "ARRIBA ABAJO ENTER");
        add("GET READY", "PREPÁRATE"); add("P TO PAUSE", "P PARA PAUSAR"); add("GO", "YA");
        add("GAME OVER", "FIN DE PARTIDA"); add("DRAW", "EMPATE"); add("TIED RESULT", "EMPATE");
        add("TIED PERFORMANCE", "EMPATE"); add("BOTH CHARACTERS LOST", "AMBOS PERSONAJES PERDIERON");
        add("RESULTS", "RESULTADOS"); add("YOUR STATS", "TUS ESTADÍSTICAS");
        add("TOTAL NOTES", "NOTAS TOTALES"); add("HIT NOTES", "NOTAS ACERTADAS");
        add("MAX COMBO", "COMBO MÁXIMO"); add("RAW SCORE", "PUNTOS");
        add("ACCURACY", "PRECISIÓN"); add("PERFORMANCE", "PRECISIÓN");
        add("ARROWS ENTER", "FLECHAS ENTER");
        add("AUDIO AND DISPLAY", "AUDIO Y PANTALLA"); add("RESOLUTION", "RESOLUCIÓN");
        add("MUSIC VOLUME", "VOLUMEN MÚSICA"); add("VOCAL VOLUME", "VOLUMEN VOCES");
        add("EFFECTS VOLUME", "VOLUMEN EFECTOS"); add("WINDOW MODE", "MODO DE VENTANA");
        add("WINDOWED", "VENTANA"); add("FULLSCREEN", "PANTALLA COMPLETA");
        add("LANGUAGE", "IDIOMA"); add("FPS LIMIT", "LÍMITE DE FPS");
        add("SHOW FPS", "MOSTRAR FPS"); add("SHOW GAME STATS", "MOSTRAR ESTADÍSTICAS");
        add("ON", "SÍ"); add("OFF", "NO"); add("CUSTOM", "PERSONALIZADO");
        add("CUSTOM FPS", "FPS PERSONALIZADOS"); add("UNLIMITED", "SIN LÍMITE");
        add("CONFIRM AND BACK", "CONFIRMAR Y VOLVER");
        add("ENTER OR CLICK TO CONFIRM AND RETURN", "ENTER O CLIC PARA CONFIRMAR Y VOLVER");
        add("LEFT WINDOWED   RIGHT FULLSCREEN   F11 TOGGLE", "IZQ VENTANA   DER PANTALLA COMPLETA   F11 CAMBIA");
        add("FULLSCREEN USES DESKTOP RESOLUTION   F11 WINDOWED", "PANTALLA COMPLETA USA EL ESCRITORIO   F11 VENTANA");
        add("LEFT RIGHT SELECT RESOLUTION   ENTER NEXT", "IZQ DER ELIGE RESOLUCIÓN   ENTER SIGUIENTE");
        add("LEFT RIGHT SELECT LANGUAGE   ENTER NEXT", "IZQ DER ELIGE IDIOMA   ENTER SIGUIENTE");
        add("LEFT RIGHT ADJUST OR DRAG BAR   ENTER FINISH", "IZQ DER O ARRASTRA LA BARRA   ENTER TERMINA");
        add("LEFT RIGHT PRESET   ENTER EDIT CUSTOM FPS", "IZQ DER ELIGE FPS   ENTER PERSONALIZA");
        add("LEFT RIGHT CHOOSE FPS PRESET", "IZQ DER ELIGE LÍMITE DE FPS");
        add("UP DOWN SELECT   LEFT RIGHT ADJUST   ENTER TOGGLE", "ARRIBA ABAJO ELIGE   IZQ DER AJUSTA   ENTER CAMBIA");
        add("ESC FINISH EDIT   CONFIRM TO APPLY", "ESC TERMINA EDICIÓN   CONFIRMA PARA APLICAR");
        add("ESC BACK   UNSAVED CHANGES - CONFIRM TO APPLY", "ESC VOLVER   CAMBIOS PENDIENTES - CONFIRMA PARA APLICAR");
        add("ESC BACK   NO UNSAVED CHANGES", "ESC VOLVER   SIN CAMBIOS PENDIENTES");
        add("SAVE CHANGES?", "¿GUARDAR CAMBIOS?");
        add("YOUR CHANGES HAVE NOT BEEN APPLIED", "LOS CAMBIOS TODAVÍA NO SE HAN APLICADO");
        add("SAVE AND QUIT", "GUARDAR Y SALIR"); add("QUIT WITHOUT SAVING", "SALIR SIN GUARDAR");
        add("KEEP EDITING", "SEGUIR EDITANDO"); add("SAVE AND BACK", "GUARDAR Y VOLVER");
        add("DISCARD CHANGES", "DESCARTAR CAMBIOS");
        add("ARROWS SELECT   ENTER CONFIRM   ESC CANCEL", "FLECHAS ELIGE   ENTER CONFIRMA   ESC CANCELA");
        add("QUIT GAME?", "¿SALIR DEL JUEGO?"); add("LEAVE SONG?", "¿SALIR DE LA CANCIÓN?");
        add("RETURN TO YOUR GAME OR CONFIRM EXIT", "CONTINÚA O CONFIRMA LA SALIDA");
        add("CURRENT SONG PROGRESS WILL BE LOST", "SE PERDERÁ EL PROGRESO DE LA CANCIÓN");
        add("CONTINUE", "CONTINUAR"); add("QUIT GAME", "SALIR DEL JUEGO");
        add("LEAVE SONG", "SALIR DE LA CANCIÓN");
        add("PROJECT TEAM", "EQUIPO"); add("MUSIC", "MÚSICA"); add("SOUND EFFECTS", "EFECTOS DE SONIDO");
        add("ART AND UI", "ARTE E INTERFAZ");
        add("Programming and music", "Programación y música");
        add("Sprite artwork, lettering and programming", "Sprites, letras y programación");
        add("SOURCES AND LICENSES IN CREDITS.TXT", "FUENTES Y LICENCIAS EN CREDITS.TXT");
        add("ARROWS CHANGE PAGE   ESC OR ENTER BACK", "FLECHAS CAMBIA PÁGINA   ESC O ENTER VOLVER");
        add("Title screen", "Pantalla inicial"); add("Menu music", "Música del menú");
        add("Pause music", "Música de pausa"); add("Game over music", "Música de fin de partida");
        add("Miss sound", "Sonido de fallo"); add("Countdown", "Cuenta atrás");
        add("Death sound", "Sonido de derrota"); add("Retry confirmation", "Confirmación de reinicio");
        add("Menu navigation", "Navegación del menú"); add("Menu confirmation", "Confirmación del menú");
        add("Menu back", "Volver en el menú"); add("Level complete", "Nivel completado");
        add("Combo alert", "Aviso de combo"); add("Second miss sound", "Segundo sonido de fallo");
        add("Gameplay background", "Fondo del escenario"); add("UI icon resource", "Iconos de interfaz");
        add("Menu and song selection background", "Fondo del menú y selección de canción");
        add("ERROR", "ERROR"); add("COULD NOT LOAD SONG", "NO SE PUDO CARGAR LA CANCIÓN");
        add("COULD NOT RESTART SONG", "NO SE PUDO REINICIAR LA CANCIÓN");
        add("COULD NOT START SONG", "NO SE PUDO INICIAR LA CANCIÓN");
        add("COULD NOT OPEN CREDITS", "NO SE PUDIERON ABRIR LOS CRÉDITOS");
        add("COULD NOT SAVE SETTINGS", "NO SE PUDO GUARDAR LA CONFIGURACIÓN");
        add("Could not save settings", "No se pudo guardar la configuración");
    }

    private GameText() { }
    private static void add(String source, String translation) { spanish.put(source, translation); }

    public static String display(String text) {
        if (text == null) return "";
        String source = aliases.getOrDefault(text, text);
        if (LanguageSettings.get() == LanguageSettings.Language.ENGLISH) return source;
        String translated = spanish.get(source);
        if (translated != null) return translated;
        // format dynamic ui messages while preserving names, keys, numbers and asset titles
        if (source.startsWith("SCORE ") && source.contains("  ACC ")) {
            return source.replaceFirst("^SCORE ", "PUNTOS ")
                    .replace("  ACC ", "  PREC ").replace("  MISSES ", "  FALLOS ");
        }
        if (source.startsWith("HOLD ") && source.endsWith(" TO RESUME")) {
            return "MANTÉN " + source.substring(5, source.length() - 10) + " PARA CONTINUAR";
        }
        if (source.endsWith(" WINS")) return "GANA " + source.substring(0, source.length() - 5);
        if (source.endsWith(" LOST")) return display(source.substring(0, source.length() - 5)) + " PERDIÓ";
        if (source.startsWith("PAGE ")) {
            Matcher match = page.matcher(source);
            if (match.matches()) return "PÁGINA " + match.group(1) + " DE " + match.group(2);
        }
        if (source.endsWith(" NOTES")) {
            Matcher match = noteCount.matcher(source);
            if (match.matches()) return match.group(1) + " NOTAS";
        }
        int separator = source.indexOf(" - ");
        if (separator >= 0) {
            String prefix = spanish.get(source.substring(0, separator));
            if (prefix != null) return prefix + source.substring(separator);
        }
        return source;
    }
}
