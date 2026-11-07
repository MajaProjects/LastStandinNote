import java.util.List;

public final class HealthState {

    public static final int MAX_HEALTH = 100;
    private final List<Note> notes;
    private final boolean[] countedHeads;
    private final boolean[] countedMisses;
    private final int playerVoice;
    private final boolean twoPlayers;
    private final double[] noteWeights = {1.0, 1.0};
    private final double botNoteWeight;
    // one shared balance: windows owns the left portion and tux owns the right
    private double tuxHealth = MAX_HEALTH / 2.0;

    public HealthState(List<Note> notes) {
        this(notes, 2, true);
    }

    public HealthState(List<Note> notes, int playerVoice, boolean twoPlayers) {
        this.notes = List.copyOf(notes);
        this.playerVoice = playerVoice;
        this.twoPlayers = twoPlayers;
        countedHeads = new boolean[notes.size()];
        countedMisses = new boolean[notes.size()];
        int[] totals = new int[2];
        for (Note note : this.notes) {
            totals[note.voice() - 1]++;
        }
        int botSide = playerVoice == 1 ? 1 : 0;
        botNoteWeight = totals[botSide] == 0 ? 0.0
                : totals[playerVoice - 1] / (double) totals[botSide];
        if (twoPlayers) {
            int smallerChart = Math.min(totals[0], totals[1]);
            for (int side = 0; side < 2; side++) {
                // give both charts the same total influence on shared health
                noteWeights[side] = totals[side] == 0 ? 0.0
                        : smallerChart / (double) totals[side];
            }
        }
    }

    public void update(NoteJudge judge) {
        update(judge, -1);
    }

    public void update(NoteJudge judge, long songTimeMs) {
        if (isGameOver()) {
            return;
        }
        double movement = 0;
        double botMovement = 0;
        long humanAge = judge.getJudgementAgeMs(playerVoice, songTimeMs);
        boolean applyPressure = !twoPlayers && songTimeMs >= 0
                && judge.getBotChallengeLevel() > 0 && humanAge >= 0 && humanAge <= 1500;
        for (int i = 0; i < notes.size(); i++) {
            int voice = notes.get(i).voice();
            boolean bot = !twoPlayers && voice != playerVoice;
            int direction = voice == 2 ? 1 : -1;
            if (!countedHeads[i] && judge.getHeadHitTimeMs(i) >= 0) {
                countedHeads[i] = true;
                // judge the head once; sustain completion must not grant health again
                int change = switch (judge.getHeadRating(i)) {
                    case "SICK" -> 2;
                    case "GOOD" -> 1;
                    case "BAD" -> -3;
                    case "SHIT" -> -5;
                    default -> 0;
                };
                if (bot) {
                    // consume old events even while pressure is off, preventing catch-up damage
                    if (applyPressure) {
                        double pressure = change * botNoteWeight * (1 + judge.getBotChallengeLevel());
                        botMovement += direction * Math.max(-6.0, Math.min(6.0, pressure));
                    }
                } else {
                    movement += direction * change * noteWeights[voice - 1];
                }
            }
            if (!countedMisses[i] && judge.isMissed(i)) {
                countedMisses[i] = true;
                if (bot) {
                    if (applyPressure) {
                        botMovement -= direction * Math.min(6.0, 8 * botNoteWeight);
                    }
                } else {
                    movement += direction * -8 * noteWeights[voice - 1];
                }
            }
        }
        // combine events from this update before clamping the shared balance
        tuxHealth = Math.max(0, Math.min(MAX_HEALTH, tuxHealth + movement));
        if (!twoPlayers && !isDefeated(playerVoice)) {
            double humanLife = playerVoice == 2 ? tuxHealth : MAX_HEALTH - tuxHealth;
            double pressure = playerVoice == 2 ? botMovement : -botMovement;
            // bot recovery can take the lead, but cannot kill a player during a resting turn
            pressure = Math.max(pressure, -Math.max(0, humanLife - 30));
            humanLife = Math.max(0, Math.min(MAX_HEALTH, humanLife + pressure));
            tuxHealth = playerVoice == 2 ? humanLife : MAX_HEALTH - humanLife;
        }
        // snap tiny rounding residues at the ends so fractional weights cannot prevent defeat
        if (tuxHealth < 0.000000001) {
            tuxHealth = 0;
        } else if (MAX_HEALTH - tuxHealth < 0.000000001) {
            tuxHealth = MAX_HEALTH;
        }
    }

    public int getHealth(int voice) {
        int right = (int) Math.round(tuxHealth);
        return voice == 2 ? right : MAX_HEALTH - right;
    }

    public boolean isDefeated(int voice) {
        // shared play always continues until the song ends, even at an empty bar
        return !twoPlayers && voice == playerVoice
                && (voice == 2 ? tuxHealth == 0 : tuxHealth == MAX_HEALTH);
    }

    public boolean isGameOver() {
        return !twoPlayers && isDefeated(playerVoice);
    }
}
