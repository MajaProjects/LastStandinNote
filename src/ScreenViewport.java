import java.awt.Point;

record ScreenViewport(double scale, double left, double top) {

    static final int WIDTH = 960;
    static final int HEIGHT = 540;

    static ScreenViewport fit(int width, int height) {
        double scale = Math.min(Math.max(1, width) / (double) WIDTH,
                Math.max(1, height) / (double) HEIGHT);
        return new ScreenViewport(scale, (width - WIDTH * scale) / 2.0,
                (height - HEIGHT * scale) / 2.0);
    }

    Point toLogical(int x, int y) {
        return new Point((int) Math.floor((x - left) / scale),
                (int) Math.floor((y - top) / scale));
    }

    boolean contains(int x, int y) {
        return x >= left && y >= top && x < left + WIDTH * scale && y < top + HEIGHT * scale;
    }
}
