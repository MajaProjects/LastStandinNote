import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public final class ImageLoader {

    // this utility class does not need instances
    private ImageLoader() {
    }

    public static BufferedImage load(String path) {
        // find the image among the bundled resources
        URL resource = ImageLoader.class.getResource(path);

        if (resource == null) {
            throw new IllegalStateException(
                    "Image not found: " + path
            );
        }

        try {
            BufferedImage image = ImageIO.read(resource);

            if (image == null) {
                throw new IllegalStateException(
                        "Unsupported image format: " + path
                );
            }

            return image;
        } catch (IOException error) {
            throw new IllegalStateException(
                    "Could not read image: " + path,
                    error
            );
        }
    }
}
