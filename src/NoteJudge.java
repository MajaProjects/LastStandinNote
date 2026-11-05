import java.util.List;
import java.util.Random;
public final class NoteJudge {

    private final List<Note> notes;
    private final boolean[] hitNotes;
    // track sustained notes that are currently being held
    private final boolean[] activeSustains;
    // remember the head timing for each sustained note
    private final long[] sustainDifferencesMs;
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
        this.activeSustains = new boolean[notes.size()];
        this.sustainDifferencesMs = new long[notes.size()];
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
    public boolean isComboMilestone(int voice) {
        int totalNotes = 0;
        int combo = combos[voice - 1];

        // count all notes belonging to this side
        for (Note note : notes) {
            if (note.voice() == voice) {
                totalNotes++;
            }
        }

        if (totalNotes == 0 || combo == 0) {
            return false;
        }

        // check the 20, 40, 60, 80 and 100 percent milestones
        for (int step = 1; step <= 5; step++) {
            int milestone = (totalNotes * step + 4) / 5;

            if (combo == milestone) {
                return true;
            }
        }

        return false;
    }

    public int tryHit(int voice, int lane, long songTimeMs) {
        int closestIndex = -1;
        long closestDifference = Long.MAX_VALUE;

        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);

            // ignore notes that are finished or already being held
            if (hitNotes[i] || missedNotes[i] || activeSustains[i]) {
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

        if (closestIndex == -1) {
            return -1;
        }

        Note selectedNote = notes.get(closestIndex);

        if (selectedNote.durationMs() > 0) {
            // start the sustained note and keep the head timing
            activeSustains[closestIndex] = true;
            sustainDifferencesMs[closestIndex] = closestDifference;

            return closestIndex;
        }

        // complete and score a normal note immediately
        hitNotes[closestIndex] = true;

        int side = voice - 1;

        lastJudgements[side] = getRating(closestDifference);
        judgementTimesMs[side] = songTimeMs;
        scores[side] += getScore(closestDifference);
        combos[side]++;

        return closestIndex;
    }

    public int updateMisses(
            long songTimeMs,
            boolean judgeVoiceOne,
            boolean judgeVoiceTwo
    ) {
        int newMisses = 0;

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

                int side = note.voice() - 1;

                lastJudgements[side] = "MISS";
                judgementTimesMs[side] = songTimeMs;
                scores[side] -= 100;
                combos[side] = 0;

                // report this new miss to the game screen
                newMisses++;
            }
        }
        return newMisses;
    }
    public int[] updateSustains(
            long songTimeMs,
            boolean[][] heldKeys,
            boolean judgeVoiceOne,
            boolean judgeVoiceTwo
    ) {
        // store completed and missed sustains for each side
        int[] results = new int[4];

        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);

            // only process sustained notes that are still pending
            if (note.durationMs() <= 0 || hitNotes[i] || missedNotes[i]) {
                continue;
            }

            // only judge sides controlled by human players
            if (note.voice() == 1 && !judgeVoiceOne) {
                continue;
            }

            if (note.voice() == 2 && !judgeVoiceTwo) {
                continue;
            }

            int side = note.voice() - 1;

            if (!activeSustains[i]) {
                // miss the note if its head was not pressed in time
                if (songTimeMs > note.timeMs() + hitWindowMs) {
                    missedNotes[i] = true;

                    lastJudgements[side] = "MISS";
                    judgementTimesMs[side] = songTimeMs;
                    scores[side] -= 100;
                    combos[side] = 0;

                    results[side + 2]++;
                }

                continue;
            }

            // check whether the player is still holding this lane
            boolean stillHeld = heldKeys[side][note.lane()];

            if (!stillHeld) {
                // releasing the key early breaks the sustained note
                activeSustains[i] = false;
                missedNotes[i] = true;

                lastJudgements[side] = "MISS";
                judgementTimesMs[side] = songTimeMs;
                scores[side] -= 100;
                combos[side] = 0;

                results[side + 2]++;
                continue;
            }

            long endTimeMs = note.timeMs() + note.durationMs();

            if (songTimeMs >= endTimeMs) {
                // complete the note after it has been held to the end
                activeSustains[i] = false;
                hitNotes[i] = true;

                long difference = sustainDifferencesMs[i];

                lastJudgements[side] = getRating(difference);
                judgementTimesMs[side] = songTimeMs;
                scores[side] += getScore(difference);
                combos[side]++;

                results[side]++;
            }
        }

        return results;
    }
    public boolean isHit(int index) {
        return hitNotes[index];
    }
    public boolean isMissed(int index) {
        return missedNotes[index];
    }
    public boolean isSustainActive(int index) {
        return activeSustains[index];
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

            // only process notes belonging to the bot's side
            if (note.voice() != voice) {
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
            long difference = Math.abs(botTimingOffsets[i]);

            if (note.durationMs() > 0) {
                // start holding the sustained note at the chosen time
                if (!activeSustains[i]
                        && !hitNotes[i]
                        && !missedNotes[i]
                        && songTimeMs >= botHitTime) {
                    activeSustains[i] = true;
                    sustainDifferencesMs[i] = difference;
                }

                if (activeSustains[i]) {
                    long endTimeMs = note.timeMs() + note.durationMs();

                    if (songTimeMs >= endTimeMs) {
                        // complete and score the sustained note
                        activeSustains[i] = false;
                        hitNotes[i] = true;

                        lastJudgements[side] = getRating(difference);
                        judgementTimesMs[side] = songTimeMs;
                        scores[side] += getScore(difference);
                        combos[side]++;
                    } else {
                        // keep the bot's receptor pressed until the note ends
                        botKeys[note.lane()] = true;
                    }
                }

                continue;
            }

            // hit and score a normal note at the chosen time
            if (songTimeMs >= botHitTime
                    && !hitNotes[i]
                    && !missedNotes[i]) {
                hitNotes[i] = true;

                lastJudgements[side] = getRating(difference);
                judgementTimesMs[side] = songTimeMs;
                scores[side] += getScore(difference);
                combos[side]++;
            }

            // light the receptor briefly after a normal note is hit
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
