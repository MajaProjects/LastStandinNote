import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import java.awt.Point;
import java.util.Map;
import java.util.Set;


public class GameScreen implements Screen, AutoCloseable {

    private final SongChart chart;
    // check player input against the song's notes
    private final NoteJudge noteJudge;
    private final MusicPlayer music;
    // keep a newly loaded song frozen until its countdown finishes
    private boolean paused = true;
    private boolean started;
    private long pausedSongTimeMs;
    private final BitmapFont font;
    private final BufferedImage background;
    // keep the monitor images for a tie and for each leader
    private final BufferedImage monitorVersus;
    private final BufferedImage monitorLinux;
    private final BufferedImage monitorWindows;

    private final SpriteAnimation playerIdle;
    private final SpriteAnimation opponentIdle;
    // keep the four singing directions for each character
    private final SpriteAnimation[] playerDirections = new SpriteAnimation[4];
    // keep the four miss poses for each character
    private final SpriteAnimation[] playerMissDirections = new SpriteAnimation[4];
    private final SpriteAnimation[] opponentMissDirections = new SpriteAnimation[4];
    private final SpriteAnimation[] opponentDirections = new SpriteAnimation[4];
    private final SpriteAnimation kernelIdle;
    // keep kernel's reactions ready for combo milestones and misses
    private final SpriteAnimation kernelHappy;
    private final SpriteAnimation kernelSad;


    private final boolean twoPlayers;
    private final String opponentLabel;

    // voice one belongs to the left side and voice two to the right
    private final int playerVoice;

    // store the four held lanes for each side of the stage
    private final boolean[][] heldKeys = new boolean[2][4];

    private final NoteRenderer noteRenderer;

    private final double shakeAmplitudePx;
    private final double shakeIntervalBeats;
    private final double shakeDurationBeats;
    private final double shakeCycles;
    // remember the latest combo alert for each side
    private final long[] comboAlertTimesMs = {-1, -1};
    private final int[] comboAlertValues = new int[2];
    // load the sound used for human combo alerts
    private final AudioPlayer comboSound = new AudioPlayer(
            "/audio/comboSound.wav"
    );
    // load the sound used when a human player misses a note
    private final AudioPlayer missSound = new AudioPlayer(
            "/audio/miss_sound_16bit.wav"
    );
    // remember the most recent direction and input time for each side
    private final int[] lastDirections = {-1, -1};
    private final long[] lastDirectionTimesMs = {-1, -1};

    public GameScreen(
            String songFolder,
            String chartPath,
            String playerCharacter,
            String opponentCharacter,
            boolean twoPlayers,
            Difficulty difficulty
    ) {
        this.twoPlayers = twoPlayers;

        // root can add visual effects while every mode keeps its configured speed
        noteRenderer = new NoteRenderer(
                48, 12, difficulty.getScrollSpeed(), difficulty == Difficulty.ROOT
        );

        if (difficulty == Difficulty.ROOT) {
            Map<String, String> settings = TextLoader.loadSettings("/songs/root_effects.txt");
            boolean enabled = TextLoader.getBooleanSetting(settings, "shake_enabled");
            double amplitude = TextLoader.getNumberSetting(settings, "shake_amplitude_px", true);
            shakeAmplitudePx = enabled ? amplitude : 0;
            shakeIntervalBeats = TextLoader.getNumberSetting(settings, "shake_interval_beats", false);
            shakeDurationBeats = TextLoader.getNumberSetting(settings, "shake_duration_beats", false);
            shakeCycles = TextLoader.getNumberSetting(settings, "shake_cycles", false);

            if (shakeDurationBeats > shakeIntervalBeats) {
                throw new IllegalArgumentException(
                        "ROOT shake duration must not exceed its interval"
                );
            }
        } else {
            shakeAmplitudePx = 0;
            shakeIntervalBeats = 1;
            shakeDurationBeats = 1;
            shakeCycles = 1;
        }

        // tux stays on the right even when another character is selected
        playerVoice = playerCharacter.equals("tux") ? 2 : 1;

        // load the chart selected for this song and difficulty
        chart = SongChart.load(chartPath);
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
        // load singing animations in the same order as the note lanes
        String[] directions = {"left", "down", "up", "right"};

        for (int lane = 0; lane < directions.length; lane++) {
            // load the normal singing pose for each character
            playerDirections[lane] = atlas.getAnimation(
                    playerCharacter,
                    directions[lane]
            );

            opponentDirections[lane] = atlas.getAnimation(
                    opponentCharacter,
                    directions[lane]
            );

            // load the matching miss pose from the atlas
            playerMissDirections[lane] = atlas.getAnimation(
                    playerCharacter,
                    directions[lane] + "_miss"
            );

            opponentMissDirections[lane] = atlas.getAnimation(
                    opponentCharacter,
                    directions[lane] + "_miss"
            );
        }
        kernelIdle = atlas.getAnimation("kernel", "idle");
        // load kernel's reactions from the shared sprite atlas
        kernelHappy = atlas.getAnimation("kernel", "happy");
        kernelSad = atlas.getAnimation("kernel", "sad");
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
        started = true;
        paused = false;
        music.play();
    }

