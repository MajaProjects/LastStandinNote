import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GamePanel extends JPanel {

    private Screen pantallaActual;
    private SongSelectScreen previousSongScreen;
    private ModeSelectScreen previousModeScreen;
    private CharacterSelectScreen previousCharacterScreen;

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

        // confirm the current selection with enter
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                "openMenu"
        );

        getActionMap().put("openMenu", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                activateSelection();
            }
        });

        // return to the previous screen with escape
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                "backToTitle"
        );

        getActionMap().put("backToTitle", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (pantallaActual instanceof GameScreen game) {
                    game.close();
                    pantallaActual = previousCharacterScreen;
                    menuMusic.playLoop();
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
        });

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

        // clear held inputs when switching away from this window
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addPropertyChangeListener("focusedWindow", event -> {
                    if (event.getNewValue() != SwingUtilities.getWindowAncestor(this)
                            && pantallaActual instanceof GameScreen game) {
                        game.releaseKeys();
                        repaint();
                    }
                });

        // request regular redraws for interface and gameplay animations
        Timer animationTimer = new Timer(16, event -> repaint());
        animationTimer.start();
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
                } else if (pantallaActual instanceof ModeSelectScreen modes) {
                    modes.moveSelection(direction);
                } else if (pantallaActual instanceof SongSelectScreen songs) {
                    songs.moveSelection(direction);
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
                if (pantallaActual instanceof CharacterSelectScreen characters) {
                    characters.moveSelection(direction);
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
                    // let the game screen choose the player and lane
                    if (pantallaActual instanceof GameScreen game) {
                        game.setKeyPressed(keyCode, pressed);
                        repaint();
                    }
                }
            });
        }
    }

    private void activateSelection() {
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
            pantallaActual = new SongSelectScreen();

            menuConfirm.play();
            repaint();
            return;
        }

        if (pantallaActual instanceof SongSelectScreen songs) {
            // preserve the song selection and pass the selected mode
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
            String folder = previousSongScreen.getSelectedSongFolder();

            // check the required files before preparing the stage
            if (GamePanel.class.getResource(folder + "chart.txt") == null
                    || GamePanel.class.getResource(folder + "instrumental.wav") == null) {
                JOptionPane.showMessageDialog(
                        GamePanel.this,
                        "Missing chart.txt or instrumental.wav in " + folder
                );
                return;
            }

            boolean twoPlayers = previousModeScreen
                    .getSelectedMode()
                    .equals("DOS JUGADORES");

            GameScreen game = new GameScreen(
                    folder,
                    characters.getSelectedCharacter(),
                    characters.getOtherCharacter(),
                    twoPlayers
            );

            // switch screens after the gameplay resources have loaded
            previousCharacterScreen = characters;
            menuMusic.stop();
            pantallaActual = game;
            game.start();

            repaint();
            return;
        }

        if (pantallaActual instanceof MenuScreen menu
                && menu.getSelectedOption().equals("SALIR")) {
            Window window = SwingUtilities.getWindowAncestor(this);

            if (window != null) {
                window.dispatchEvent(
                        new WindowEvent(window, WindowEvent.WINDOW_CLOSING)
                );
            }
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
        } finally {
            g2.dispose();
        }
    }
}