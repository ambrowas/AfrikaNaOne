package iniciativaselebi.com.guinealogiaediciontrivial;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import iniciativaselebi.com.guinealogiaediciontrivial.R;

public class GameOverActivity extends AppCompatActivity {

    Button gameOverText;
    private Animation disintegratingAnimation;
    private MediaPlayer mediaPlayer;

    MediaPlayer swooshPlayer;
    private Animation pulseAnimation;
    int aciertos, puntuacion, errores;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);


        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);
        mediaPlayer = MediaPlayer.create(this, R.raw.gameover);
        mediaPlayer.start();
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation);
        gameOverText = findViewById(R.id.gameOverText);
        disintegratingAnimation = AnimationUtils.loadAnimation(this, R.anim.disintegrating_animation);

        disintegratingAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // No need to implement if you don't have logic for this
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Start pulseAnimation when disintegratingAnimation ends
                gameOverText.startAnimation(pulseAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // No need to implement if you don't have logic for this
            }
        });

        gameOverText.startAnimation(disintegratingAnimation);

        // Listen for any touch on the screen to navigate to the next activity
        View rootView = findViewById(android.R.id.content);
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSwoosh();

                int aciertos = getIntent().getIntExtra("aciertos", 0);
                int puntuacion = getIntent().getIntExtra("puntuacion", 0);
                int errores = getIntent().getIntExtra("errores", 0);

                saveToSharedPreferences(aciertos, puntuacion, errores);

                Intent nextActivityIntent = new Intent(GameOverActivity.this, ClassficationActivity.class);
                nextActivityIntent.putExtra("aciertos", aciertos);
                nextActivityIntent.putExtra("puntuacion", puntuacion);
                nextActivityIntent.putExtra("errores", errores);

                startActivity(nextActivityIntent);
                finish();
            }
        });
    }
    private void saveToSharedPreferences(int aciertos, int puntuacion, int errores) {
        SharedPreferences sharedPreferences = getSharedPreferences("GameStats", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt("aciertos", aciertos);
        editor.putInt("puntuacion", puntuacion);
        editor.putInt("errores", errores);

        editor.apply();
    }

    private void playSwoosh() {
        if (swooshPlayer != null) {
            swooshPlayer.seekTo(0);
            swooshPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        if (swooshPlayer != null) {
            swooshPlayer.release();
            swooshPlayer = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

}
