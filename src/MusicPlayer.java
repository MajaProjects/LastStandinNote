import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

public final class MusicPlayer implements AutoCloseable {

    private final Clip clip;
    private boolean paused;
    private int pausedFrame;
    private long pausedPositionMs;
    private boolean playingOnce;

    public MusicPlayer(String path) {
        URL resource = MusicPlayer.class.getResource(path);

        if (resource == null) {
            throw new IllegalArgumentException("Music file not found: " + path);
        }

        Clip loadedClip = null;

        // load one playback channel for this music track
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(resource)) {
            loadedClip = AudioSystem.getClip();
            loadedClip.open(stream);
        } catch (UnsupportedAudioFileException
                 | IOException
                 | LineUnavailableException
                 | IllegalArgumentException error) {
            if (loadedClip != null) {
                loadedClip.close();
            }

            throw new IllegalStateException(
                    "Could not load music: " + path,
                    error
            );
        }

        clip = loadedClip;
    }

    public void playLoop() {
        // start from the beginning and repeat until stopped
        clip.stop();
        paused = false;
        playingOnce = false;
        clip.setFramePosition(0);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }
    public void play() {
        // start the song from the beginning and play it once
        clip.stop();
        paused = false;
        playingOnce = true;
        clip.setFramePosition(0);
        clip.start();
    }

    public long getPositionMs() {
        // use the audio position as the clock for gameplay
        return paused ? pausedPositionMs : clip.getMicrosecondPosition() / 1000;
    }

    public boolean isFinished() {
        // paused, stopped and looping tracks are not completed songs
        return playingOnce && !paused
                && clip.getFrameLength() > 0
                && clip.getFramePosition() >= clip.getFrameLength();
    }

    public void pause() {
        if (paused) {
            return;
        }

        // remember the stopped audio frame without rewinding the track
        clip.stop();
        pausedFrame = clip.getFramePosition();
        pausedPositionMs = clip.getMicrosecondPosition() / 1000;
        paused = true;
    }

    public void resume() {
        if (!paused) {
            return;
        }

        clip.setFramePosition(pausedFrame);
        paused = false;

        // an already finished track must not restart from the beginning
        if (pausedFrame < clip.getFrameLength()) {
            clip.start();
        }
    }

    public void stop() {
        // stop playback when another track takes over
        clip.stop();
        paused = false;
        playingOnce = false;
    }

    @Override
    public void close() {
        // release the audio channel when this player is discarded
        clip.close();
        paused = false;
        playingOnce = false;
    }
}
