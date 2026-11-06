import java.util.List;

public final class HealthState {

    public static final int MAX_HEALTH = 100;
    private final List<Note> notes;
    private final boolean[] countedHeads;
    private final boolean[] countedMisses;
    private final int playerVoice;
    private final boolean twoPlayers;
    // one shared balance: windows owns the left portion and tux owns the right
    private int tuxHealth = MAX_HEALTH / 2;

    public HealthState(List<Note> notes) {
        this(notes, 2, true);
    }

    public HealthState(List<Note> notes, int playerVoice, boolean twoPlayers) {
        this.notes = List.copyOf(notes);
        this.playerVoice = playerVoice;
        this.twoPlayers = twoPlayers;
        countedHeads = new boolean[notes.size()];
        countedMisses = new boolean[notes.size()];
    }

    public void update(NoteJudge judge) {
        if (isGameOver()) {
            return;
        }
        int movement = 0;
        for (int i = 0; i < notes.size(); i++) {
            int voice = notes.get(i).voice();
            if (!twoPlayers && voice != playerVoice) {
                // the bot's turn must not drain the human player's life
                continue;
            }
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
                movement += direction * change;
            }
            if (!countedMisses[i] && judge.isMissed(i)) {
                countedMisses[i] = true;
                movement += direction * -8;
            }
        }
        // combine events from this update before clamping the shared balance
        tuxHealth = Math.max(0, Math.min(MAX_HEALTH, tuxHealth + movement));
    }

    public int getHealth(int voice) {
        return voice == 2 ? tuxHealth : MAX_HEALTH - tuxHealth;
    }

    public boolean isDefeated(int voice) {
        return (twoPlayers || voice == playerVoice) && getHealth(voice) == 0;
    }

    public boolean isGameOver() {
        return twoPlayers ? tuxHealth == 0 || tuxHealth == MAX_HEALTH
                : getHealth(playerVoice) == 0;
    }
}
