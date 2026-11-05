import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;

public class GameScreen implements Screen, AutoCloseable {

    private final SongChart chart;
    // check player input against the song's notes
    private final NoteJudge noteJudge;
    private final MusicPlayer music;
    private final BitmapFont font;
    private final BufferedImage background;
    // keep the monitor images for a tie and for each leader
    private final BufferedImage monitorVersus;
    private final BufferedImage monitorLinux;
    private final BufferedImage monitorWindows;

    private final SpriteAnimation playerIdle;
    private final SpriteAnimation opponentIdle;
    private final SpriteAnimation kernelIdle;

    private final boolean twoPlayers;
    private final String opponentLabel;

    // voice one belongs to the left side and voice two to the right
    private final int playerVoice;

    // store the four held lanes for each side of the stage
    private final boolean[][] heldKeys = new boolean[2][4];

    private final NoteRenderer noteRenderer =
            new NoteRenderer(48, 12, 300);
    // remember the latest combo alert for each side
    private final long[] comboAlertTimesMs = {-1, -1};
    private final int[] comboAlertValues = new int[2];
    // load the sound used for human combo alerts
    private final AudioPlayer comboSound = new AudioPlayer(
            "/audio/comboSound.wav"
    );
    // load the sound used when a human player misses a note
    private final AudioPlayer missSound = new AudioPlayer(
            "/audio/miss sound.wav"
    );

    public GameScreen(
            String songFolder,
            String playerCharacter,
            String opponentCharacter,
            boolean twoPlayers
    ) {
        this.twoPlayers = twoPlayers;

        // tux stays on the right even when another character is selected
        playerVoice = playerCharacter.equals("tux") ? 2 : 1;

        // load the selected song's settings and notes
        chart = SongChart.load(songFolder + "chart.txt");
        // allow a 160 ms difference on either side of the note
        noteJudge = new NoteJudge(chart.notes(), 160);

        background = ImageLoader.load(
                "/images/backgrounds/fondo1_480x270.png"
        );

        font = new BitmapFont("/images/font/font.txt");

        SpriteAtlas atlas = new SpriteAtlas(
                "/images/sprites/sprites.txt"
        );

        // keep the selected character assigned to player one
        playerIdle = atlas.getAnimation(playerCharacter, "idle");
        opponentIdle = atlas.getAnimation(opponentCharacter, "idle");
        kernelIdle = atlas.getAnimation("kernel", "idle");
        // load the three monitor images from the sprite atlas
        monitorVersus = atlas.getAnimation("monitor", "versus").getFrame(0);
        monitorLinux = atlas.getAnimation("monitor", "linux").getFrame(0);
        monitorWindows = atlas.getAnimation("monitor", "windows").getFrame(0);

        opponentLabel = twoPlayers ? "PLAYER TWO" : "BOT";

        music = new MusicPlayer(songFolder + "instrumental.wav");

        System.out.println(
                "Loaded " + chart.name()
                        + " with " + chart.notes().size() + " notes"
        );
    }

    public void start() {
        // start the song after the screen has been installed
        music.play();
    }
    public void update() {
        // read the audio position once for this update
        long songTimeMs = music.getPositionMs();

        if (!twoPlayers) {
            // the bot controls the side not selected by the player
            int botVoice = playerVoice == 1 ? 2 : 1;
            boolean[] botKeys = noteJudge.updateBot(botVoice, songTimeMs);

            // pass the bot's active lanes to the receptor renderer
            for (int lane = 0; lane < botKeys.length; lane++) {
                heldKeys[botVoice - 1][lane] = botKeys[lane];
            }
        }

        // update sustained notes controlled by human players
        updateHumanSustains(songTimeMs);

        // judge normal note misses on human-controlled sides
        boolean judgeVoiceOne = twoPlayers || playerVoice == 1;
        boolean judgeVoiceTwo = twoPlayers || playerVoice == 2;

        int newMisses = noteJudge.updateMisses(
                songTimeMs,
                judgeVoiceOne,
                judgeVoiceTwo
        );

        if (newMisses > 0) {
            missSound.play();
        }
    }
    private void updateHumanSustains(long songTimeMs) {
        boolean judgeVoiceOne = twoPlayers || playerVoice == 1;
        boolean judgeVoiceTwo = twoPlayers || playerVoice == 2;

        // check completed and missed sustained notes
        int[] results = noteJudge.updateSustains(
                songTimeMs,
                heldKeys,
                judgeVoiceOne,
                judgeVoiceTwo
        );

        // play the miss sound if a human missed a sustained note
        if (results[2] > 0 || results[3] > 0) {
            missSound.play();
        }

        for (int side = 0; side < 2; side++) {
            int voice = side + 1;

            // prepare a combo alert after completing a sustained note
            if (results[side] > 0 && noteJudge.isComboMilestone(voice)) {
                comboAlertValues[side] = noteJudge.getComboForVoice(voice);
                comboAlertTimesMs[side] = songTimeMs;
                comboSound.play();
            }
        }
    }

