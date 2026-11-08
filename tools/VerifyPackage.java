// SPDX-License-Identifier: GPL-3.0-only
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

// verify real resources from the built jar without opening the game window
public final class VerifyPackage {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !AppVersion.get().equals(args[0])) {
            throw new IllegalStateException("Packaged version differs from the release version");
        }
        int charts = 0;
        int audioFiles = 0;
        for (String line : TextLoader.load("/songs/songs.txt")) {
            if (line.isBlank() || line.strip().startsWith("#")) continue;
            String folder = "/songs/" + line.split(";", -1)[0].strip() + "/";
            PcmTrack instrumental = PcmTrack.load(folder + "instrumental.wav", false);
            audioFiles++;
            for (String difficulty : List.of("user", "admin", "root")) {
                String path = folder + "chart_" + difficulty + ".txt";
                SongChart chart = SongChart.load(path);
                int declared = -1;
                for (String row : TextLoader.load(path)) {
                    if (row.startsWith("TOTAL_NOTAS;")) declared = Integer.parseInt(row.split(";")[1].strip());
                }
                if (declared != chart.notes().size() || declared <= 0) {
                    throw new IllegalStateException("Invalid note count: " + path);
                }
                long duration = instrumental.frameCount() * 1000L / PcmTrack.SAMPLE_RATE;
                for (Note note : chart.notes()) {
                    if (note.timeMs() + note.durationMs() > duration + 1) {
                        throw new IllegalStateException("Note extends beyond audio: " + path);
                    }
                }
                charts++;
            }
            SongPlayer.validateVocalFiles(folder, true);
            for (int voice = 1; voice <= 2; voice++) {
                PcmTrack.load(folder + "vocals/voice" + voice + ".wav", false);
                audioFiles++;
            }
            var settings = TextLoader.loadSettings(folder + "vocals/playback.txt");
            for (String key : List.of("master_gain", "voice1_offset_ms", "voice2_offset_ms")) {
                double value = Double.parseDouble(settings.get(key));
                if (!Double.isFinite(value)) throw new IllegalStateException("Invalid playback setting: " + key);
            }
        }
        for (String name : List.of("comboSound", "countdown", "dying_sound", "gameover_music",
                "level_complete", "menu_back", "menu_confirm", "menu_move", "menu_music",
                "miss_sound_16bit", "miss_sound_2", "pause_music", "retry_confirm", "title_music")) {
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(
                    VerifyPackage.class.getResource("/audio/" + name + ".wav"))) {
                if (stream.getFrameLength() <= 0) throw new IllegalStateException("Empty audio: " + name);
            }
            audioFiles++;
        }
        new BitmapFont("/images/font/font.txt");
        int[] iconSizes = {16, 24, 32, 48, 64, 128, 256};
        var icons = AppIcon.loadImages();
        if (icons.size() != iconSizes.length) throw new IllegalStateException("Missing packaged window icons");
        for (int i = 0; i < iconSizes.length; i++) {
            if (icons.get(i).getWidth(null) != iconSizes[i] || icons.get(i).getHeight(null) != iconSizes[i]) {
                throw new IllegalStateException("Invalid packaged icon dimensions: " + iconSizes[i]);
            }
        }
        SpriteAtlas characters = new SpriteAtlas("/images/sprites/sprites.txt");
        for (String actor : List.of("tux", "windows", "kernel")) characters.getAnimation(actor, "idle").getFrame(0);
        characters.getAnimation("monitor", "versus").getFrame(0);
        new NoteRenderer(48, 12, 300);
        new NoteRenderer(48, 12, 300, true);
        for (String path : List.of("/images/backgrounds/circuito_480x270.png",
                "/images/backgrounds/fondo1_480x270.png", "/images/ui/barra_cancion.png")) {
            try (InputStream stream = VerifyPackage.class.getResourceAsStream(path)) {
                if (stream == null || ImageIO.read(stream) == null) throw new IllegalStateException("Invalid image: " + path);
            }
        }
        for (String line : TextLoader.load("/CREDITS.txt")) {
            if (!line.isBlank() && !line.strip().startsWith("#") && line.split(";", -1).length != 8) {
                throw new IllegalStateException("Invalid credits row");
            }
        }
        if (VerifyPackage.class.getResource("/META-INF/LICENSE") == null
                || VerifyPackage.class.getResource("/META-INF/LICENSE-ASSETS") == null
                || VerifyPackage.class.getResource("/META-INF/NOTICE.md") == null) {
            throw new IllegalStateException("Missing license notices");
        }
        System.out.println("Package resources verified: " + charts + " charts, " + audioFiles + " audio files, images and credits.");
    }
}
