import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GamePanel extends JPanel {

    private Screen pantallaActual;
    private SongSelectScreen previousSongScreen;
    private ModeSelectScreen previousModeScreen;

    // load the navigation sound once and reuse it
    private final AudioPlayer menuMove = new AudioPlayer(
            "/audio/menu_move.wav"
    );

    public GamePanel() {
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(960, 540));

        // open the title screen when the game starts
        pantallaActual = new TitleScreen();

        // connect enter to the selection action
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
                // reuse previous screens to preserve their selections
                if (pantallaActual instanceof CharacterSelectScreen) {
                    pantallaActual = previousSongScreen;
                } else if (pantallaActual instanceof SongSelectScreen) {
                    pantallaActual = previousModeScreen;
                } else if (pantallaActual instanceof ModeSelectScreen) {
                    pantallaActual = new MenuScreen();
                } else if (pantallaActual instanceof MenuScreen) {
                    pantallaActual = new TitleScreen();
                } else {
                    return;
                }

                repaint();
            }
        });

        // navigate menus and songs with the vertical arrows
        bindMenuNavigation("menuUp", KeyEvent.VK_UP, -1);
        bindMenuNavigation("menuDown", KeyEvent.VK_DOWN, 1);

        // choose a character with the horizontal arrows
        bindCharacterNavigation("characterLeft", KeyEvent.VK_LEFT, -1);
        bindCharacterNavigation("characterRight", KeyEvent.VK_RIGHT, 1);

        // request regular redraws for the interface animations
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

                // play the sound after a navigation action
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
            repaint();
            return;
        }

        if (pantallaActual instanceof MenuScreen menu
                && menu.getSelectedOption().equals("JUGAR")) {
            pantallaActual = new ModeSelectScreen();
            repaint();
            return;
        }

        if (pantallaActual instanceof ModeSelectScreen modes) {
            // keep the mode selection before opening the song list
            previousModeScreen = modes;
            pantallaActual = new SongSelectScreen();
            repaint();
            return;
        }

        if (pantallaActual instanceof SongSelectScreen songs) {
            // keep the song selection and pass the chosen mode
            previousSongScreen = songs;

            boolean twoPlayers = previousModeScreen
                    .getSelectedMode()
                    .equals("DOS JUGADORES");

            pantallaActual = new CharacterSelectScreen(
                    songs.getSelectedSong(),
                    twoPlayers
            );

            repaint();
            return;
        }

        if (pantallaActual instanceof MenuScreen menu
                && menu.getSelectedOption().equals("SALIR")) {
            // close the window using its configured closing behavior
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