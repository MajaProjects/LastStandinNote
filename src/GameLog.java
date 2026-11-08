import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class GameLog {
    private static final AtomicBoolean installed = new AtomicBoolean();
    private static final AtomicBoolean fatalReported = new AtomicBoolean();
    private static Path logPath;

    private GameLog() { }

    public static void install() {
        if (!installed.compareAndSet(false, true)) return;
        PrintStream console = System.err;
        try {
            Path directory = Path.of(System.getProperty("user.home"), ".laststandinnote");
            Files.createDirectories(directory);
            logPath = directory.resolve("log.txt");
            System.setErr(new PrintStream(new ErrorOutput(console, logPath), true, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException error) {
            logPath = null;
            console.println("Could not open the game log: " + error);
        }
        Thread.setDefaultUncaughtExceptionHandler(GameLog::uncaught);
        System.err.println("LastStandinNote " + AppVersion.get() + " | Java "
                + System.getProperty("java.version") + " | " + System.getProperty("os.name"));
    }

    public static Path getLogPath() { return logPath; }

    public static void record(String context, Throwable error) {
        System.err.println(context);
        error.printStackTrace(System.err);
    }

    private static void uncaught(Thread thread, Throwable error) {
        record("Unhandled error on " + thread.getName(), error);
        if (!fatalReported.compareAndSet(false, true)) return;
        Runnable report = () -> {
            try {
                if (!GraphicsEnvironment.isHeadless()) {
                    boolean spanish = Locale.getDefault().getLanguage().equalsIgnoreCase("es");
                    String message = spanish
                            ? "El juego encontro un error y se cerrara.\n"
                            : "The game encountered an error and will close.\n";
                    message += logPath == null
                            ? (spanish ? "No se pudo escribir el registro." : "The log could not be written.")
                            : (spanish ? "Adjunta este archivo al reporte:\n" : "Attach this file to your report:\n") + logPath;
                    JOptionPane.showMessageDialog(null, message, "LastStandinNote " + AppVersion.get(),
                            JOptionPane.ERROR_MESSAGE);
                }
            } finally {
                // stop repeated event-thread failures after reporting the original error
                System.exit(1);
            }
        };
        if (SwingUtilities.isEventDispatchThread() || GraphicsEnvironment.isHeadless()) report.run();
        else SwingUtilities.invokeLater(report);
    }

    private static final class ErrorOutput extends OutputStream {
        private static final long MAX_BYTES = 1_048_576;
        private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        private final PrintStream console;
        private final Path path;
        private OutputStream file;
        private long size;
        private boolean lineStart = true;
        private boolean broken;

        ErrorOutput(PrintStream console, Path path) throws IOException {
            this.console = console;
            this.path = path;
            open();
        }

        private void open() throws IOException {
            size = Files.exists(path) ? Files.size(path) : 0;
            if (size >= MAX_BYTES) {
                rotate();
                return;
            }
            file = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }

        private void rotate() throws IOException {
            if (file != null) file.close();
            Files.move(path, path.resolveSibling("log.previous.txt"), StandardCopyOption.REPLACE_EXISTING);
            size = 0;
            file = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }

        @Override
        public synchronized void write(int value) {
            write(new byte[]{(byte) value}, 0, 1);
        }

        @Override
        public synchronized void write(byte[] bytes, int offset, int count) {
            console.write(bytes, offset, count);
            if (broken) return;
            try {
                for (int i = offset; i < offset + count; i++) {
                    if (size >= MAX_BYTES) rotate();
                    if (lineStart) {
                        byte[] prefix = ("[" + format.format(LocalDateTime.now()) + "] ")
                                .getBytes(StandardCharsets.UTF_8);
                        file.write(prefix);
                        size += prefix.length;
                        lineStart = false;
                    }
                    file.write(bytes[i]);
                    size++;
                    if (bytes[i] == '\n') lineStart = true;
                }
                file.flush();
            } catch (IOException error) {
                broken = true;
                console.println("Game log writing failed: " + error.getMessage());
                try { file.close(); } catch (IOException ignored) { }
            }
        }

        @Override
        public synchronized void flush() throws IOException {
            console.flush();
            if (!broken) file.flush();
        }
    }
}
