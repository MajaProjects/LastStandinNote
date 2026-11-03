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
                    "No se encontró la imagen: " + path
            );
        }

        try {
            BufferedImage image = ImageIO.read(resource);

            if (image == null) {
                throw new IllegalStateException(
                        "Formato de imagen no compatible: " + path
                );
            }

            return image;
        } catch (IOException error) {
            throw new IllegalStateException(
                    "No se pudo leer la imagen: " + path,
                    error
            );
        }
    }
}