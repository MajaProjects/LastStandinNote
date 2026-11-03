import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
public class GamePanel extends JPanel {
    private final BufferedImage fondoInicio;

    public GamePanel() {
        setBackground(Color.BLACK);
        URL ruta = GamePanel.class.getResource(
                "/images/backgrounds/circuito_480x270.png"
        );

        if (ruta == null) {
            throw new IllegalStateException(
                    "No se encontró el fondo de inicio"
            );
        }

        try {
            fondoInicio = ImageIO.read(ruta);
        } catch (IOException error) {
            throw new IllegalStateException(
                    "No se pudo leer el fondo de inicio", error
            );
        }

        if (fondoInicio == null) {
            throw new IllegalStateException(
                    "El archivo del fondo no contiene una imagen compatible"
            );
        }
        setPreferredSize(new Dimension(960, 540));
    }
    @Override
    protected void paintComponent(Graphics g) {
        // paint the panel background before drawing the image
        super.paintComponent(g);
        // copy the drawing context so our settings do not affect the original

        Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setRenderingHint(
                    // keep pixel edges sharp when scaling the image
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
            );
            // draw the background from the top-left corner
            // scale it to fill the current panel size
            g2.drawImage(
                    fondoInicio,
                    0, 0,
                    getWidth(), getHeight(),
                    this
            );
        } finally {
            // release the copied drawing context
            g2.dispose();
        }
    }
}