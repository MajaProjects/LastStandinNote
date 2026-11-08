import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

public final class AppIcon {
    // small sizes are drawn separately so the taskbar never shows a blurred downscale
    private static final int[] SIZES = {16, 24, 32, 48, 64, 128, 256};

    private AppIcon() { }

    public static List<Image> loadImages() {
        List<Image> images = new ArrayList<>();
        for (int size : SIZES) {
            try {
                images.add(ImageLoader.load("/images/icon/icon_" + size + ".png"));
            } catch (RuntimeException error) {
                // a missing icon must never stop the game from opening
                GameLog.record("Could not load window icon " + size, error);
            }
        }
        return List.copyOf(images);
    }
}
