public record Note(
        long timeMs,
        int voice,
        int lane,
        long durationMs,
        String vowel,
        double semitones
) {
    public Note {
        // reject invalid timing values
        if (timeMs < 0 || durationMs < 0) {
            throw new IllegalArgumentException("Note times cannot be negative");
        }

        // the chart identifies its two voices as 1 and 2
        if (voice < 1 || voice > 2) {
            throw new IllegalArgumentException("Note voice must be 1 or 2");
        }

        // lanes follow the order left, down, up and right
        if (lane < 0 || lane > 3) {
            throw new IllegalArgumentException("Note lane must be between 0 and 3");
        }

        if (vowel == null || vowel.isBlank()) {
            throw new IllegalArgumentException("A note needs a vowel");
        }

        if (!Double.isFinite(semitones)) {
            throw new IllegalArgumentException("Invalid note pitch");
        }
    }
}
