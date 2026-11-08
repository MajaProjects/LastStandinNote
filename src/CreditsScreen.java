import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public final class CreditsScreen implements Screen {

    private static final int ENTRIES_PER_PAGE = 4;
    private final MenuScreen previousMenu;
    private final BitmapFont font = new BitmapFont("/images/font/font.txt");
    private final BufferedImage background = ImageLoader.load(
            "/images/backgrounds/circuito_480x270.png"
    );
    private final List<Page> pages = new ArrayList<>();
    private int selectedPage;

    private record Entry(String author, String title, String role, String license) { }
    private record Page(String category, List<Entry> entries) { }

    public CreditsScreen(MenuScreen previousMenu) {
        this.previousMenu = previousMenu;
        String category = null;
        List<Entry> entries = new ArrayList<>();
        // keep attribution details in a readable resource instead of embedding them in code
        for (String rawLine : TextLoader.load("/CREDITS.txt")) {
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] fields = line.split(";", -1);
            if (fields.length != 8 || fields[0].isBlank() || fields[1].isBlank()
                    || fields[2].isBlank()) {
                throw new IllegalArgumentException("Invalid credits entry: " + line);
            }
            if (!fields[0].equals(category) || entries.size() == ENTRIES_PER_PAGE) {
                if (!entries.isEmpty()) pages.add(new Page(category, List.copyOf(entries)));
                category = fields[0];
                entries.clear();
            }
            entries.add(new Entry(fields[1], fields[2], fields[3], fields[4]));
        }
        if (!entries.isEmpty()) pages.add(new Page(category, List.copyOf(entries)));
        if (pages.isEmpty()) throw new IllegalArgumentException("The credits list is empty");
    }

    public MenuScreen getPreviousMenu() {
        return previousMenu;
    }

    public void movePage(int direction) {
        selectedPage = Math.floorMod(selectedPage + direction, pages.size());
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        Graphics2D screen = (Graphics2D) g.create();
        try {
            screen.drawImage(background, 0, 0, width, height, null);
            screen.setColor(new Color(0, 0, 0, 180));
            screen.fillRect(0, 0, width, height);
            drawText(screen, "CREDITS", width / 2, 26, 2.0, width - 120);
            drawText(screen, "v" + AppVersion.get(), width - 90, 30, 0.45, 150);
            Page page = pages.get(selectedPage);
            drawText(screen, page.category(), width / 2, 100, 0.85, width - 140);
            int count = page.entries().size();
            int firstY = count <= 2 ? 180 : 144;
            int spacing = count <= 2 ? 112 : 76;
            for (int i = 0; i < count; i++) {
                Entry entry = page.entries().get(i);
                int top = firstY + i * spacing;
                screen.setColor(new Color(20, 16, 30, 240));
                screen.fillRoundRect(70, top, width - 140, count <= 2 ? 94 : 70, 10, 10);
                screen.setColor(new Color(110, 65, 145));
                screen.drawRoundRect(70, top, width - 140, count <= 2 ? 94 : 70, 10, 10);
                double nameScale = count <= 2 ? 1.0 : 0.75;
                drawText(screen, entry.author(), width / 2, top + 6,
                        nameScale, width - 190);
                drawText(screen, entry.title(), width / 2,
                        top + (count <= 2 ? 43 : 31), 0.6, width - 190);
                if (!entry.role().isEmpty() || !entry.license().isEmpty()) {
                    String details = entry.role() + (entry.role().isEmpty() || entry.license().isEmpty()
                            ? "" : " - ") + entry.license();
                    drawText(screen, details, width / 2,
                            top + (count <= 2 ? 68 : 51), 0.43, width - 190);
                }
            }
            drawText(screen, "PAGE " + (selectedPage + 1) + " OF " + pages.size(),
                    width / 2, height - 76, 0.55, width - 120);
            drawText(screen, "SOURCES AND LICENSES IN CREDITS.TXT",
                    width / 2, height - 52, 0.45, width - 120);
            drawText(screen, "ARROWS CHANGE PAGE   ESC OR ENTER BACK",
                    width / 2, height - 28, 0.45, width - 120);
        } finally {
            screen.dispose();
        }
    }

    private void drawText(Graphics2D g, String text, int centerX, int top,
                          double preferred, int maxWidth) {
        text = GameText.display(text);
        // preserve exact author names even when punctuation is absent from the bitmap atlas
        int width = 0;
        int run = 0;
        for (int i = 0; i < text.length(); i++) {
            if (isPunctuation(text.charAt(i))) {
                width += font.getTextWidth(text.substring(run, i), 1);
                width += text.charAt(i) == '_' ? 14 : 6;
                run = i + 1;
            }
        }
        width += font.getTextWidth(text.substring(run), 1);
        double scale = Math.min(preferred, maxWidth / (double) Math.max(1, width));
        Graphics2D copy = (Graphics2D) g.create();
        try {
            copy.translate(centerX, top);
            copy.scale(scale, scale);
            int cursor = -width / 2;
            run = 0;
            for (int i = 0; i <= text.length(); i++) {
                if (i < text.length() && !isPunctuation(text.charAt(i))) continue;
                String part = text.substring(run, i);
                font.drawAligned(copy, part, cursor, 0, 1);
                cursor += font.getTextWidth(part, 1);
                if (i < text.length()) {
                    char mark = text.charAt(i);
                    copy.setColor(new Color(195, 65, 245));
                    if (mark == '_') {
                        copy.fillRect(cursor + 1, 29, 12, 3);
                        cursor += 14;
                    } else {
                        copy.fillRect(cursor + 1, 27, 3, 3);
                        if (mark == ':') copy.fillRect(cursor + 1, 13, 3, 3);
                        if (mark == ',') copy.fillRect(cursor, 30, 3, 3);
                        cursor += 6;
                    }
                }
                run = i + 1;
            }
        } finally {
            copy.dispose();
        }
    }

    private boolean isPunctuation(char character) {
        return character == '.' || character == '_' || character == ',' || character == ':';
    }

}
