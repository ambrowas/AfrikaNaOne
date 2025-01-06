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
import androidx.appcompat.app.AlertDialog;
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
    private ValueAnimator animator;
    String savedName;
    String message;
    String highScoreText;
    int highScore;
    MediaPlayer swooshPlayer;
    ImageView logo;
    private Animation pulseAnimation;
    private boolean isGooglePlayServicesAvailable;
    private SharedPreferences sharedPreferences;
    SharedPreferences prefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_modo_libre);
        prefs = getSharedPreferences("quiz_prefs", MODE_PRIVATE);
        sharedPreferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);

        initializeUI();
        checkGooglePlayServicesAvailability();
        handleUserSession();

        // Handle the latest score passed via Intent
        Intent intent = getIntent();
        int latestScore = intent.getIntExtra("latest_score", -1);
        if (latestScore != -1) {
            Log.d("ModoLibre", "Latest score received: " + latestScore);

            // Save latest score and check for high score
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("latest_score", latestScore);
            editor.apply();

            checkForNewHighScore();
        } else {
            // If no latest score, update display with current high score
            updateScoreDisplay();
        }
    }
    private void initializeUI() {
        sharedPreferences = getSharedPreferences("quiz_prefs", MODE_PRIVATE);
        editTextNombre = findViewById(R.id.editTextNombre);
        TextViewRecord = findViewById(R.id.TextViewRecord);
        TextViewSaludo2 = findViewById(R.id.TextViewSaludo2);
        button_save = findViewById(R.id.button_save);
        button_jugar = findViewById(R.id.button_jugar);
        buttonvolver = findViewById(R.id.buttonvolver);
        logo = findViewById(R.id.logo);

        setupListeners();
        animateLogo();
       // setupColorChangeAnimation();
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


    private void checkForNewHighScore() {
        int latestScore = prefs.getInt("latest_score", 0); // Retrieve latest score
        int highScore = prefs.getInt("highscore", 0); // Retrieve current high score

        Log.d("ModoLibre", "Current high score: " + highScore);
        Log.d("ModoLibre", "New score to compare: " + latestScore);

        if (latestScore > highScore) {
            Log.d("ModoLibre", "New high score detected!");

            // Update high score
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("highscore", latestScore);
            editor.apply();

            Log.d("ModoLibre", "High score updated to: " + latestScore);

            // Show dialog if the activity is not finishing or destroyed
            if (!isFinishing() && !isDestroyed()) {
                runOnUiThread(() -> {
                    Log.d("ModoLibre", "Calling showNewHighScoreDialog.");
                    showNewHighScoreDialog(latestScore);
                });
            } else {
                Log.w("ModoLibre", "Dialog not shown. Activity is finishing or destroyed.");
            }
        } else {
            Log.d("ModoLibre", "No new high score detected.");
        }

        // Update the score display
        updateScoreDisplay();
    }

    private void showNewHighScoreDialog(int latestScore) {
        Log.d("ModoLibre", "Preparing to show high score dialog for score: " + latestScore);
        Sounds.playMagicalSound(getApplicationContext());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Congratulations!")
                .setMessage("You've set a new high score: " + latestScore + " points!")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    Log.d("ModoLibre", "High score dialog dismissed.");
                    Sounds.playSwooshSound(this);
                });

        AlertDialog alert = builder.create();
        Log.d("ModoLibre", "Showing high score dialog.");
        alert.show();
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
            playSwoosh();
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
            Sounds.playMagicalSound(this);
            updateUIAfterNameSaved(nombre);  // Call the method here after saving the name
        } else {
            Sounds.playWarningSound(this);
            Toast.makeText(getApplicationContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
        }
    }
    private void clearPreferences() {
        SharedPreferences.Editor editor = prefs.edit(); // Use prefs for high score and related data
        editor.remove("nombre"); // Remove the user name
        editor.remove("highscore"); // Reset the high score
        editor.remove("latest_score"); // Optionally clear the latest score
        editor.apply();

        Log.d("ModoLibre", "Preferences cleared. High score reset to 0.");

        // Update UI for a new user
        updateUIForNewUser();
    }

    private void updateUIForNewUser() {
        TextViewSaludo2.setText("");
        TextViewRecord.setText("CURRENT RECORD IS 0 POINTS"); // Explicitly show reset state
        editTextNombre.setText("");
        editTextNombre.setVisibility(View.VISIBLE); // Show input for a new user
        button_save.setText("SAVE");
        Log.d("ModoLibre", "UI updated for a new user. High score set to 0.");
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
        // Retrieve high score from "quiz_prefs"
        highScore = prefs.getInt("highscore", 0);
        Log.d("ModoLibre", "Displaying high score: " + highScore);

        if (highScore > 0) {
            // Display the high score if it's greater than 0
            String highScoreText = "CURRENT RECORD IS " + highScore + " POINTS";
            TextViewRecord.setText(highScoreText);
            TextViewRecord.setVisibility(View.VISIBLE);
        } else {
            // Hide the high score TextView if no high score exists
            TextViewRecord.setVisibility(View.GONE);
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
