import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

public final class NoteRenderer {

    private final BufferedImage[] arrows = new BufferedImage[4];
    private final BufferedImage[] bodies = new BufferedImage[4];
    private final BufferedImage[] tails = new BufferedImage[4];
    private final BufferedImage receptor;

    private final int arrowSize;
    private final int gap;
    private final double pixelsPerMs;

    // rotate the original right-facing arrow for each lane
    private final double[] rotations = {
            Math.PI,
            Math.PI / 2,
            -Math.PI / 2,
            0
    };

    public NoteRenderer(int arrowSize, int gap, double pixelsPerSecond) {
        if (arrowSize <= 0 || gap < 0
                || !Double.isFinite(pixelsPerSecond)
                || pixelsPerSecond <= 0) {
            throw new IllegalArgumentException("Invalid note rendering settings");
        }

        this.arrowSize = arrowSize;
        this.gap = gap;
        this.pixelsPerMs = pixelsPerSecond / 1000.0;

        SpriteAtlas atlas = new SpriteAtlas("/images/arrows/arrows.txt");

        // load the head, body and tail for each lane
        for (int lane = 0; lane < arrows.length; lane++) {
            arrows[lane] = atlas.getAnimation(
                    "arrow", "normal_" + lane
            ).getFrame(0);

            bodies[lane] = atlas.getAnimation(
                    "arrow", "body_" + lane
            ).getFrame(0);

            tails[lane] = atlas.getAnimation(
                    "arrow", "tail_" + lane
            ).getFrame(0);
        }

        receptor = atlas.getAnimation("arrow", "receptor").getFrame(0);
    }

    public void render(
            Graphics2D g,
            List<Note> notes,
            long songTimeMs,
            int width,
            int height
    ) {
        int receptorY = height - 100;
        Graphics2D field = (Graphics2D) g.create();

        try {
            // allow notes to enter from the top and protect the bottom labels
            field.clipRect(0, 0, width, height - 64);

            for (int voice = 1; voice <= 2; voice++) {
                for (int lane = 0; lane < 4; lane++) {
                    int x = getLaneX(voice, lane, width);
                    drawArrow(field, receptor, lane, x, receptorY);
                }
            }

            for (Note note : notes) {
                // the head reaches the receptor when the note starts
                double headY = receptorY
                        + (songTimeMs - note.timeMs()) * pixelsPerMs;

                // the tail reaches the receptor when the note ends
                double tailY = headY - note.durationMs() * pixelsPerMs;

                if (headY < -arrowSize) {
                    break;
                }

                // keep sustained notes visible until their tails leave
                if (tailY > height + arrowSize) {
                    continue;
                }

                int x = getLaneX(note.voice(), note.lane(), width);

                if (note.durationMs() > 0) {
                    drawSustain(
                            field, note.lane(), x, headY, tailY, height
                    );
                }

                // draw the head over its sustained body
                if (headY <= height + arrowSize) {
                    drawArrow(
                            field,
                            arrows[note.lane()],
                            note.lane(),
                            x,
                            (int) Math.round(headY)
                    );
                }
            }
        } finally {
            // release the graphics copy even if drawing fails
            field.dispose();
        }
    }

    private int getLaneX(int voice, int lane, int width) {
        // center each voice's four lanes on its side of the stage
        int centerX = width / 4;

        if (voice == 2) {
            centerX = width * 3 / 4;
        }

        int groupWidth = 4 * arrowSize + 3 * gap;
        int left = centerX - groupWidth / 2;

        return left + lane * (arrowSize + gap) + arrowSize / 2;
    }

    private void drawSustain(
            Graphics2D g,
            int lane,
            int centerX,
            double headY,
            double tailY,
            int screenHeight
    ) {
        // limit the body to the screen before converting to integers
        int top = (int) Math.round(Math.max(0, tailY));
        int bottom = (int) Math.round(Math.min(screenHeight, headY));

        if (bottom > top) {
            g.drawImage(
                    bodies[lane],
                    centerX - arrowSize / 2,
                    top,
                    arrowSize,
                    bottom - top,
                    null
            );
        }

        if (tailY >= -arrowSize && tailY <= screenHeight + arrowSize) {
            // keep the rounded tail vertical above the body
            g.drawImage(
                    tails[lane],
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