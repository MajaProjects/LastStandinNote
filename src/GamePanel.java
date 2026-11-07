import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;

public class GamePanel extends JPanel {

    private Screen pantallaActual;
    private SwingWorker<GameScreen, Void> songLoader;
    // track physical keys even while the gameplay screen is paused
    private final Set<Integer> pressedGameplayKeys = new HashSet<>();
    private final Set<Integer> pressedActionKeys = new HashSet<>();

    private SongSelectScreen previousSongScreen;
    private ModeSelectScreen previousModeScreen;
    private CharacterSelectScreen previousCharacterScreen;
    // keep the chosen difficulty while moving between screens
    private Difficulty selectedDifficulty = Difficulty.ADMIN;
    private final GameHud gameHud = new GameHud();
    private final Timer animationTimer;
    private final MusicPlayer countdownSound = new MusicPlayer(
            "/audio/countdown.wav", AudioSettings.Channel.EFFECTS
    );
    // load the interface sounds once
    private final AudioPlayer menuMove = new AudioPlayer(
            "/audio/menu_move.wav"
    );

    private final AudioPlayer menuConfirm = new AudioPlayer(
            "/audio/menu_confirm.wav"
    );

    private final AudioPlayer menuBack = new AudioPlayer(
            "/audio/menu_back.wav"
    );

    // keep both menu tracks ready for screen transitions
    private final MusicPlayer titleMusic = new MusicPlayer(
            "/audio/title_music.wav"
    );

    private final MusicPlayer menuMusic = new MusicPlayer(
            "/audio/menu_music.wav"
    );

    public GamePanel() {
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(960, 540));

        pantallaActual = new TitleScreen();
        titleMusic.playLoop();

        // these actions run once per press, even if the key is held
        bindSinglePress("openMenu", KeyEvent.VK_ENTER, this::activateSelection);
        bindSinglePress("backToTitle", KeyEvent.VK_ESCAPE, this::goBack);
        bindSinglePress("pauseGame", KeyEvent.VK_P, this::togglePause);

        // navigate menus and song selections vertically
        bindMenuNavigation("menuUp", KeyEvent.VK_UP, -1);
        bindMenuNavigation("menuDown", KeyEvent.VK_DOWN, 1);

        // select a character horizontally
        bindCharacterNavigation("characterLeft", KeyEvent.VK_LEFT, -1);
        bindCharacterNavigation("characterRight", KeyEvent.VK_RIGHT, 1);

        // register each physical key once and resolve its lane during gameplay
        bindGameplayKey(KeyEvent.VK_A);
        bindGameplayKey(KeyEvent.VK_S);
        bindGameplayKey(KeyEvent.VK_D);
        bindGameplayKey(KeyEvent.VK_F);
        bindGameplayKey(KeyEvent.VK_J);
        bindGameplayKey(KeyEvent.VK_K);
        bindGameplayKey(KeyEvent.VK_L);
        bindGameplayKey(KeyEvent.getExtendedKeyCodeForChar('Ñ'));

        // losing focus pauses the song before keys can become stuck
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addPropertyChangeListener("focusedWindow", event -> {
                    if (event.getNewValue() != SwingUtilities.getWindowAncestor(this)) {
                        onFocusLost();
                    }
                });

        // only the active song advances; countdowns use their own clock
        animationTimer = new Timer(16, event -> {
            updateCurrentScreen();
            repaint();
        });

