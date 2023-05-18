package iniciativaselebi.com.guinealogiaediciontrivial;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class GameOverActivity extends AppCompatActivity {

    Button gameOverText;

    private Animation disintegratingAnimation;
    private MediaPlayer mediaPlayer;



    @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_game_over);

        mediaPlayer = MediaPlayer.create(this, R.raw.gameover);
        mediaPlayer.start();


        gameOverText = findViewById(R.id.gameOverText);
        disintegratingAnimation = AnimationUtils.loadAnimation(this, R.anim.disintegrating_animation);

            gameOverText.startAnimation(disintegratingAnimation);

            // Set a delay before moving to the next activity
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Replace "NextActivity.class" with the next activity you want to move to
                    Intent nextActivityIntent = new Intent(GameOverActivity.this, ClassficationActivity.class);
                    startActivity(nextActivityIntent);
                    finish();
                }
            }, 3500); // 3000ms = 3 seconds
        }
    }

