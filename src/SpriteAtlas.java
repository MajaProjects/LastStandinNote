import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SpriteAtlas {

    private final BufferedImage image;
    private final List<String> lines = new ArrayList<>();
    private final Map<String, SpriteAnimation> animations = new HashMap<>();

    public SpriteAtlas(String configPath) {
        // keep only the configuration lines
        for (String line : TextLoader.load(configPath)) {
            String trimmed = line.strip();

            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                lines.add(trimmed);
            }
        }

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("The sprite configuration is empty");
        }

        String[] header = lines.get(0).split(";", -1);

        if (header.length != 2 || !header[0].equals("image")) {
            throw new IllegalArgumentException(
                    "The sprite configuration must start with an image"
            );
        }

        // find the image relative to its configuration file
        String folder = configPath.substring(0, configPath.lastIndexOf('/') + 1);
        image = ImageLoader.load(folder + header[1]);
    }

    public SpriteAnimation getAnimation(String actor, String name) {
        String key = actor + "/" + name;
        SpriteAnimation cached = animations.get(key);

        if (cached != null) {
            return cached;
        }

        List<SpriteAnimation.Frame> frames = new ArrayList<>();
        boolean selected = false;
        boolean found = false;
        boolean loop = false;

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] fields = line.split(";", -1);

            try {
                switch (fields[0]) {
                    case "animation" -> {
                        requireFields(fields, 4);

                        selected = fields[1].equals(actor)
                                && fields[2].equals(name);

                        if (selected) {
                            if (found) {
                                throw new IllegalArgumentException(
                                        "Duplicate animation"
                                );
                            }

                            if (!fields[3].equals("true")
                                    && !fields[3].equals("false")) {
                                throw new IllegalArgumentException(
                                        "Loop must be true or false"
                                );
                            }

                            found = true;
                            loop = Boolean.parseBoolean(fields[3]);
                        }
                    }

                    case "frame" -> {
                        if (selected) {
                            requireFields(fields, 9);

                            if (!fields[1].equals(actor)
                                    || !fields[2].equals(name)) {
                                throw new IllegalArgumentException(
                                        "Frame belongs to a different animation"
                                );
                            }

                            int index = Integer.parseInt(fields[3]);

                            if (index != frames.size()) {
                                throw new IllegalArgumentException(
                                        "Frame indices must start at zero and follow order"
                                );
                            }

                            frames.add(readFrame(fields));
                        }
                    }

                    default -> throw new IllegalArgumentException(
                            "Unknown sprite setting"
                    );
                }
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException(
                        "Invalid sprite setting: " + line,
                        error
                );
            }
        }

        if (!found || frames.isEmpty()) {
            throw new IllegalArgumentException("Animation not found or empty: " + key);
        }

        SpriteAnimation animation = new SpriteAnimation(frames, loop);
        animations.put(key, animation);
        return animation;
    }

    private SpriteAnimation.Frame readFrame(String[] fields) {
        int x = Integer.parseInt(fields[4]);
        int y = Integer.parseInt(fields[5]);
        int width = Integer.parseInt(fields[6]);
        int height = Integer.parseInt(fields[7]);
        int durationMs = Integer.parseInt(fields[8]);

        if (x < 0 || y < 0 || width <= 0 || height <= 0
                || width > image.getWidth() || height > image.getHeight()
                || x > image.getWidth() - width
                || y > image.getHeight() - height) {
            throw new IllegalArgumentException("Frame is outside the sprite image");
        }

        // extract the original pixels for this frame
        BufferedImage frameImage = image.getSubimage(x, y, width, height);
        return new SpriteAnimation.Frame(frameImage, durationMs);
    }

    private static void requireFields(String[] fields, int expected) {
        if (fields.length != expected) {
            throw new IllegalArgumentException("Wrong number of sprite fields");
        }
    }
}