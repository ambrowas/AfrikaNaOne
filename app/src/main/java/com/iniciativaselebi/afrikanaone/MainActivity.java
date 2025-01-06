package com.iniciativaselebi.afrikanaone;

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

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.inappmessaging.FirebaseInAppMessaging;
import com.google.firebase.inappmessaging.FirebaseInAppMessagingClickListener;
import com.google.firebase.inappmessaging.model.Action;
import com.google.firebase.inappmessaging.model.InAppMessage;
import com.google.firebase.installations.FirebaseInstallations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;



public class MainActivity extends AppCompatActivity {

    ImageView imageView;
    MediaPlayer swooshPlayer;
    MediaPlayer mediaPlayer;
    TextView TextViewClicAqui, TextViewProverb;
    private static final int MY_REQUEST_CODE = 100;
    private AppUpdateManager appUpdateManager;
    private FirebaseFirestore db;
    private Handler fadeHandler = new Handler();

    private boolean isUpdateCheckPending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeFirebase();
        checkForAppUpdates();
        setupFirebaseInAppMessaging();
        setupDatabaseAndInstallationId();
        animateLogoAndSetVisibility();
        setupMediaPlayer();
        handleMainLayoutClick();
        fetchRandomProverb();
    }

    private void initializeFirebase() {
        FirebaseApp.initializeApp(this);
        if (FirebaseApp.getApps(this).isEmpty()) {
            Log.e("FirebaseInit", "Firebase initialization failed.");
        } else {
            Log.d("FirebaseInit", "Firebase initialized successfully.");
        }
    }

    private void checkForAppUpdates() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(this);
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
                }
            }
        });
    }

    private void setupFirebaseInAppMessaging() {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        FirebaseInAppMessaging.getInstance().addClickListener((inAppMessage, action) -> {
            // Handle in-app message click here
        });
    }

    private void setupDatabaseAndInstallationId() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseInstallations.getInstance().getId().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String installationId = task.getResult();
                Log.d("MainActivity", "Firebase Installation ID: " + installationId);
                saveInstallationIdInPreferences(installationId);
            } else {
                Log.e("MainActivity", "Unable to get Installation ID", task.getException());
            }
        });
    }

    private void animateLogoAndSetVisibility() {
        TextView textViewClicAqui = findViewById(R.id.TextViewClicAqui);
        new Handler().postDelayed(() -> {
            if (!isFinishing()) {
                textViewClicAqui.setVisibility(View.VISIBLE);
            }
        }, 10000);

        ImageView imageView = findViewById(R.id.logo);
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.myanim);
        Animation pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse_animation);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                imageView.startAnimation(pulseAnim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        imageView.setAnimation(anim);
        anim.start();
    }

    private void setupMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this, R.raw.bikutsi);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d("MediaPlayer", "Playback completed. Stopping the player.");
                stopMediaPlayer();
            }
        });

        mediaPlayer.start();

        // Schedule fade-out after 8 seconds
        fadeHandler.postDelayed(() -> fadeOutMediaPlayer(), 8000);

        // Schedule stop after 10 seconds
        fadeHandler.postDelayed(() -> stopMediaPlayer(), 10000);
    }

    private synchronized void fadeOutMediaPlayer() {
        if (mediaPlayer == null) {
            Log.e("MainActivity", "MediaPlayer is null. Cannot fade out.");
            return;
        }

        new Thread(() -> {
            try {
                float volume = 1.0f;
                while (volume > 0.0f) {
                    synchronized (this) {
                        if (mediaPlayer == null) return;
                        volume -= 0.05f;
                        mediaPlayer.setVolume(Math.max(volume, 0.0f), Math.max(volume, 0.0f));
                    }
                    Thread.sleep(100);
                }
                synchronized (this) {
                    if (mediaPlayer != null) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                }
            } catch (Exception e) {
                Log.e("fadeOutMediaPlayer", "Error during fade-out", e);
            }
        }).start();
    }
    private synchronized void stopMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (IllegalStateException e) {
                Log.e("MainActivity", "Error while stopping MediaPlayer", e);
            }
        }
    }
    private void handleMainLayoutClick() {
        RelativeLayout mainLayout = findViewById(R.id.main_activity_layout);
        mainLayout.setOnClickListener(v -> {
            // Stop the current media playback
            stopMediaPlayer();

            // Play the swoosh sound and navigate to the next screen
            playSwoosh();

            // Delay to allow swoosh sound to play before navigating
            mainLayout.postDelayed(() -> {
                startActivity(new Intent(MainActivity.this, Menuprincipal.class));
                finish();
            }, 500); // 500ms delay
        });
    }

    private void playSwoosh() {
        if (swooshPlayer != null) {
            swooshPlayer.release();
        }
        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);
        swooshPlayer.start();
    }

    private void fetchRandomProverb() {
        TextViewProverb = findViewById(R.id.TextViewProverb);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("PROVERBS").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<DocumentSnapshot> documents = task.getResult().getDocuments();
                if (!documents.isEmpty()) {
                    Random random = new Random();
                    String randomProverb = documents.get(random.nextInt(documents.size())).getString("text");
                    if (randomProverb != null && !randomProverb.isEmpty()) {
                        TextViewProverb.setText(randomProverb);
                        TextViewProverb.setVisibility(View.VISIBLE);
                    } else {
                        TextViewProverb.setVisibility(View.GONE);
                    }
                } else {
                    TextViewProverb.setVisibility(View.GONE);
                }
            } else {
                TextViewProverb.setVisibility(View.GONE);
                Log.e("MainActivity", "Failed to fetch proverbs", task.getException());
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                // The update was cancelled by the user or it failed.
                // You can prompt the user again to update the app.
                checkForAppUpdates();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isUpdateCheckPending) {
            isUpdateCheckPending = true;
            checkForAppUpdates();
        }
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

}