package com.iniciativaselebi.afrikanaone;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.FirebaseApp;


public class ModoLibre extends AppCompatActivity {
    Button button_jugar;
    Button button_save, buttonvolver;
    TextView TextViewSaludo2;
    TextView TextViewRecord;
    EditText editTextNombre;
    int scoretotal;
    private SharedPreferences sharedPreferences;
    private ValueAnimator animator;
    String savedName;
    String message;
    String highScoreText;
    int highScore;
    MediaPlayer swooshPlayer;
    ImageView logo;
    private Animation pulseAnimation;
    private boolean isGooglePlayServicesAvailable;


@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_modo_libre);

        initializeUI();
        checkGooglePlayServicesAvailability();
        handleUserSession();
    }

    private void initializeUI() {
        editTextNombre = findViewById(R.id.editTextNombre);
        TextViewRecord = findViewById(R.id.TextViewRecord);
        TextViewSaludo2 = findViewById(R.id.TextViewSaludo2);
        button_save = findViewById(R.id.button_save);
        button_jugar = findViewById(R.id.button_jugar);
        buttonvolver = findViewById(R.id.buttonvolver);
        logo = findViewById(R.id.logo);

        setupListeners();
        animateLogo();
        setupColorChangeAnimation();
        setupMediaPlayer();
    }

    private void setupMediaPlayer() {
        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);

    }

    private void setupListeners() {
        setupSaveButton();
        setupPlayButton();
        setupReturnButton();
    }

    private void handleUserSession() {
        sharedPreferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
        savedName = sharedPreferences.getString("nombre", "");

        if (!savedName.isEmpty()) {
            displayExistingUser(savedName);
        } else {
            requestNewUserName();
            TextViewRecord.setVisibility(View.GONE);  // Directly hide the high score here
        }
    }


    private void displayExistingUser(String savedName) {
        // Display the greeting and record
        String message = "¡Hello there " + savedName + "!";
        TextViewSaludo2.setText(message);
        button_save.setText("SWITCH PLAYER");
        highScore = sharedPreferences.getInt("highscore", 0);
        String highScoreText = "THE CURRENT RECORD IS " + highScore + " POINTS";
        TextViewRecord.setText(highScoreText);
        editTextNombre.setVisibility(View.GONE); // Hide the EditText
        TextViewRecord.setVisibility(View.VISIBLE); // Show the high score
    }
    private void requestNewUserName() {
        // Display the EditText for name input
        button_save.setText("SAVE");
        editTextNombre.setVisibility(View.VISIBLE); // Show the EditText
        TextViewSaludo2.setText(""); // Clear any previous greeting
        TextViewRecord.setVisibility(View.GONE); // Hide the high score when no user is logged in
    }

    private void setupSaveButton() {
        button_save.setOnClickListener(v -> handleSaveOrSwitch());
    }

    private void handleSaveOrSwitch() {
        if (button_save.getText().toString().equals("SWITCH PLAYER")) {
            clearPreferences();
        } else {
            saveNewUser();
        }
    }
    private void saveNewUser() {
        String nombre = editTextNombre.getText().toString().trim();
        if (!nombre.isEmpty()) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("nombre", nombre);
            editor.apply();

            updateUIAfterNameSaved(nombre);  // Call the method here after saving the name
        } else {
            Toast.makeText(getApplicationContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
        }
    }
    private void clearPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("nombre");
        editor.apply();
        updateUIForNewUser();
    }

    private void updateUIForNewUser() {
        TextViewSaludo2.setText("");
        TextViewRecord.setText("CURRENT RECORD IS 0 POINTS");
        editTextNombre.setText("");
        editTextNombre.setVisibility(View.VISIBLE);
        button_save.setText("SAVE");
    }
    private void updateUIAfterNameSaved(String nombre) {
        String message = "¡Hello there " + nombre + " !";
        TextViewSaludo2.setText(message);
        editTextNombre.setVisibility(View.GONE); // Hide the EditText after saving
        button_save.setText("SWITCH PLAYER");
        updateScoreDisplay(); // This now also handles visibility
        Toast.makeText(getApplicationContext(), "Name saved", Toast.LENGTH_SHORT).show();
        playSwoosh();  // Sound effect on saving
    }

    private void setupPlayButton() {
        button_jugar.setOnClickListener(v -> {
            playSwoosh();  // Play the swoosh sound
            // Add a delay to allow the sound to play before transitioning
            button_jugar.postDelayed(() -> {
                startActivity(new Intent(ModoLibre.this, PreguntasModoLibre2.class));
                finish();
            }, 500); // 500ms delay
        });
    }

    private void setupReturnButton() {
        buttonvolver.setOnClickListener(v -> {
            playSwoosh();  // Play the swoosh sound
            // Add a small delay to allow the sound to play before transitioning
            buttonvolver.postDelayed(() -> {
                startActivity(new Intent(ModoLibre.this, Menuprincipal.class));
                finish();
            }, 500); // 500ms delay
        });
    }


    private void animateLogo() {
        Animation pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation);
        logo.startAnimation(pulseAnimation);
    }

    private void setupColorChangeAnimation() {
        final int[] colors = {Color.RED, Color.GREEN, Color.BLUE};
        animator = ValueAnimator.ofInt(colors);
        animator.setEvaluator(new ArgbEvaluator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> TextViewRecord.setTextColor((int) animation.getAnimatedValue()));
        animator.start();
    }

    private void playSwoosh() {
        // Stop and release any existing instance
        stopSwoosh();

        // Create a new MediaPlayer instance
        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);

        if (swooshPlayer != null) {
            // Set listener to release the MediaPlayer after completion
            swooshPlayer.setOnCompletionListener(mp -> {
                mp.release(); // Release resources
                swooshPlayer = null; // Prevent invalid references
                Log.d("MediaPlayer", "MediaPlayer released after playback");
            });

            // Start playback and log the event
            try {
                swooshPlayer.start();
                Log.d("MediaPlayer", "Swoosh sound started");
            } catch (IllegalStateException e) {
                Log.e("MediaPlayer", "Failed to start playback: " + e.getMessage());
            }
        } else {
            Log.e("MediaPlayer", "Failed to create MediaPlayer instance");
        }
    }


    private void stopSwoosh() {
        if (swooshPlayer != null) {
            try {
                if (swooshPlayer.isPlaying()) {
                    swooshPlayer.stop(); // Stop playback if it's running
                }
            } catch (IllegalStateException e) {
                Log.e("MediaPlayer", "Error stopping MediaPlayer: " + e.getMessage());
            } finally {
                swooshPlayer.release(); // Release resources
                swooshPlayer = null; // Avoid memory leaks
                Log.d("MediaPlayer", "MediaPlayer stopped and released");
            }
        }
    }

    private void updateScoreDisplay() {
        highScore = sharedPreferences.getInt("highscore", 0);
        if (highScore > 0) {
            String highScoreText = "CURRENT RECORD IS " + highScore + " POINTS";
            TextViewRecord.setText(highScoreText);
            TextViewRecord.setVisibility(View.VISIBLE); // Only show if there's a high score
        } else {
            TextViewRecord.setVisibility(View.GONE); // No high score to display
        }
    }
    private void checkGooglePlayServicesAvailability() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, 2404).show();
            } else {
                Log.e("GooglePlayServices", "This device does not support Google Play Services.");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (swooshPlayer != null) {
            swooshPlayer.release();
            swooshPlayer = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSwoosh();
        if (logo != null) {
            logo.clearAnimation();
        }
        if (animator != null) {
            animator.cancel();
        }
    }
}
