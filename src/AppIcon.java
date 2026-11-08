import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public final class AppIcon {
    private AppIcon() { }

    public static List<Image> loadImages() {
        BufferedImage frame = new SpriteAtlas("/images/sprites/sprites.txt")
                .getAnimation("monitor", "versus").getFrame(0);
        List<Image> images = new ArrayList<>();
        for (int size : new int[]{16, 32, 48, 64, 128}) {
            BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = icon.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(frame, 0, 0, size, size, null);
            } finally {
                g.dispose();
            }
            images.add(icon);
        }
        return List.copyOf(images);
    }
}
