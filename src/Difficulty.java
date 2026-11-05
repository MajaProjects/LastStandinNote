import java.util.EnumMap;
import java.util.Locale;

public enum Difficulty {
    USER,
    ADMIN,
    ROOT;

    public Difficulty move(int direction) {
        // get the available difficulty values
        Difficulty[] difficulties = values();

        // move through the list and wrap at either end
        int nextIndex = Math.floorMod(
                ordinal() + direction,
                difficulties.length
        );

        return difficulties[nextIndex];
    }

    public double getScrollSpeed() {
        EnumMap<Difficulty, Double> speeds = new EnumMap<>(Difficulty.class);

        // read settings when a song starts, rather than during rendering
        for (String originalLine : TextLoader.load("/songs/difficulties.txt")) {
            String line = originalLine.strip();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] fields = line.split(";", -1);

            if (fields.length != 2) {
                throw new IllegalArgumentException(
                        "Each difficulty needs a name and scroll speed: " + line
                );
            }

            Difficulty difficulty;
            double speed;

            try {
                difficulty = Difficulty.valueOf(
                        fields[0].strip().toUpperCase(Locale.ROOT)
                );
                speed = Double.parseDouble(fields[1].strip());
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException(
                        "Invalid difficulty setting: " + line, error
                );
            }

            // reject values that cannot produce valid note positions
            if (!Double.isFinite(speed) || speed <= 0) {
                throw new IllegalArgumentException(
                        "Scroll speed must be positive and finite: " + line
                );
            }

            if (speeds.putIfAbsent(difficulty, speed) != null) {
                throw new IllegalArgumentException(
                        "Duplicate difficulty setting: " + difficulty.name()
                );
            }
        }

        // keep the configuration complete for all three menu choices
        for (Difficulty difficulty : values()) {
            if (!speeds.containsKey(difficulty)) {
                throw new IllegalArgumentException(
                        "Missing scroll speed for " + difficulty.name()
                );
            }
        }

        return speeds.get(this);
    }
}
