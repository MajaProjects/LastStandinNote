import java.util.List;
import java.util.Random;
public final class NoteJudge {

    private final List<Note> notes;
    private final boolean[] hitNotes;
    private final boolean[] missedNotes;
    private final long hitWindowMs;
    // keep the score and latest judgement for each side
    private final int[] scores = new int[2];
    // track consecutive hits separately for each side
    private final int[] combos = new int[2];
    private final String[] lastJudgements = {"", ""};
    private final long[] judgementTimesMs = {-1, -1};
    // store one timing choice for each note
    private final int[] botTimingOffsets;
    private final boolean[] botMisses;

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
        // choose the bot's result once for each note
        botTimingOffsets = new int[notes.size()];
        botMisses = new boolean[notes.size()];

        Random random = new Random();

        for (int i = 0; i < notes.size(); i++) {
            int chance = random.nextInt(100);

            if (chance < 3) {
                // three percent of notes will be missed
                botMisses[i] = true;
                continue;
            }

            int difference;

            if (chance < 78) {
                // choose a sick timing between 0 and 45 ms
                difference = random.nextInt(46);
            } else if (chance < 95) {
                // choose a good timing between 46 and 90 ms
                difference = 46 + random.nextInt(45);
            } else {
                // choose a bad timing between 91 and 135 ms
                difference = 91 + random.nextInt(45);
            }

            // choose whether the bot presses early or late
            if (random.nextBoolean()) {
                difference = -difference;
            }

            botTimingOffsets[i] = difference;
        }
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

            // increase the combo after a successful hit
            combos[side]++;
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

            // reset the combo when this side misses a note
                combos[side] = 0;
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
    public long getJudgementAgeMs(int voice, long songTimeMs) {
        int side = voice - 1;
        long judgementTime = judgementTimesMs[side];

        // report when this side has no judgement yet
        if (judgementTime < 0) {
            return -1;
        }

        // measure how long the latest judgement has been visible
        return Math.max(0, songTimeMs - judgementTime);
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

            if (botMisses[i]) {
                // judge the miss after the note's timing window has passed
                if (!missedNotes[i]
                        && !hitNotes[i]
                        && songTimeMs > note.timeMs() + hitWindowMs) {
                    missedNotes[i] = true;

                    lastJudgements[side] = "MISS";
                    judgementTimesMs[side] = songTimeMs;
                    scores[side] -= 100;
                    combos[side] = 0;
                }

                continue;
            }

            // use the timing chosen when the game started
            long botHitTime = note.timeMs() + botTimingOffsets[i];

            if (songTimeMs >= botHitTime
                    && !hitNotes[i]
                    && !missedNotes[i]) {
                hitNotes[i] = true;

                // record the bot's rating, score and combo
                long difference = Math.abs(botTimingOffsets[i]);

                lastJudgements[side] = getRating(difference);
                judgementTimesMs[side] = songTimeMs;
                scores[side] += getScore(difference);
                combos[side]++;
            }

            // light the receptor briefly after the bot hits the note
            if (hitNotes[i]
                    && songTimeMs >= botHitTime
                    && songTimeMs - botHitTime < 100) {
                botKeys[note.lane()] = true;
            }
        }

        return botKeys;
    }

    public int getScoreForVoice(int voice) {
        // return the score of the requested side
        return scores[voice - 1];
    }

    public int getComboForVoice(int voice) {
        // return the current combo for the requested side
        return combos[voice - 1];
    }
}
