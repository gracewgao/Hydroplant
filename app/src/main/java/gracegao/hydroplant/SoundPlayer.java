package gracegao.hydroplant;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

// This class accesses audio resources to play sound effects for the game
public class SoundPlayer {

    private AudioAttributes audioAttributes;
    private static SoundPool soundPool;

    private static int rainSound;
    private static int heartSound;
    private static int acidSound;

    final int SOUND_POOL_MAX = 3; // Maximum number of sounds that can be played at once

    // Constructor method to initialize audio files
    public SoundPlayer(Context context) {
        audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .setMaxStreams(SOUND_POOL_MAX)
                .build();
        // Loads sound resources
        rainSound = soundPool.load(context, R.raw.rain_sound, 1);
        heartSound = soundPool.load(context, R.raw.heart_sound, 1);
        acidSound = soundPool.load(context, R.raw.acid_sound, 1);
    }

    // Methods to play different audio clips

    public void playRainSound() {
        soundPool.play(rainSound, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playHeartSound() {
        soundPool.play(heartSound, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playAcidSound() {
        soundPool.play(acidSound, 1.0f, 1.0f, 1, 0, 1.0f);
    }
}
