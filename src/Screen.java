import java.awt.Graphics2D;

public interface Screen {

    // draw the screen inside the available area
    void render(Graphics2D g, int width, int height);
}