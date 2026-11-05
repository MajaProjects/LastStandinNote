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
}
