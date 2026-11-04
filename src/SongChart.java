import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record SongChart(
        String name,
        long durationMs,
        double bpm,
        long beatOffsetMs,
        List<Note> notes
) {
    public SongChart {
        // check the settings needed to play the chart
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The song needs a name");
        }

        if (durationMs <= 0) {
            throw new IllegalArgumentException("Invalid song duration");
        }

        if (!Double.isFinite(bpm) || bpm <= 0) {
            throw new IllegalArgumentException("Invalid song BPM");
        }

        // protect the chart from changes to the original list
        notes = List.copyOf(notes);
    }

    public static SongChart load(String path) {
        String name = "";
        long durationMs = 0;
        double bpm = 0;
        long beatOffsetMs = 0;
        int expectedNotes = -1;

        List<Note> notes = new ArrayList<>();
        int lineNumber = 0;

        for (String originalLine : TextLoader.load(path)) {
            lineNumber++;
            String line = originalLine.strip();

            // ignore comments and empty lines
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] fields = line.split(";", -1);

            for (int i = 0; i < fields.length; i++) {
                fields[i] = fields[i].strip();
            }

            try {
                switch (fields[0]) {
                    case "CANCION" -> {
                        requireFields(fields, 2);
                        name = fields[1];
                    }

                    case "DURACION_MS" -> {
                        requireFields(fields, 2);
                        durationMs = Long.parseLong(fields[1]);
                    }

                    case "BPM" -> {
                        requireFields(fields, 2);
                        bpm = Double.parseDouble(fields[1]);
                    }

                    case "BEAT_OFFSET_MS" -> {
                        requireFields(fields, 2);
                        beatOffsetMs = Long.parseLong(fields[1]);
                    }

                    case "TOTAL_NOTAS" -> {
                        requireFields(fields, 2);
                        int count = Integer.parseInt(fields[1]);

                        if (count < 0) {
                            throw new IllegalArgumentException(
                                    "Note count cannot be negative"
                            );
                        }

                        expectedNotes = count;
                    }

                    case "VOZ1", "VOZ2" -> {
                        // voice names and base pitches are descriptive metadata
                        requireFields(fields, 3);
                    }

                    case "NOTA" -> {
                        requireFields(fields, 7);

                        // convert the text fields into a validated note
                        Note note = new Note(
                                Long.parseLong(fields[1]),
                                Integer.parseInt(fields[2]),
                                Integer.parseInt(fields[3]),
                                Long.parseLong(fields[4]),
                                fields[5],
                                Double.parseDouble(fields[6])
                        );

                        notes.add(note);
                    }

                    default -> throw new IllegalArgumentException(
                            "Unknown chart setting: " + fields[0]
                    );
                }
            } catch (IllegalArgumentException error) {
                // report the invalid line and continue reading the chart
                System.err.println(
                        path + " at line " + lineNumber + ": " + error.getMessage()
                );
            }
        }

        // keep notes ordered even if the text file uses a different order
        notes.sort(Comparator.comparingLong(Note::timeMs));

        if (expectedNotes >= 0 && expectedNotes != notes.size()) {
            System.err.println(
                    path + ": expected " + expectedNotes
                            + " notes, loaded " + notes.size()
            );
        }

        return new SongChart(name, durationMs, bpm, beatOffsetMs, notes);
    }

    private static void requireFields(String[] fields, int expected) {
        if (fields.length != expected) {
            throw new IllegalArgumentException("Wrong number of chart fields");
        }
    }
}