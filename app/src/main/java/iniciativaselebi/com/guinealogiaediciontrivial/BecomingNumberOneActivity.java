package iniciativaselebi.com.guinealogiaediciontrivial;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class BecomingNumberOneActivity extends AppCompatActivity {

    TextView felicidadestextview;
    private Animation disintegratingAnimation;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_becoming_number_one);


            mediaPlayer = MediaPlayer.create(this, R.raw.hallelujah);
            mediaPlayer.start();


            felicidadestextview = findViewById(R.id.felicidadestextview);
            disintegratingAnimation = AnimationUtils.loadAnimation(this, R.anim.disintegrating_animation);

           felicidadestextview.startAnimation(disintegratingAnimation);

            // Set a delay before moving to the next activity
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Replace "NextActivity.class" with the next activity you want to move to
                    Intent nextActivityIntent = new Intent(BecomingNumberOneActivity.this, ClassficationActivity.class);
                    startActivity(nextActivityIntent);
                    finish();
                }
            }, 3000); // 3000ms = 3 seconds
        }
    }


