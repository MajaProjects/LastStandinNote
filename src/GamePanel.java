import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GamePanel extends JPanel {

    private Screen pantallaActual;
    private SongSelectScreen previousSongScreen;
    private ModeSelectScreen previousModeScreen;
    private CharacterSelectScreen previousCharacterScreen;

    // load the menu effects once
    private final AudioPlayer menuMove = new AudioPlayer(
            "/audio/menu_move.wav"
    );

    private final AudioPlayer menuConfirm = new AudioPlayer(
            "/audio/menu_confirm.wav"
    );

    private final AudioPlayer menuBack = new AudioPlayer(
            "/audio/menu_back.wav"
    );
    // keep both music tracks loaded for screen transitions
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

        // connect escape to the back action
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                "backToTitle"
        );

        getActionMap().put("backToTitle", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (pantallaActual instanceof GameScreen game) {
                    // release the song before returning to character selection
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
        // open the title screen and start its music
        pantallaActual = new TitleScreen();
        titleMusic.playLoop();

        // use vertical arrows for menus and songs
        bindMenuNavigation("menuUp", KeyEvent.VK_UP, -1);
        bindMenuNavigation("menuDown", KeyEvent.VK_DOWN, 1);

        // use horizontal arrows for characters
        bindCharacterNavigation("characterLeft", KeyEvent.VK_LEFT, -1);
        bindCharacterNavigation("characterRight", KeyEvent.VK_RIGHT, 1);

        // redraw regularly to keep interface animations running
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
                // apply horizontal navigation on the character screen
                if (pantallaActual instanceof CharacterSelectScreen characters) {
                    characters.moveSelection(direction);
                    menuMove.play();
                    repaint();
                }
            }
        });
    }

    private void activateSelection() {
        if (pantallaActual instanceof TitleScreen) {
            pantallaActual = new MenuScreen();

            // switch tracks when entering the main menu
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
            // preserve the selected mode before opening the songs
            previousModeScreen = modes;
            pantallaActual = new SongSelectScreen();

            menuConfirm.play();
            repaint();
            return;
        }

        if (pantallaActual instanceof SongSelectScreen songs) {
            // preserve the selected song and pass the game mode
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

            // check that this song has its required gameplay files
            if (GamePanel.class.getResource(folder + "chart.txt") == null
                    || GamePanel.class.getResource(folder + "instrumental.wav") == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Missing chart.txt or instrumental.wav in " + folder
                );
                return;
            }

            boolean twoPlayers = previousModeScreen
                    .getSelectedMode()
                    .equals("DOS JUGADORES");

            // prepare the stage before stopping the menu music
            GameScreen game = new GameScreen(
                    folder,
                    characters.getSelectedCharacter(),
                    characters.getOtherCharacter(),
                    twoPlayers
            );

            previousCharacterScreen = characters;
            menuMusic.stop();
            pantallaActual = game;
            game.start();

            repaint();
            return;
        }

        if (pantallaActual instanceof MenuScreen menu
                && menu.getSelectedOption().equals("SALIR")) {
            // close the window using its normal closing behavior
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

        // keep drawing settings inside a separate graphics context
        Graphics2D g2 = (Graphics2D) g.create();

        try {
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