    public void setKeyPressed(int keyCode, boolean pressed) {
        // shared play uses the left layout first; solo play uses dfjk
        String primaryKeys = twoPlayers ? "ASDF" : "DFJK";
        String rightKeys = "JKLÑ";

        for (int lane = 0; lane < 4; lane++) {
            int primaryCode = KeyEvent.getExtendedKeyCodeForChar(
                    primaryKeys.charAt(lane)
            );

            if (keyCode == primaryCode) {
                // solo input follows the selected character's side
                int voice = twoPlayers ? 1 : playerVoice;

                setLanePressed(voice, lane, pressed);
                return;
            }

            // jklñ always controls tux when both players share a keyboard
            if (twoPlayers) {
                int rightCode = KeyEvent.getExtendedKeyCodeForChar(
                        rightKeys.charAt(lane)
                );

                if (keyCode == rightCode) {
                    setLanePressed(2, lane, pressed);
                    return;
                }
            }
        }
    }

    public void setLanePressed(int voice, int lane, boolean pressed) {
        // solo keyboard input only controls the selected character
        if (!twoPlayers && voice != playerVoice) {
            return;
        }

        int side = voice - 1;
        boolean wasPressed = heldKeys[side][lane];
        long songTimeMs = music.getPositionMs();

        if (!pressed && wasPressed) {
            // complete notes that ended before the key was released
            updateHumanSustains(songTimeMs);
        }

        if (pressed && !wasPressed) {
            // judge a note when the key changes from released to pressed
            int hitIndex = noteJudge.tryHit(voice, lane, songTimeMs);

            // announce milestones after a normal note is completed
            if (hitIndex != -1
                    && !noteJudge.isSustainActive(hitIndex)
                    && noteJudge.isComboMilestone(voice)) {
                comboAlertValues[side] = noteJudge.getComboForVoice(voice);
                comboAlertTimesMs[side] = songTimeMs;
                comboSound.play();
            }
        }

        // remember whether the key is currently held
        heldKeys[side][lane] = pressed;

        if (!pressed && wasPressed) {
            // detect an early release immediately
            updateHumanSustains(songTimeMs);
        }
    }
    public void releaseKeys() {
        // clear all held lanes when the game loses window focus
        for (boolean[] sideKeys : heldKeys) {
            for (int lane = 0; lane < sideKeys.length; lane++) {
                sideKeys[lane] = false;
            }
        }
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        g.drawImage(background, 0, 0, width, height, null);

        // derive musical time from the audio position
        long songTimeMs = music.getPositionMs();

        double beat = Math.max(
                0.0,
                (songTimeMs - chart.beatOffsetMs())
                        * chart.bpm() / 60_000.0
        );

        drawLabel(g, chart.name(), width / 2, 20);

        int groundY = height - 52;
        int leftX = width / 4;
        int rightX = width * 3 / 4;

        // selection changes the controller, while character positions stay fixed
        int playerX = playerVoice == 2 ? rightX : leftX;
        int opponentX = playerVoice == 2 ? leftX : rightX;

        drawIdle(g, playerIdle, beat, playerX, groundY, 2);
        drawIdle(g, opponentIdle, beat, opponentX, groundY, 2);

        // enlarge the monitor while preserving its center
        int monitorSize = 192;
        int monitorCenterY = height - 232;
        int monitorX = width / 2 - monitorSize / 2;
        int monitorY = monitorCenterY - monitorSize / 2;

// compare the scores of the two sides
        int windowsScore = noteJudge.getScoreForVoice(1);
        int tuxScore = noteJudge.getScoreForVoice(2);

// show versus when the scores are equal
        BufferedImage currentMonitor = monitorVersus;

// show the character that currently has the higher score
        if (windowsScore > tuxScore) {
            currentMonitor = monitorWindows;
        } else if (tuxScore > windowsScore) {
            currentMonitor = monitorLinux;
        }

        g.drawImage(
                currentMonitor,
                monitorX,
                monitorY,
                monitorSize,
                monitorSize,
                null
        );

        // position kernel on the monitor and draw her legs in front
        int kernelBottom = monitorY + 56;
        drawIdle(g, kernelIdle, beat, width / 2, kernelBottom, 1);

        // place each controller label below the character it belongs to
        drawLabel(g, "PLAYER ONE", playerX, height - 42);
        drawLabel(g, opponentLabel, opponentX, height - 42);

        // share the audio clock and held inputs with the note renderer
        noteRenderer.render(
                g,
                chart.notes(),
                songTimeMs,
                width,
                height,
                heldKeys,
                noteJudge
        );

        // animate the latest judgement on each side
        drawJudgement(g, 1, width / 4, 60, songTimeMs);
        drawJudgement(g, 2, width * 3 / 4, 60, songTimeMs);

        // show temporary combo alerts for human players
        drawComboAlert(g, 1, width / 4, 105, songTimeMs);
        drawComboAlert(g, 2, width * 3 / 4, 105, songTimeMs);
    }
    private void drawJudgement(
            Graphics2D g,
            int voice,
            int centerX,
            int y,
            long songTimeMs
    ) {
        String judgement = noteJudge.getVisibleJudgement(voice, songTimeMs);

        // skip drawing when there is no visible judgement
        if (judgement.isEmpty()) {
            return;
        }

        long ageMs = noteJudge.getJudgementAgeMs(voice, songTimeMs);

        // shrink from a larger size during the first 180 ms
        double progress = Math.min(ageMs / 180.0, 1.0);
        double scale = 1.0 + 0.4 * (1.0 - progress);

        Graphics2D copy = (Graphics2D) g.create();

        try {
            // scale around the horizontal center of the text
            copy.translate(centerX, y);
            copy.scale(scale, scale);

            int textWidth = font.getTextWidth(judgement, 1);
            font.draw(copy, judgement, -textWidth / 2, 0, 1);
        } finally {
            copy.dispose();
        }
    }
    private void drawComboAlert(
            Graphics2D g,
            int voice,
            int centerX,
            int y,
            long songTimeMs
    ) {
        int side = voice - 1;
        long alertTime = comboAlertTimesMs[side];

        // show the alert for one second after the milestone
        if (alertTime < 0 || songTimeMs - alertTime >= 1000) {
            return;
        }

        String text = "COMBO " + comboAlertValues[side];
        drawLabel(g, text, centerX, y);
    }

    private void drawIdle(
            Graphics2D g,
            SpriteAnimation animation,
            double beat,
            int centerX,
            int bottom,
            int scale
    ) {
        // fit a complete idle animation into each musical beat
        long animationTimeMs =
                (long) (beat * animation.getDurationMs());

        BufferedImage frame = animation.getFrame(animationTimeMs);

        int width = frame.getWidth() * scale;
        int height = frame.getHeight() * scale;

        g.drawImage(
                frame,
                centerX - width / 2,
                bottom - height,
                width,
                height,
                null
        );
    }

    private void drawLabel(
            Graphics2D g,
            String text,
            int centerX,
            int y
    ) {
        int x = centerX - font.getTextWidth(text, 1) / 2;
        font.draw(g, text, x, y, 1);
    }

    @Override
    public void close() {
        // release the song and sound resources
        music.close();
        comboSound.close();
        missSound.close();
    }
}