    public void pause() {
        if (paused) {
            return;
        }

        music.pause();
        pausedSongTimeMs = music.getPositionMs();

        // finish the current instant before freezing the gameplay state
        update();
        paused = true;
    }

    public boolean resume(Set<Integer> pressedKeys) {
        if (!paused) {
            return true;
        }

        if (!getMissingResumeKeys(pressedKeys).isEmpty()) {
            return false;
        }

        // restore physical inputs without judging keys pressed in the menu
        for (int voice = 1; voice <= 2; voice++) {
            String keys = getKeysForVoice(voice);
            if (keys == null) {
                continue;
            }

            for (int lane = 0; lane < 4; lane++) {
                int code = KeyEvent.getExtendedKeyCodeForChar(keys.charAt(lane));
                heldKeys[voice - 1][lane] = pressedKeys.contains(code);
            }
        }

        // a countdown can either start a new track or resume a paused one
        if (started) {
            music.resume();
        } else {
            music.play();
            started = true;
        }
        paused = false;
        return true;
    }

    public String getMissingResumeKeys(Set<Integer> pressedKeys) {
        boolean[][] required = new boolean[2][4];

        for (int i = 0; i < chart.notes().size(); i++) {
            if (noteJudge.isSustainActive(i)) {
                Note note = chart.notes().get(i);
                required[note.voice() - 1][note.lane()] = true;
            }
        }

        StringBuilder missing = new StringBuilder();
        for (int voice = 1; voice <= 2; voice++) {
            String keys = getKeysForVoice(voice);
            if (keys == null) {
                continue;
            }

            for (int lane = 0; lane < 4; lane++) {
                char key = keys.charAt(lane);
                int code = KeyEvent.getExtendedKeyCodeForChar(key);
                if (required[voice - 1][lane] && !pressedKeys.contains(code)) {
                    if (missing.length() > 0) {
                        missing.append(' ');
                    }
                    missing.append(key);
                }
            }
        }

        return missing.toString();
    }

    private String getKeysForVoice(int voice) {
        if (twoPlayers) {
            return voice == 1 ? "ASDF" : "JKLÑ";
        }
        return voice == playerVoice ? "DFJK" : null;
    }

    private long getSongTimeMs() {
        return paused ? pausedSongTimeMs : music.getPositionMs();
    }

