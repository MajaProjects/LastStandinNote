import java.util.List;

public final class NoteJudge {

    private final List<Note> notes;
    private final boolean[] hitNotes;
    private final boolean[] missedNotes;
    private final long hitWindowMs;
    // keep the score and latest judgement for each side
    private final int[] scores = new int[2];
    private final String[] lastJudgements = {"", ""};
    private final long[] judgementTimesMs = {-1, -1};

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

            // record the rating and score for the side that hit the note
            int side = voice - 1;

            lastJudgements[side] = getRating(closestDifference);
            judgementTimesMs[side] = songTimeMs;
            scores[side] += getScore(closestDifference);
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

                // record the miss and apply its score penalty
                int side = note.voice() - 1;

                lastJudgements[side] = "MISS";
                judgementTimesMs[side] = songTimeMs;
                scores[side] -= 100;
            }
        }
    }

    public boolean isHit(int index) {
        return hitNotes[index];
    }

    public boolean isMissed(int index) {
        return missedNotes[index];
    }

    public static String getRating(long timingDifferenceMs) {
        // use the distance from the note time for early and late presses
        long difference = Math.abs(timingDifferenceMs);

        if (difference <= 45) {
            return "SICK";
        }

        if (difference <= 90) {
            return "GOOD";
        }

        if (difference <= 135) {
            return "BAD";
        }

        if (difference <= 160) {
            return "SHIT";
        }

        return "MISS";
    }

    public static int getScore(long timingDifferenceMs) {
        // calculate the score from the exact timing difference
        long difference = Math.abs(timingDifferenceMs);

        if (difference > 160) {
            return -100;
        }

        if (difference < 5) {
            return 500;
        }

        double factor = 1.0 - (
                1.0 / (1.0 + Math.exp(-0.080 * (difference - 54.99)))
        );

        return (int) (500 * factor + 9);
    }
    public String getVisibleJudgement(int voice, long songTimeMs) {
        int side = voice - 1;
        long judgementTime = judgementTimesMs[side];

        // hide the judgement after 700 ms
        if (judgementTime < 0 || songTimeMs - judgementTime >= 700) {
            return "";
        }

        return lastJudgements[side];
    }

    public boolean[] updateBot(int voice, long songTimeMs) {
        boolean[] botKeys = new boolean[4];
        int side = voice - 1;

        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);

            // sustained notes will be handled separately
            if (note.voice() != voice || note.durationMs() > 0) {
                continue;
            }

            // let the bot hit the note when its scheduled time arrives
            if (songTimeMs >= note.timeMs() && !hitNotes[i]) {
                hitNotes[i] = true;

                // the bot hits normal notes with perfect timing
                scores[side] += getScore(0);
            }

            // light the receptor briefly after the note is hit
            if (songTimeMs >= note.timeMs()
                    && songTimeMs < note.timeMs() + 100) {
                botKeys[note.lane()] = true;
            }
        }

        return botKeys;
    }
}