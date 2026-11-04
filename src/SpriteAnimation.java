import java.awt.image.BufferedImage;
import java.util.List;

public final class SpriteAnimation {

    public record Frame(BufferedImage image, int durationMs) {
        public Frame {
            if (image == null || durationMs < 0) {
                throw new IllegalArgumentException("Invalid animation frame");
            }
        }
    }

    private final List<Frame> frames;
    private final boolean loop;
    private final long totalDurationMs;

    public SpriteAnimation(List<Frame> frames, boolean loop) {
        this.frames = List.copyOf(frames);
        this.loop = loop;

        if (this.frames.isEmpty()) {
            throw new IllegalArgumentException("An animation needs at least one frame");
        }

        long duration = 0;

        // add the duration of every frame in the animation
        for (Frame frame : this.frames) {
            if (this.frames.size() > 1 && frame.durationMs() == 0) {
                throw new IllegalArgumentException(
                        "Frames in a moving animation need a positive duration"
                );
            }

            duration += frame.durationMs();
        }

        totalDurationMs = duration;
    }
    public long getDurationMs() {
        // return the full animation duration loaded from the sprite configuration
        return totalDurationMs;
    }
    public BufferedImage getFrame(long elapsedMs) {
        // a single frame also represents a static pose
        if (frames.size() == 1) {
            return frames.get(0).image();
        }

        long time = Math.max(0, elapsedMs);

        if (loop) {
            time %= totalDurationMs;
        } else if (time >= totalDurationMs) {
            return frames.get(frames.size() - 1).image();
        }

        // find the frame that contains the current animation time
        long frameEnd = 0;

        for (Frame frame : frames) {
            frameEnd += frame.durationMs();

            if (time < frameEnd) {
                return frame.image();
            }
        }

        return frames.get(frames.size() - 1).image();
    }
}