    public void update() {
        if (paused) {
            return;
        }

        // read the audio position once for this update
        long songTimeMs = getSongTimeMs();

        if (!twoPlayers) {
            // the bot controls the side not selected by the player
            int botVoice = playerVoice == 1 ? 2 : 1;
            boolean[] botKeys = noteJudge.updateBot(botVoice, songTimeMs);

            for (int lane = 0; lane < botKeys.length; lane++) {
                int side = botVoice - 1;

                // remember when the bot starts or releases a direction
                if (botKeys[lane] != heldKeys[side][lane]) {
                    lastDirections[side] = lane;
                    lastDirectionTimesMs[side] = songTimeMs;
                }

                heldKeys[side][lane] = botKeys[lane];
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

        int[] previousCombos = {
                noteJudge.getComboForVoice(1),
                noteJudge.getComboForVoice(2)
        };

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
            if (results[side] > 0
                    && noteJudge.getComboForVoice(voice) > previousCombos[side]
                    && noteJudge.isComboMilestone(voice)) {
                comboAlertValues[side] = noteJudge.getComboForVoice(voice);
                comboAlertTimesMs[side] = songTimeMs;
                comboSound.play();
            }
        }
    }

    public void setKeyPressed(int keyCode, boolean pressed) {
        for (int voice = 1; voice <= 2; voice++) {
            String keys = getKeysForVoice(voice);
            if (keys == null) {
                continue;
            }

            for (int lane = 0; lane < 4; lane++) {
                int code = KeyEvent.getExtendedKeyCodeForChar(keys.charAt(lane));
                if (keyCode == code) {
                    setLanePressed(voice, lane, pressed);
                    return;
                }
            }
        }
    }

    public void setLanePressed(int voice, int lane, boolean pressed) {
        // solo keyboard input only controls the selected character
        if (paused || (!twoPlayers && voice != playerVoice)) {
            return;
        }

        int side = voice - 1;
        boolean wasPressed = heldKeys[side][lane];

        // repeated press events must not judge another note
        if (pressed == wasPressed) {
            return;
        }

        long songTimeMs = getSongTimeMs();

        // evaluate any release gap before accepting the new input state
        updateHumanSustains(songTimeMs);

        heldKeys[side][lane] = pressed;
        lastDirections[side] = lane;
        lastDirectionTimesMs[side] = songTimeMs;

        if (pressed) {
            int hitIndex = noteJudge.tryHit(voice, lane, songTimeMs);

            // normal notes can advance the combo immediately
            if (hitIndex != -1
                    && !noteJudge.isSustainActive(hitIndex)
                    && noteJudge.isComboMilestone(voice)) {
                comboAlertValues[side] = noteJudge.getComboForVoice(voice);
                comboAlertTimesMs[side] = songTimeMs;
                comboSound.play();
            }
        }

        // record the start of a release or resume the held sustain
        updateHumanSustains(songTimeMs);
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
        // derive every visual effect from the same audio position
        long songTimeMs = getSongTimeMs();
        double beat = Math.max(
                0.0,
                (songTimeMs - chart.beatOffsetMs())
                        * chart.bpm() / 60_000.0
        );

        int groundY = height - 52;
        int leftX = width / 4;
        int rightX = width * 3 / 4;
        int playerX = playerVoice == 2 ? rightX : leftX;
        int opponentX = playerVoice == 2 ? leftX : rightX;
        Point offset = getStageOffset(beat);

        // fill the edges before moving the stage so no empty border appears
        g.drawImage(background, 0, 0, width, height, null);
        Graphics2D stage = (Graphics2D) g.create();

        try {
            stage.translate(offset.x, offset.y);
            if (offset.x != 0 || offset.y != 0) {
                stage.drawImage(background, 0, 0, width, height, null);
            }

            // find the voice controlled by the other participant
            int opponentVoice = playerVoice == 1 ? 2 : 1;

            // choose the current pose for each character
            SpriteAnimation playerAnimation = getCharacterAnimation(
                    playerIdle,
                    playerDirections,
                    playerMissDirections,
                    playerVoice,
                    songTimeMs
            );

            SpriteAnimation opponentAnimation = getCharacterAnimation(
                    opponentIdle,
                    opponentDirections,
                    opponentMissDirections,
                    opponentVoice,
                    songTimeMs
            );

            drawIdle(stage, playerAnimation, beat, playerX, groundY, 2);
            drawIdle(stage, opponentAnimation, beat, opponentX, groundY, 2);
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

            stage.drawImage(
                    currentMonitor,
                    monitorX,
                    monitorY,
                    monitorSize,
                    monitorSize,
                    null
            );

            // react to misses and combo progress using their own timers
            SpriteAnimation kernelAnimation = kernelIdle;
            String tuxJudgement = noteJudge.getVisibleJudgement(2, songTimeMs);
            long tuxComboAge = noteJudge.getComboAgeMs(2, songTimeMs);

            if (tuxJudgement.equals("MISS")) {
                kernelAnimation = kernelSad;
            } else if (tuxComboAge >= 0 && tuxComboAge < 700
                    && noteJudge.isComboMilestone(2)) {
                kernelAnimation = kernelHappy;
            }

            // position kernel on the monitor and draw her legs in front
            int kernelBottom = monitorY + 56;
            drawIdle(stage, kernelAnimation, beat, width / 2, kernelBottom, 1);

        } finally {
            stage.dispose();
        }

        // keep the title, controls and note field outside the camera transform
        drawLabel(g, chart.name(), width / 2, 20);

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
                noteJudge,
                beat
        );

        // animate the latest judgement on each side
        drawJudgement(g, 1, width / 4, 60, songTimeMs);
        drawJudgement(g, 2, width * 3 / 4, 60, songTimeMs);

        // show temporary combo alerts for human players
        drawComboAlert(g, 1, width / 4, 105, songTimeMs);
        drawComboAlert(g, 2, width * 3 / 4, 105, songTimeMs);
    }

    private Point getStageOffset(double beat) {
        if (shakeAmplitudePx == 0) {
            return new Point(0, 0);
        }

        double position = beat % shakeIntervalBeats;
        if (position >= shakeDurationBeats) {
            return new Point(0, 0);
        }

        // a short deterministic burst fades out after each marked beat
        double progress = position / shakeDurationBeats;
        double envelope = (1.0 - progress) * (1.0 - progress);
        double phase = progress * Math.PI * 2.0 * shakeCycles;
        int x = (int) Math.round(Math.sin(phase) * shakeAmplitudePx * envelope);
        int y = (int) Math.round(
                Math.sin(phase * 1.37) * shakeAmplitudePx * envelope * 0.6
        );

        return new Point(x, y);
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
    private SpriteAnimation getCharacterAnimation(
            SpriteAnimation idle,
            SpriteAnimation[] directions,
            SpriteAnimation[] missDirections,
            int voice,
            long songTimeMs
    ) {
        int side = voice - 1;

        String judgement = noteJudge.getVisibleJudgement(voice, songTimeMs);
        long judgementAge = noteJudge.getJudgementAgeMs(voice, songTimeMs);

        // briefly show the direction of the latest missed note
        if (judgement.equals("MISS")
                && judgementAge >= 0
                && judgementAge < 250) {
            int missLane = noteJudge.getLastMissLane(voice);

            if (missLane >= 0) {
                return missDirections[missLane];
            }
        }

        // use the first held lane as the singing direction
        for (int lane = 0; lane < directions.length; lane++) {
            if (heldKeys[side][lane]) {
                return directions[lane];
            }
        }
        // keep the most recent singing pose briefly after release
        int lastLane = lastDirections[side];
        long directionAge = songTimeMs - lastDirectionTimesMs[side];

        if (lastLane >= 0 && directionAge >= 0 && directionAge < 150) {
            return directions[lastLane];
        }

        return idle;
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