        animationTimer.start();
    }

    private void bindSinglePress(String actionName, int keyCode, Runnable action) {
        String releasedAction = actionName + "Released";
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(keyCode, 0), actionName
        );
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(keyCode, 0, true), releasedAction
        );

        getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (pressedActionKeys.add(keyCode)) {
                    action.run();
                }
            }
        });
        getActionMap().put(releasedAction, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                pressedActionKeys.remove(keyCode);
            }
        });
    }

    private void goBack() {
        if (pantallaActual instanceof ExitConfirmScreen confirmation) {
            cancelExit(confirmation);
            return;
        }
        if (pantallaActual instanceof OptionsScreen options) {
            pantallaActual = options.getPreviousMenu();
            menuBack.play();
            repaint();
            return;
        }
        if (pantallaActual instanceof GameOverScreen gameOver) {
            if (!gameOver.isMenuReady()) {
                return;
            }
            returnToSongs(gameOver.getGame());
            return;
        }
        if (pantallaActual instanceof LoadingScreen loading) {
            // let a completed background load release its own audio safely
            loading.abandon();
            pantallaActual = loading.getReturnScreen();
            menuMusic.playLoop();
            menuBack.play();
            repaint();
            return;
        }

        if (pantallaActual instanceof ResultsScreen results) {
            returnToSongs(results.getGame());
            return;
        }

        if (pantallaActual instanceof CountdownScreen countdown) {
            showExitConfirmation(false);
            return;
        }

        if (pantallaActual instanceof PauseScreen pause) {
            requestResume(pause);
            return;
        }

        if (pantallaActual instanceof GameScreen game) {
            showExitConfirmation(false);
            return;
        } else if (pantallaActual instanceof CharacterSelectScreen) {
            pantallaActual = previousSongScreen;
        } else if (pantallaActual instanceof SongSelectScreen) {
            pantallaActual = previousModeScreen;
        } else if (pantallaActual instanceof ModeSelectScreen) {
            pantallaActual = new MenuScreen();
        } else if (pantallaActual instanceof MenuScreen) {
            pantallaActual = new TitleScreen();
            menuMusic.stop();
            titleMusic.playLoop();
        } else {
            return;
        }

        menuBack.play();
        repaint();
    }

    private void togglePause() {
        if (pantallaActual instanceof CountdownScreen countdown) {
            countdown.stop();
            pantallaActual = new PauseScreen(countdown.getGame());
            repaint();
        } else if (pantallaActual instanceof GameScreen game) {
            game.pause();
            pantallaActual = game.isGameOver() ? new GameOverScreen(game) : new PauseScreen(game);
            repaint();
        } else if (pantallaActual instanceof PauseScreen pause) {
            requestResume(pause);
        }
    }

    private void requestResume(PauseScreen pause) {
        GameScreen game = pause.getGame();
        String missingKeys = game.getMissingResumeKeys(pressedGameplayKeys);
        pause.requestResume(missingKeys);

        if (missingKeys.isEmpty()) {
            countdownSound.stop();
            // continue at the saved song position without replaying the intro countdown
            if (game.resume(pressedGameplayKeys)) {
                pantallaActual = game;
                menuConfirm.play();
            } else if (game.isGameOver()) {
                pantallaActual = new GameOverScreen(game);
            }
        }

        repaint();
    }

    private void updateCurrentScreen() {
        if (pantallaActual instanceof GameScreen game) {
            game.update();
            if (game.isGameOver()) {
                game.pause();
                pantallaActual = new GameOverScreen(game);
            } else if (game.isFinished()) {
                // finish the last gameplay update before freezing the result
                game.pause();
                pantallaActual = new ResultsScreen(game);
            }
        } else if (pantallaActual instanceof CountdownScreen countdown) {
            // check held sustains before allowing the music clock to restart
            if (!checkCountdownKeys(countdown)) {
                return;
            }

            if (countdown.isFinished()
                    && countdown.getGame().resume(pressedGameplayKeys)) {
                countdown.stop();
                pantallaActual = countdown.getGame();
            }
        }
    }

    private boolean checkCountdownKeys(CountdownScreen countdown) {
        String missingKeys = countdown.getGame()
                .getMissingResumeKeys(pressedGameplayKeys);
        if (missingKeys.isEmpty()) {
            return true;
        }

        // releasing a required hold returns to pause without judging a miss
        countdown.stop();
        PauseScreen pause = new PauseScreen(countdown.getGame());
        pause.requestResume(missingKeys);
        pantallaActual = pause;
        return false;
    }

    private void onFocusLost() {
        pressedGameplayKeys.clear();
        pressedActionKeys.clear();

        if (pantallaActual instanceof LoadingScreen loading) {
            loading.pauseAfterLoad();
        } else if (pantallaActual instanceof GameScreen
                || pantallaActual instanceof CountdownScreen) {
            togglePause();
        } else if (pantallaActual instanceof PauseScreen pause) {
            pause.cancelResume();
        }
    }

    private SongRequest prepareSongRequest(CharacterSelectScreen characters) {
        String folder = previousSongScreen.getSelectedSongFolder();
        Difficulty difficulty = previousSongScreen.getDifficulty();
        String name = difficulty.name().toLowerCase(java.util.Locale.ROOT);
        String chartPath = folder + "chart_" + name + ".txt";

        // capture menu choices on the event thread before starting background work
        return new SongRequest(
                folder, chartPath,
                characters.getSelectedCharacter(), characters.getOtherCharacter(),
                previousModeScreen.getSelectedMode().equals("DOS JUGADORES"),
                difficulty
        );
    }

    private record SongRequest(String folder, String chartPath, String playerCharacter,
                               String opponentCharacter, boolean twoPlayers, Difficulty difficulty) {
        GameScreen createGame() {
            String musicPath = folder + "instrumental.wav";
            if (GamePanel.class.getResource(chartPath) == null
                    || GamePanel.class.getResource(musicPath) == null) {
                throw new IllegalStateException("Missing song files:\n" + chartPath + "\n" + musicPath);
            }
            return new GameScreen(folder, chartPath, playerCharacter, opponentCharacter,
                    twoPlayers, difficulty);
        }
    }

    private void beginSongLoad(CharacterSelectScreen characters, Screen returnScreen) {
        SongRequest request = prepareSongRequest(characters);
        LoadingScreen loading = new LoadingScreen(returnScreen);
        pantallaActual = loading;
        menuMusic.stop();

        songLoader = new SwingWorker<>() {
            @Override
            protected GameScreen doInBackground() {
                if (loading.isAbandoned()) {
                    return null;
                }
                GameScreen game = request.createGame();
                if (loading.isAbandoned()) {
                    game.close();
                    return null;
                }
                return game;
            }

            @Override
            protected void done() {
                GameScreen game = null;
                try {
                    game = get();
                    if (loading.isAbandoned() || pantallaActual != loading) {
                        if (game != null) {
                            game.close();
                        }
                        return;
                    }
                    if (game == null) {
                        pantallaActual = returnScreen;
                        menuMusic.playLoop();
                        return;
                    }
                    pantallaActual = loading.shouldPauseAfterLoad()
                            ? new PauseScreen(game) : new CountdownScreen(game, countdownSound);
                    menuConfirm.play();
                } catch (Exception error) {
                    if (game != null) {
                        game.close();
                    }
                    if (!loading.isAbandoned() && pantallaActual == loading) {
                        pantallaActual = returnScreen;
                        menuMusic.playLoop();
                        Throwable cause = error.getCause() == null ? error : error.getCause();
                        JOptionPane.showMessageDialog(GamePanel.this,
                                "Could not load song:\n" + cause.getMessage());
                    }
                } finally {
                    if (songLoader == this) {
                        songLoader = null;
                    }
                    repaint();
                }
            }
        };
        songLoader.execute();
        repaint();
    }

    private void restartGame(GameScreen previousGame) {
        // release the previous audio channels before loading the song again
        previousGame.close();

        try {
            beginSongLoad(previousCharacterScreen, previousSongScreen);
        } catch (RuntimeException error) {
            pantallaActual = previousSongScreen;
            menuMusic.playLoop();
            JOptionPane.showMessageDialog(
                    this, "Could not restart song:\n" + error.getMessage()
            );
        }

        repaint();
    }

    private void returnToSongs(GameScreen game) {
        game.close();
        pantallaActual = previousSongScreen;
        menuMusic.playLoop();
        menuBack.play();
        repaint();
    }

    public void requestApplicationExit() {
        showExitConfirmation(true);
    }

    private GameScreen getActiveGame(Screen screen) {
        if (screen instanceof GameScreen game) return game;
        if (screen instanceof CountdownScreen countdown) return countdown.getGame();
        if (screen instanceof PauseScreen pause) return pause.getGame();
        if (screen instanceof ResultsScreen results) return results.getGame();
        if (screen instanceof GameOverScreen ended) return ended.getGame();
        return null;
    }

    private void showExitConfirmation(boolean applicationExit) {
        if (pantallaActual instanceof ExitConfirmScreen) {
            return;
        }
        if (pantallaActual instanceof LoadingScreen loading) {
            // abandon the background load safely so cancelling exit cannot strand a loading screen
            loading.abandon();
            pantallaActual = loading.getReturnScreen();
            menuMusic.playLoop();
        }
        Screen previous = pantallaActual;
        GameScreen game = getActiveGame(previous);
        if (previous instanceof CountdownScreen countdown) {
            countdown.stop();
        } else if (previous instanceof GameScreen) {
            game.pause();
            if (game.isGameOver()) {
                pantallaActual = new GameOverScreen(game);
                if (!applicationExit) {
                    repaint();
                    return;
                }
                previous = pantallaActual;
            }
        } else if (previous instanceof PauseScreen pause) {
            pause.cancelResume();
        }
        pantallaActual = new ExitConfirmScreen(previous, game, applicationExit);
        menuBack.play();
        repaint();
    }

    private void cancelExit(ExitConfirmScreen confirmation) {
        Screen previous = confirmation.getPreviousScreen();
        pantallaActual = previous;
        if (previous instanceof GameScreen || previous instanceof CountdownScreen) {
            // resume through the held-key check without replaying the intro countdown
            PauseScreen pause = new PauseScreen(confirmation.getGame());
            pantallaActual = pause;
            requestResume(pause);
        } else {
            menuBack.play();
            repaint();
        }
    }

    private void confirmExit(ExitConfirmScreen confirmation) {
        if (!confirmation.isApplicationExit()) {
            returnToSongs(confirmation.getGame());
            return;
        }
        animationTimer.stop();
        if (confirmation.getGame() != null) {
            confirmation.getGame().close();
        }
        countdownSound.close();
        titleMusic.close();
        menuMusic.close();
        menuMove.close();
        menuConfirm.close();
        menuBack.close();
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
    }

    private void bindMenuNavigation(
            String actionName,
            int keyCode,
            int direction
    ) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(keyCode, 0),
                actionName
        );

        getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                // move the selection in the active menu
                if (pantallaActual instanceof MenuScreen menu) {
                    menu.moveSelection(direction);
                } else if (pantallaActual instanceof OptionsScreen options) {
                    options.moveSelection(direction);
                } else if (pantallaActual instanceof ExitConfirmScreen confirmation) {
                    confirmation.moveSelection(direction);
                } else if (pantallaActual instanceof ModeSelectScreen modes) {
                    modes.moveSelection(direction);
                } else if (pantallaActual instanceof SongSelectScreen songs) {
                    songs.moveSelection(direction);
                } else if (pantallaActual instanceof PauseScreen pause) {
                    pause.moveSelection(direction);
                } else if (pantallaActual instanceof ResultsScreen results) {
                    results.moveSelection(direction);
                } else if (pantallaActual instanceof GameOverScreen gameOver) {
                    gameOver.moveSelection(direction);
                } else {
                    return;
                }

                menuMove.play();
                repaint();
            }
        });
    }

    private void bindCharacterNavigation(
            String actionName,
            int keyCode,
            int direction
    ) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(keyCode, 0),
                actionName
        );

        getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (pantallaActual instanceof ExitConfirmScreen confirmation) {
                    confirmation.moveSelection(direction);
                    menuMove.play();
                    repaint();
                    return;
                }
                if (pantallaActual instanceof OptionsScreen options) {
                    options.changeSelectedValue(direction);
                    menuMove.play();
                    repaint();
                    return;
                }
                if (pantallaActual instanceof GameOverScreen gameOver) {
                    gameOver.moveSelection(direction);
                    menuMove.play();
                    repaint();
                    return;
                }
                if (pantallaActual instanceof ResultsScreen results) {
                    results.moveSelection(direction);
                    menuMove.play();
                    repaint();
                    return;
                }

                if (pantallaActual instanceof CharacterSelectScreen characters) {
                    // move between characters
                    characters.moveSelection(direction);
                    menuMove.play();
                    repaint();
                    return;
                }

                if (pantallaActual instanceof SongSelectScreen songs) {
                    // change difficulty from anywhere on the song screen
                    songs.changeDifficulty(direction);
                    selectedDifficulty = songs.getDifficulty();

                    menuMove.play();
                    repaint();
                }
            }
        });
    }

    private void bindGameplayKey(int keyCode) {
        // handle both pressing and releasing the physical key
        for (boolean pressed : new boolean[]{true, false}) {
            String actionName = "game_" + keyCode + "_" + pressed;

            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke(keyCode, 0, !pressed),
                    actionName
            );

            getActionMap().put(actionName, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    boolean changed = pressed
                            ? pressedGameplayKeys.add(keyCode)
                            : pressedGameplayKeys.remove(keyCode);

                    if (!changed) {
                        return;
                    }

                    if (pantallaActual instanceof GameScreen game) {
                        game.setKeyPressed(keyCode, pressed);
                    } else if (pantallaActual instanceof PauseScreen pause
                            && pause.isResumeRequested()) {
                        requestResume(pause);
                    } else if (pantallaActual instanceof CountdownScreen countdown) {
                        // track physical keys without judging note heads yet
                        checkCountdownKeys(countdown);
                    }

                    repaint();
                }
            });
        }
    }

    private void activateSelection() {
        if (pantallaActual instanceof ExitConfirmScreen confirmation) {
            if (confirmation.isExitSelected()) {
                confirmExit(confirmation);
            } else {
                cancelExit(confirmation);
            }
            return;
        }
        if (pantallaActual instanceof OptionsScreen options) {
            if (options.toggleSelected()) {
                menuConfirm.play();
                repaint();
            }
            return;
        }
        if (pantallaActual instanceof MenuScreen menu
                && (menu.getSelectedOption().equals("OPCIONES")
                || menu.getSelectedOption().equals("OPTIONS"))) {
            pantallaActual = new OptionsScreen(menu);
            menuConfirm.play();
            repaint();
            return;
        }
        if (pantallaActual instanceof GameOverScreen gameOver) {
            if (!gameOver.isMenuReady()) {
                return;
            }
            switch (gameOver.getSelectedOption()) {
                case "RESTART" -> restartGame(gameOver.getGame());
                case "SONG SELECT" -> returnToSongs(gameOver.getGame());
            }
            return;
        }
        if (pantallaActual instanceof ResultsScreen results) {
            switch (results.getSelectedOption()) {
                case "RESTART" -> restartGame(results.getGame());
                case "SONG SELECT" -> returnToSongs(results.getGame());
            }
            return;
        }

        if (pantallaActual instanceof PauseScreen pause) {
            switch (pause.getSelectedOption()) {
                case "RESUME" -> requestResume(pause);
                case "RESTART" -> restartGame(pause.getGame());
                case "SONG SELECT" -> showExitConfirmation(false);
            }
            return;
        }

        if (pantallaActual instanceof TitleScreen) {
            pantallaActual = new MenuScreen();

            titleMusic.stop();
            menuMusic.playLoop();

            menuConfirm.play();
            repaint();
            return;
        }

        if (pantallaActual instanceof MenuScreen menu
                && menu.getSelectedOption().equals("JUGAR")) {
            pantallaActual = new ModeSelectScreen();

            menuConfirm.play();
            repaint();
            return;
        }

        if (pantallaActual instanceof ModeSelectScreen modes) {
            // preserve the chosen mode before opening the song list
            previousModeScreen = modes;
            pantallaActual = new SongSelectScreen(selectedDifficulty);

            menuConfirm.play();
            repaint();
            return;
        }

        if (pantallaActual instanceof SongSelectScreen songs) {
            // preserve the song selection before choosing a character
            previousSongScreen = songs;

            boolean twoPlayers = previousModeScreen
                    .getSelectedMode()
                    .equals("DOS JUGADORES");

            pantallaActual = new CharacterSelectScreen(
                    songs.getSelectedSong(),
                    twoPlayers
            );

            menuConfirm.play();
            repaint();
            return;
        }

        if (pantallaActual instanceof CharacterSelectScreen characters) {
            try {
                previousCharacterScreen = characters;
                beginSongLoad(characters, characters);
            } catch (RuntimeException error) {
                JOptionPane.showMessageDialog(
                        this, "Could not start song:\n" + error.getMessage()
                );
            }

            repaint();
            return;
        }

        if (pantallaActual instanceof MenuScreen menu
                && menu.getSelectedOption().equals("SALIR")) {
            requestApplicationExit();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        try {
            // preserve pixel edges when scaling the artwork
            g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
            );

            pantallaActual.render(g2, getWidth(), getHeight());
            gameHud.recordPaint();
            if (DisplaySettings.isShowFps()) {
                gameHud.drawFps(g2, getWidth());
            }
        } finally {
            g2.dispose();
        }
    }
}

