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
    // keep the best combo even after a bad hit or a miss resets the current one
    private final int[] maxCombos = new int[2];
    private final String[] headRatings;
    private final long[] headHitTimesMs;
    // keep combo reactions independent from the head judgement timer
    private final long[] comboProgressTimesMs = {-1, -1};
    private final String[] lastJudgements = {"", ""};
    // remember the lane of the latest miss for each side
    private final int[] lastMissLanes = {-1, -1};
    private final long[] judgementTimesMs = {-1, -1};
    // store one timing choice for each note
    private final int[] botTimingOffsets;
    private final boolean[] botMisses;
    // remember bad hits so the renderer can show their dark sprites
    private final boolean[] badHits;
    private final long[] badHitTimesMs;
    // allow a brief release when pressing another note in the same lane
    private static final long SUSTAIN_RELEASE_GRACE_MS = 60;
    private static final long SUSTAIN_JOIN_TOLERANCE_MS = 2;

    private final long[] sustainReleaseTimesMs;


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
        sustainReleaseTimesMs = new long[notes.size()];
        java.util.Arrays.fill(sustainReleaseTimesMs, -1);
        this.sustainDifferencesMs = new long[notes.size()];
        this.hitWindowMs = hitWindowMs;
        headRatings = new String[notes.size()];
        headHitTimesMs = new long[notes.size()];
        java.util.Arrays.fill(headHitTimesMs, -1);
        badHits = new boolean[notes.size()];
        badHitTimesMs = new long[notes.size()];
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

        // show the head judgement when the key is pressed
        registerHeadJudgement(closestIndex, closestDifference, songTimeMs);

        if (selectedNote.durationMs() > 0) {
            // start the sustained note and keep the head timing
            activeSustains[closestIndex] = true;
            sustainDifferencesMs[closestIndex] = closestDifference;
            sustainReleaseTimesMs[closestIndex] = -1;

            return closestIndex;
        }

        // complete and score a normal note immediately
        hitNotes[closestIndex] = true;

        int side = voice - 1;

        scores[side] += getScore(closestDifference);
        registerComboHit(closestIndex, songTimeMs);

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
                lastMissLanes[side] = note.lane();
                judgementTimesMs[side] = songTimeMs;
                scores[side] -= 100;
                combos[side] = 0;

                // report this new miss to the game screen
                newMisses++;
            }
        }
        return newMisses;
    }
    private long getRepressDeadlineMs(int sustainIndex, long releaseTimeMs) {
        long normalDeadline = releaseTimeMs + SUSTAIN_RELEASE_GRACE_MS;
        Note sustain = notes.get(sustainIndex);
        long sustainEndMs = sustain.timeMs() + sustain.durationMs();

        for (int i = 0; i < notes.size(); i++) {
            Note next = notes.get(i);

            // only another pending sustained head can extend this release
            if (i == sustainIndex || next.durationMs() <= 0
                    || hitNotes[i] || missedNotes[i] || activeSustains[i]
                    || next.voice() != sustain.voice()
                    || next.lane() != sustain.lane()) {
                continue;
            }

            // allow overlapping holds and adjacent heads with rounded times
            if (next.timeMs() <= sustain.timeMs()
                    || next.timeMs() > sustainEndMs + SUSTAIN_JOIN_TOLERANCE_MS) {
                continue;
            }

            // the release itself must happen within the new head's hit window
            if (Math.abs(next.timeMs() - releaseTimeMs) > hitWindowMs) {
                continue;
            }

            // use the existing timing window; never register the new head here
            return Math.max(normalDeadline, next.timeMs() + hitWindowMs);
        }

        return normalDeadline;
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

            if (note.durationMs() <= 0
                    || hitNotes[i]
                    || missedNotes[i]) {
                continue;
            }

            boolean humanControlled = note.voice() == 1
                    ? judgeVoiceOne
                    : judgeVoiceTwo;

            if (!humanControlled) {
                continue;
            }

            int side = note.voice() - 1;

            if (!activeSustains[i]) {
                // the head still needs its own correctly timed press
                if (songTimeMs > note.timeMs() + hitWindowMs) {
                    missedNotes[i] = true;

                    lastJudgements[side] = "MISS";
                    lastMissLanes[side] = note.lane();
                    judgementTimesMs[side] = songTimeMs;
                    scores[side] -= 100;
                    combos[side] = 0;

                    results[side + 2]++;
                }

                continue;
            }

            long endTimeMs = note.timeMs() + note.durationMs();
            boolean stillHeld = heldKeys[side][note.lane()];

            if (stillHeld) {
                // resume the sustain after a brief release
                sustainReleaseTimesMs[i] = -1;
            } else {
                if (sustainReleaseTimesMs[i] < 0) {
                    sustainReleaseTimesMs[i] = songTimeMs;
                }

                // only measure the released portion before the note ends
                long releasedUntilMs = Math.min(songTimeMs, endTimeMs);
                long releasedDurationMs =
                        releasedUntilMs - sustainReleaseTimesMs[i];

                if (releasedDurationMs > SUSTAIN_RELEASE_GRACE_MS) {
                    long deadlineMs = getRepressDeadlineMs(
                            i, sustainReleaseTimesMs[i]
                    );

                    if (songTimeMs > deadlineMs) {
                        activeSustains[i] = false;
                        missedNotes[i] = true;

                        lastJudgements[side] = "MISS";
                        lastMissLanes[side] = note.lane();
                        judgementTimesMs[side] = songTimeMs;
                        scores[side] -= 100;
                        combos[side] = 0;

                        results[side + 2]++;
                    }

                    // wait for the re-press; do not award completion while released
                    continue;
                }
            }

            if (songTimeMs >= endTimeMs) {
                // complete the sustain without penalizing a brief final release
                activeSustains[i] = false;
                hitNotes[i] = true;
                sustainReleaseTimesMs[i] = -1;

                long difference = sustainDifferencesMs[i];

                // score the stored head timing without repeating its judgement
                scores[side] += getScore(difference);

                // preserve the rule that bad hits break the combo
                registerComboHit(i, songTimeMs);

                results[side]++;
            }
        }

        return results;
    }
    public int getLastMissLane(int voice) {
        // return the direction of this side's latest missed note
        return lastMissLanes[voice - 1];
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
                    lastMissLanes[side] = note.lane();
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
                    // give the bot the same head feedback as human players
                    registerHeadJudgement(i, difference, songTimeMs);
                    activeSustains[i] = true;
                    sustainDifferencesMs[i] = difference;
                }

                if (activeSustains[i]) {
                    long endTimeMs = note.timeMs() + note.durationMs();

                    if (songTimeMs >= endTimeMs) {
                        // complete and score the sustained note
                        activeSustains[i] = false;
                        hitNotes[i] = true;

                        scores[side] += getScore(difference);
                        registerComboHit(i, songTimeMs);
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
                registerHeadJudgement(i, difference, songTimeMs);
                hitNotes[i] = true;
                scores[side] += getScore(difference);
                registerComboHit(i, songTimeMs);
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

    public double getPerformanceForVoice(int voice, boolean finalResult) {
        if (voice < 1 || voice > 2) {
            throw new IllegalArgumentException("Note voice must be 1 or 2");
        }
        int opportunities = 0;
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).voice() == voice
                    && (finalResult || hitNotes[i] || missedNotes[i])) {
                opportunities++;
            }
        }
        if (opportunities == 0) {
            return 0.0;
        }
        // compare earned points with this side's own maximum, including miss penalties
        double percentage = scores[voice - 1] * 100.0 / (500.0 * opportunities);
        return Math.max(0.0, Math.min(100.0, percentage));
    }

    public int getLeadingVoice(boolean finalResult) {
        // live comparisons exclude future notes and unfinished holds on both sides
        double difference = getPerformanceForVoice(1, finalResult)
                - getPerformanceForVoice(2, finalResult);
        return Math.abs(difference) < 0.000000001 ? 0 : difference > 0 ? 1 : 2;
    }

    public int getComboForVoice(int voice) {
        // return the current combo for the requested side
        return combos[voice - 1];
    }

    public long getHeadHitTimeMs(int index) {
        return headHitTimesMs[index];
    }

    public String getHeadRating(int index) {
        return headRatings[index] == null ? "" : headRatings[index];
    }

    public record Statistics(
            int totalNotes,
            int hitNotes,
            int sick,
            int good,
            int bad,
            int shit,
            int miss,
            int maxCombo,
            int score
    ) {}

    public Statistics getStatisticsForVoice(int voice) {
        if (voice < 1 || voice > 2) {
            throw new IllegalArgumentException("Note voice must be 1 or 2");
        }

        int total = 0;
        int hits = 0;
        int sick = 0;
        int good = 0;
        int bad = 0;
        int shit = 0;
        int misses = 0;

        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).voice() != voice) {
                continue;
            }

            total++;
            if (missedNotes[i]) {
                // a dropped sustain is one miss, regardless of its head rating
                misses++;
            } else if (hitNotes[i]) {
                // count completed notes once, without counting sustain pieces
                hits++;
                switch (headRatings[i]) {
                    case "SICK" -> sick++;
                    case "GOOD" -> good++;
                    case "BAD" -> bad++;
                    case "SHIT" -> shit++;
                }
            }
        }

        return new Statistics(total, hits, sick, good, bad, shit, misses,
                maxCombos[voice - 1], scores[voice - 1]);
    }
    private void registerHeadJudgement(
            int noteIndex,
            long timingDifferenceMs,
            long songTimeMs
    ) {
        int side = notes.get(noteIndex).voice() - 1;
        String rating = getRating(timingDifferenceMs);
        headRatings[noteIndex] = rating;
        headHitTimesMs[noteIndex] = songTimeMs;

        lastJudgements[side] = rating;
        judgementTimesMs[side] = songTimeMs;
        badHits[noteIndex] = rating.equals("BAD");

        if (badHits[noteIndex]) {
            // break the combo immediately, once for this head
            combos[side] = 0;
            badHitTimesMs[noteIndex] = songTimeMs;
        }
    }

    private void registerComboHit(int noteIndex, long songTimeMs) {
        int side = notes.get(noteIndex).voice() - 1;

        // bad heads already broke the combo when they were pressed
        if (!badHits[noteIndex]) {
            combos[side]++;
            maxCombos[side] = Math.max(maxCombos[side], combos[side]);
            comboProgressTimesMs[side] = songTimeMs;
        }
    }

    public long getComboAgeMs(int voice, long songTimeMs) {
        long progressTime = comboProgressTimesMs[voice - 1];
        return progressTime < 0 ? -1 : Math.max(0, songTimeMs - progressTime);
    }

    public boolean isBadHit(int index) {
        return badHits[index];
    }

    public long getBadHitAgeMs(int index, long songTimeMs) {
        if (!badHits[index]) {
            return -1;
        }

        // measure feedback duration using the music clock
        return Math.max(0, songTimeMs - badHitTimesMs[index]);
    }
}
