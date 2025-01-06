package com.iniciativaselebi.afrikanaone;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

public class Sounds {
    private static MediaPlayer mediaPlayer;

    // Play the magical sound (magic.mp3)
    public static void playMagicalSound(Context context) {
        playSound(context, R.raw.magic);
    }

    // Play the warning sound (warning.mp3)
    public static void playWarningSound(Context context) {
        playSound(context, R.raw.warning);
    }


    // Play the swoosh sound (swoosh.mp3)
    public static void playSwooshSound(Context context) {
        playSound(context, R.raw.swoosh);
    }

    // Generalized method to play a sound
    private static void playSound(Context context, int soundResId) {
        if (mediaPlayer != null) {
            mediaPlayer.release(); // Release any previously playing sound
        }

        mediaPlayer = MediaPlayer.create(context, soundResId);
        if (mediaPlayer != null) {
            try {
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release(); // Release resources after playback
                    mediaPlayer = null;
                });
            } catch (IllegalStateException e) {
                Log.e("Sounds", "Error playing sound: " + e.getMessage());
            }
        } else {
            Log.e("Sounds", "Failed to create MediaPlayer instance for sound ID: " + soundResId);
        }
    }
}