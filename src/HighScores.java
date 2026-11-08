import java.util.OptionalInt;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class HighScores {
    // retain old mixed-mode records separately; their original mode cannot be recovered
    private static final String NODE = "laststandinnote/highscores/solo/v2";

    private HighScores() { }

    public record Entry(int score, OptionalDouble accuracy) { }

    public static Optional<Entry> getEntry(String songId, Difficulty difficulty) {
        OptionalInt score = get(songId, difficulty);
        return score.isPresent()
                ? Optional.of(new Entry(score.getAsInt(), getAccuracy(songId, difficulty)))
                : Optional.empty();
    }

    public static OptionalInt get(String songId, Difficulty difficulty) {
        try {
            String value = Preferences.userRoot().node(NODE).get(key(songId, difficulty), null);
            if (value != null) return OptionalInt.of(Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            // ignore a damaged entry rather than inventing a score for an unplayed song
        } catch (SecurityException | IllegalArgumentException error) {
            GameLog.record("Could not read song high score", error);
        }
        return OptionalInt.empty();
    }

    public static synchronized boolean saveIfBetter(String songId, Difficulty difficulty, int score) {
        return saveIfBetter(songId, difficulty, score, OptionalDouble.empty());
    }

    public static synchronized boolean saveIfBetter(String songId, Difficulty difficulty,
                                                     int score, double accuracy) {
        rank(accuracy);
        return saveIfBetter(songId, difficulty, score, OptionalDouble.of(accuracy));
    }

    private static boolean saveIfBetter(String songId, Difficulty difficulty,
                                        int score, OptionalDouble accuracy) {
        OptionalInt previous = get(songId, difficulty);
        OptionalDouble previousAccuracy = getAccuracy(songId, difficulty);
        boolean betterScore = previous.isEmpty() || score > previous.getAsInt();
        boolean betterAccuracy = accuracy.isPresent() && (previousAccuracy.isEmpty()
                || accuracy.getAsDouble() > previousAccuracy.getAsDouble());
        if (!betterScore && !betterAccuracy) return false;
        try {
            Preferences preferences = Preferences.userRoot().node(NODE);
            if (betterScore) preferences.putInt(key(songId, difficulty), score);
            if (betterAccuracy) preferences.putDouble(key(songId, difficulty) + ".accuracy", accuracy.getAsDouble());
            preferences.flush();
            return true;
        } catch (BackingStoreException | SecurityException | IllegalArgumentException error) {
            // a record storage failure must not prevent the results screen from opening
            GameLog.record("Could not save song high score", error);
            return false;
        }
    }

    public static OptionalDouble getAccuracy(String songId, Difficulty difficulty) {
        try {
            String value = Preferences.userRoot().node(NODE).get(key(songId, difficulty) + ".accuracy", null);
            if (value != null) {
                double accuracy = Double.parseDouble(value);
                if (Double.isFinite(accuracy) && accuracy >= 0 && accuracy <= 100) {
                    return OptionalDouble.of(accuracy);
                }
            }
        } catch (NumberFormatException ignored) {
            // older score-only records have no accuracy; never infer it from raw points
        } catch (SecurityException | IllegalArgumentException error) {
            GameLog.record("Could not read song accuracy record", error);
        }
        return OptionalDouble.empty();
    }

    public static String rank(double accuracy) {
        if (!Double.isFinite(accuracy) || accuracy < 0 || accuracy > 100) {
            throw new IllegalArgumentException("Accuracy must be between 0 and 100");
        }
        if (accuracy == 100) return "P";
        if (accuracy > 95) return "S";
        if (accuracy >= 90) return "A";
        if (accuracy >= 80) return "B";
        if (accuracy >= 70) return "C";
        return "D";
    }

    private static String key(String songId, Difficulty difficulty) {
        // use the resource folder rather than the translated or editable display title
        return songId + "." + difficulty.name();
    }
}
