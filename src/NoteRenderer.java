import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

public final class NoteRenderer {

    private final BufferedImage[] arrows = new BufferedImage[4];
    private final BufferedImage[] bodies = new BufferedImage[4];
    private final BufferedImage[] tails = new BufferedImage[4];
    private final BufferedImage[] pressedArrows = new BufferedImage[4];

    // use the dark sprites when a note is missed
    private final BufferedImage[] missedArrows = new BufferedImage[4];
    private final BufferedImage[] missedBodies = new BufferedImage[4];
    private final BufferedImage[] missedTails = new BufferedImage[4];

    private final BufferedImage receptor;

    private final int arrowSize;
    private final int gap;
    private final double pixelsPerMs;

    // remember visual feedback without changing the judge's results
    private NoteJudge visualJudge;
    private long[] missTimesMs = new long[0];

    private static final long MISS_FLASH_MS = 220;

    // rotate the original right-facing arrow for each lane
    private final double[] rotations = {
            Math.PI,
            Math.PI / 2,
            -Math.PI / 2,
            0
    };

    public NoteRenderer(
            int arrowSize,
            int gap,
            double pixelsPerSecond
    ) {
        if (arrowSize <= 0 || gap < 0
                || !Double.isFinite(pixelsPerSecond)
                || pixelsPerSecond <= 0) {
            throw new IllegalArgumentException(
                    "Invalid note rendering settings"
            );
        }

        this.arrowSize = arrowSize;
        this.gap = gap;
        this.pixelsPerMs = pixelsPerSecond / 1000.0;

        SpriteAtlas atlas = new SpriteAtlas(
                "/images/arrows/arrows.txt"
        );

        // load the normal, pressed and missed sprites for each lane
        for (int lane = 0; lane < 4; lane++) {
            arrows[lane] = atlas.getAnimation(
                    "arrow", "normal_" + lane
            ).getFrame(0);

            bodies[lane] = atlas.getAnimation(
                    "arrow", "body_" + lane
            ).getFrame(0);

            tails[lane] = atlas.getAnimation(
                    "arrow", "tail_" + lane
            ).getFrame(0);

            pressedArrows[lane] = atlas.getAnimation(
                    "arrow", "pressed_" + lane
            ).getFrame(0);

            missedArrows[lane] = atlas.getAnimation(
                    "arrow", "miss_" + lane
            ).getFrame(0);

            missedBodies[lane] = atlas.getAnimation(
                    "arrow", "body_miss_" + lane
            ).getFrame(0);

            missedTails[lane] = atlas.getAnimation(
                    "arrow", "tail_miss_" + lane
            ).getFrame(0);
        }

        receptor = atlas.getAnimation(
                "arrow", "receptor"
        ).getFrame(0);
    }

    public void render(
            Graphics2D g,
            List<Note> notes,
            long songTimeMs,
            int width,
            int height,
            boolean[][] heldKeys,
            NoteJudge noteJudge
    ) {
        int receptorY = height - 100;
        Graphics2D field = (Graphics2D) g.create();

        try {
            // protect the labels below the note field
            field.clipRect(0, 0, width, height - 64);

            updateMissVisuals(notes.size(), songTimeMs, noteJudge);

            // draw all sustained bodies behind the receptors and heads
            drawNotes(
                    field, notes, songTimeMs, width, height,
                    receptorY, noteJudge, true
            );

            for (int voice = 1; voice <= 2; voice++) {
                for (int lane = 0; lane < 4; lane++) {
                    BufferedImage image = receptor;

                    if (heldKeys[voice - 1][lane]) {
                        image = pressedArrows[lane];
                    }

                    drawArrow(
                            field,
                            image,
                            lane,
                            getLaneX(voice, lane, width),
                            receptorY
                    );
                }
            }

            // keep note heads visible above sustained bodies
            drawNotes(
                    field, notes, songTimeMs, width, height,
                    receptorY, noteJudge, false
            );
        } finally {
            field.dispose();
        }
    }

    private void updateMissVisuals(
            int count,
            long songTimeMs,
            NoteJudge judge
    ) {
        // reset visual state when a different chart or judge is used
        if (visualJudge != judge || missTimesMs.length != count) {
            visualJudge = judge;
            missTimesMs = new long[count];
            Arrays.fill(missTimesMs, -1);
        }

        for (int i = 0; i < count; i++) {
            if (judge.isMissed(i) && missTimesMs[i] < 0) {
                missTimesMs[i] = songTimeMs;
            }
        }
    }

