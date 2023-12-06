package iniciativaselebi.com.guinealogiaediciontrivial;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.inappmessaging.FirebaseInAppMessaging;
import com.google.firebase.inappmessaging.FirebaseInAppMessagingClickListener;
import com.google.firebase.inappmessaging.model.Action;
import com.google.firebase.inappmessaging.model.InAppMessage;
import com.google.firebase.installations.FirebaseInstallations;
public class MainActivity extends AppCompatActivity {

    ImageView imageView;
    MediaPlayer swooshPlayer;
    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        // Listener for Firebase In-App Messaging
        FirebaseInAppMessaging.getInstance().addClickListener(new FirebaseInAppMessagingClickListener() {
            @Override
            public void messageClicked(InAppMessage inAppMessage, Action action) {
                // Handle in-app message click
                // You might want to log, show a toast, or redirect to a different activity
            }
        });

        FirebaseInstallations.getInstance().getId().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String installationId = task.getResult();
                // Log the Installation ID
                Log.d("MainActivity", "Firebase Installation ID: " + installationId);

                // Save installation ID in shared preferences
                saveInstallationIdInPreferences(installationId);
            } else {
                Log.e("MainActivity", "Unable to get Installation ID", task.getException());
            }
        });


        imageView = findViewById(R.id.logo);
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.myanim);
        Animation pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse_animation); // Load the pulse animation


        mediaPlayer = MediaPlayer.create(this, R.raw.maeledjidalot);  // Removed re-declaration
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // Do something when audio completes, if needed
            }
        });
        mediaPlayer.start();

        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Animation started
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Animation ended
                imageView.startAnimation(pulseAnim); // Start the pulse animation immediately
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Animation repeated
            }
        });
        imageView.setAnimation(anim);
        anim.start();

        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh); // Initializing the swooshPlayer

        // Setting OnClickListener to the RelativeLayout
        RelativeLayout mainLayout = findViewById(R.id.main_activity_layout);
        mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSwoosh();
                Intent intent = new Intent(MainActivity.this, Menuprincipal.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void saveInstallationIdInPreferences(String installationId) {
        SharedPreferences sharedPreferences = getSharedPreferences("appPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("firebaseInstallationId", installationId);
        editor.apply();
    }


    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (swooshPlayer != null) {
            swooshPlayer.release();
            swooshPlayer = null;
        }
        super.onDestroy();
    }

    private void playSwoosh() {
        if (swooshPlayer != null) {
            swooshPlayer.seekTo(0);
            swooshPlayer.start();
        }
    }
}
