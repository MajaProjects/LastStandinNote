public final class ScreenAudio implements AutoCloseable {

    private enum Mode { SILENT, PAUSE, DEATH, GAME_OVER, RESULTS }

    private final MusicPlayer pauseMusic;
    private final MusicPlayer gameOverMusic;
    private final MusicPlayer dyingSound;
    private final MusicPlayer completeSound;
    private final MusicPlayer retrySound;
    private Mode mode = Mode.SILENT;
    private Screen owner;
    private boolean closed;

    public ScreenAudio() {
        // use one playback channel per cue and release partial loads on failure
        MusicPlayer[] loaded = new MusicPlayer[5];
        try {
            loaded[0] = new MusicPlayer("/audio/pause_music.wav");
            loaded[1] = new MusicPlayer("/audio/gameover_music.wav");
            loaded[2] = new MusicPlayer("/audio/dying_sound.wav", AudioSettings.Channel.EFFECTS);
            loaded[3] = new MusicPlayer("/audio/level_complete.wav", AudioSettings.Channel.EFFECTS);
            loaded[4] = new MusicPlayer("/audio/retry_confirm.wav", AudioSettings.Channel.EFFECTS);
        } catch (RuntimeException error) {
            for (MusicPlayer player : loaded) {
                if (player != null) player.close();
            }
            throw error;
        }
        pauseMusic = loaded[0];
        gameOverMusic = loaded[1];
        dyingSound = loaded[2];
        completeSound = loaded[3];
        retrySound = loaded[4];
    }

    public void update(Screen screen) {
        if (closed) return;
        boolean confirmation = screen instanceof ExitConfirmScreen;
        if (screen instanceof ExitConfirmScreen exit) {
            // opening or cancelling a dialog must not replay the underlying cue
            screen = exit.getPreviousScreen();
        }

        Mode next = Mode.SILENT;
        if (screen instanceof PauseScreen
                || confirmation && (screen instanceof GameScreen || screen instanceof CountdownScreen)) {
            next = Mode.PAUSE;
        } else if (screen instanceof GameOverScreen ended) {
            // use the same phase boundary as the existing death animation
            next = ended.isMenuReady() ? Mode.GAME_OVER : Mode.DEATH;
        } else if (screen instanceof ResultsScreen) {
            next = Mode.RESULTS;
        }
        if (next == mode && (owner == screen || next == Mode.PAUSE || next == Mode.SILENT)) {
            owner = screen;
            return;
        }

        stopActive();
        mode = next;
        owner = screen;
        switch (mode) {
            case PAUSE -> pauseMusic.playLoop();
            case DEATH -> dyingSound.play();
            case GAME_OVER -> gameOverMusic.playLoop();
            case RESULTS -> completeSound.play();
            case SILENT -> { }
        }
    }

    public void playRetry() {
        if (closed) return;
        // stop pause or game over music before confirming a new attempt
        stopActive();
        mode = Mode.SILENT;
        owner = null;
        retrySound.play();
    }

    private void stopActive() {
        switch (mode) {
            case PAUSE -> pauseMusic.stop();
            case DEATH -> dyingSound.stop();
            case GAME_OVER -> gameOverMusic.stop();
            case RESULTS -> completeSound.stop();
            case SILENT -> { }
        }
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        pauseMusic.close();
        gameOverMusic.close();
        dyingSound.close();
        completeSound.close();
        retrySound.close();
    }
}
