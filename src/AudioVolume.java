import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

final class AudioVolume {

    private AudioVolume() {}

    static void apply(Clip clip, int percent) {
        // adjust the existing line without stopping, rewinding or reopening the sound
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float decibels = percent == 0 ? gain.getMinimum()
                    : (float) (20.0 * Math.log10(percent / 100.0));
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
        } else if (clip.isControlSupported(FloatControl.Type.VOLUME)) {
            FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.VOLUME);
            float value = volume.getMinimum()
                    + (volume.getMaximum() - volume.getMinimum()) * percent / 100.0f;
            volume.setValue(value);
        }
        if (clip.isControlSupported(BooleanControl.Type.MUTE)) {
            BooleanControl mute = (BooleanControl) clip.getControl(BooleanControl.Type.MUTE);
            mute.setValue(percent == 0);
        }
    }
}
