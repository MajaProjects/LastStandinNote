import java.util.List;

public final class NoteJudge {

    private final List<Note> notes;
    private final boolean[] hitNotes;
    private final boolean[] missedNotes;
    private final long hitWindowMs;

    public NoteJudge(List<Note> notes, long hitWindowMs) {
        if (hitWindowMs < 0) {
            throw new IllegalArgumentException(
                    "The hit window cannot be negative"
            );
        }

        // keep the chart order so each note has a stable index
        this.notes = List.copyOf(notes);
        this.hitNotes = new boolean[notes.size()];
        this.missedNotes = new boolean[notes.size()];
        this.hitWindowMs = hitWindowMs;
    }

    public int tryHit(int voice, int lane, long songTimeMs) {
        int closestIndex = -1;
        long closestDifference = Long.MAX_VALUE;

        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);

            // sustained notes will be judged separately later
            if (note.durationMs() > 0) {
                continue;
            }

            // ignore notes that have already been judged
            if (hitNotes[i] || missedNotes[i]) {
                continue;
            }

            // only compare notes from the pressed side and lane
            if (note.voice() != voice || note.lane() != lane) {
                continue;
            }

            long difference = Math.abs(note.timeMs() - songTimeMs);

            // keep the closest note within the allowed timing window
            if (difference <= hitWindowMs
                    && difference < closestDifference) {
                closestIndex = i;
                closestDifference = difference;
            }
        }

        if (closestIndex != -1) {
            // prevent the same note from being judged twice
            hitNotes[closestIndex] = true;
        }

        return closestIndex;
    }

    public void updateMisses(
            long songTimeMs,
            boolean judgeVoiceOne,
            boolean judgeVoiceTwo
    ) {
        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);

            // leave sustained notes for their own handling
            if (note.durationMs() > 0) {
                continue;
            }

            if (hitNotes[i] || missedNotes[i]) {
                continue;
            }

            // only judge sides controlled by players
            if (note.voice() == 1 && !judgeVoiceOne) {
                continue;
            }

            if (note.voice() == 2 && !judgeVoiceTwo) {
                continue;
            }

            // mark a miss after the timing window has passed
            if (songTimeMs > note.timeMs() + hitWindowMs) {
                missedNotes[i] = true;
            }
        }
    }

    public boolean isHit(int index) {
        return hitNotes[index];
    }

    public boolean isMissed(int index) {
        return missedNotes[index];
    }
}