    private void drawNotes(
            Graphics2D g,
            List<Note> notes,
            long songTimeMs,
            int width,
            int height,
            int receptorY,
            NoteJudge judge,
            boolean drawBodies
    ) {
        for (int i = 0; i < notes.size(); i++) {
            // briefly show bad hits even though they are already judged
            boolean badFlash = judge.isBadHit(i)
                    && judge.getBadHitAgeMs(i, songTimeMs) < MISS_FLASH_MS;

            if (judge.isHit(i) && !badFlash) {
                continue;
            }

            Note note = notes.get(i);

            boolean missed = judge.isMissed(i);
            boolean active = judge.isSustainActive(i);

            boolean missFlash = missed
                    && songTimeMs - missTimesMs[i] < MISS_FLASH_MS;

            // show normal misses briefly before hiding them
            if (missed && note.durationMs() == 0 && !missFlash) {
                continue;
            }

            // use the music clock for both head and tail positions
            double headY = receptorY
                    + (songTimeMs - note.timeMs()) * pixelsPerMs;

            double tailY = headY
                    - note.durationMs() * pixelsPerMs;

            // keep held heads and brief miss feedback at the receptor
            if (active || missFlash || badFlash) {
                headY = Math.min(headY, receptorY);
            }

            if (headY < -arrowSize) {
                continue;
            }

            if (!missFlash && !badFlash
                    && tailY > height + arrowSize) {
                continue;
            }

            int lane = note.lane();
            int x = getLaneX(note.voice(), lane, width);

            if (drawBodies) {
                // a completed bad sustain only needs head feedback
                if (note.durationMs() > 0 && !badFlash) {
                    BufferedImage body = missed
                            ? missedBodies[lane]
                            : bodies[lane];

                    BufferedImage tail = missed
                            ? missedTails[lane]
                            : tails[lane];

                    drawSustain(
                            g, body, tail, x,
                            headY, tailY, height
                    );
                }

                continue;
            }

            BufferedImage image = arrows[lane];

            if (missed || badFlash) {
                image = missedArrows[lane];
                image = missedArrows[lane];
            } else if (active) {
                // confirm visually that the sustained note was registered
                image = pressedArrows[lane];
            }

            if (headY <= height + arrowSize) {
                drawArrow(
                        g,
                        image,
                        lane,
                        x,
                        (int) Math.round(headY)
                );
            }
        }
    }

    private int getLaneX(int voice, int lane, int width) {
        int centerX = voice == 2
                ? width * 3 / 4
                : width / 4;

        int groupWidth = 4 * arrowSize + 3 * gap;
        int left = centerX - groupWidth / 2;

        return left
                + lane * (arrowSize + gap)
                + arrowSize / 2;
    }

    private void drawSustain(
            Graphics2D g,
            BufferedImage body,
            BufferedImage tail,
            int centerX,
            double headY,
            double tailY,
            int screenHeight
    ) {
        // limit the stretched body to the visible area
        int top = (int) Math.round(Math.max(0, tailY));
        int bottom = (int) Math.round(
                Math.min(screenHeight, headY)
        );

        if (bottom > top) {
            g.drawImage(
                    body,
                    centerX - arrowSize / 2,
                    top,
                    arrowSize,
                    bottom - top,
                    null
            );
        }

        // keep the tail vertical above the body
        if (tailY >= -arrowSize
                && tailY <= screenHeight + arrowSize) {
            g.drawImage(
                    tail,
                    centerX - arrowSize / 2,
                    (int) Math.round(tailY) - arrowSize / 2,
                    arrowSize,
                    arrowSize,
                    null
            );
        }
    }

    private void drawArrow(
            Graphics2D g,
            BufferedImage image,
            int lane,
            int centerX,
            int centerY
    ) {
        Graphics2D arrow = (Graphics2D) g.create();

        try {
            // rotate around the center to preserve lane alignment
            arrow.translate(centerX, centerY);
            arrow.rotate(rotations[lane]);

            arrow.drawImage(
                    image,
                    -arrowSize / 2,
                    -arrowSize / 2,
                    arrowSize,
                    arrowSize,
                    null
            );
        } finally {
            arrow.dispose();
        }
    }
}