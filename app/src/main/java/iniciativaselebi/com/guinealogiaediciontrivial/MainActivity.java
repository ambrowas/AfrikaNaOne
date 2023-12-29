package iniciativaselebi.com.guinealogiaediciontrivial;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;
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

    TextView TextViewClicAqui;
    private static final int MY_REQUEST_CODE = 100;
    private AppUpdateManager appUpdateManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appUpdateManager = AppUpdateManagerFactory.create(this);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                // Request the update.
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this,
                            MY_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        });




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


        TextViewClicAqui = findViewById(R.id.TextViewClicAqui);
// Handler to delay the visibility
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) { // Check if the activity is still active
                    TextViewClicAqui.setVisibility(View.VISIBLE);
                }
            }
        }, 10000); // 2000ms delay


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



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                // The update was cancelled by the user or it failed.
                // You can prompt the user again to update the app.
                checkForAppUpdate();
            }
        }
    }

    private void checkForAppUpdate() {
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this,
                            MY_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                    // Handle the exception here
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkForAppUpdate(); // Check for app update when the app resumes
    }
}