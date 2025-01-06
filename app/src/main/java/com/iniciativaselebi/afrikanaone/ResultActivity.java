package com.iniciativaselebi.afrikanaone;


import static com.iniciativaselebi.afrikanaone.SharedPreferenceHelper.PREFS_NAME;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Set;

public class ResultActivity extends AppCompatActivity {
    private BatchManager batchManager;

    private TextView DisplayNivel, DisplayPuntos, DisplayScore, DisplayErrores;
    private ImageView trofeo;
    private Button jugarotravez, salir;
    private MediaPlayer swooshPlayer, beginnerMediaPlayer, averageMediaPlayer, expertMediaPlayer;

    private int score, errores, scoreTotal;
    private boolean allBatchesCompleted;

    // Constants
    private static final String SHARED_PREFS = "quiz_prefs";
    private static final String KEY_HIGHSCORE = "highscore";
    private static final int QUESTIONS_PER_BATCH = 10;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        initViews();
        batchManager = new BatchManager(this);  // Initialize BatchManager
        fetchIntentData();
        calculateAndDisplayResults();


        // Set individual button listeners
        jugarotravez.setOnClickListener(v -> {
            if (batchManager.allBatchesCompleted()) {
                showCompletionDialog();
            } else {
                restartQuiz(); // Load a new batch if batches are not yet completed
            }
        });

        salir.setOnClickListener(v -> {
            if (batchManager.allBatchesCompleted()) {
                showCompletionDialog();
            } else {
                navigateToMenuModoLibre(); // Only navigate if not all batches are completed
            }
        });
    }

    private void initViews() {
        DisplayNivel = findViewById(R.id.DisplayNivel);
        DisplayPuntos = findViewById(R.id.displayPuntos);
        DisplayScore = findViewById(R.id.displayScore);
        DisplayErrores = findViewById(R.id.displayErrores);
        trofeo = findViewById(R.id.trofeo);
        jugarotravez = findViewById(R.id.jugarotravez);
        salir = findViewById(R.id.salir);
        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);
    }

    private void fetchIntentData() {
        Intent intent = getIntent();
        score = intent.getIntExtra("correct_answers", 0);
        errores = intent.getIntExtra("errors", 0);
        scoreTotal = intent.getIntExtra("scoretotal", 0);

        // Add logs to confirm received values
        Log.d("ResultActivity", "Received correct answers: " + score);
        Log.d("ResultActivity", "Received errors: " + errores);
        Log.d("ResultActivity", "Received total score: " + scoreTotal);
    }
    private void calculateAndDisplayResults() {
        Log.d("batchFlow", "Calculating and displaying results.");

        int scorePercentage = (int) (((double) score / QUESTIONS_PER_BATCH) * 100);

        Log.d("ResultActivity", "scorePercentage: " + scorePercentage);

        DisplayPuntos.setText("You nailed " + score + " correct answers");
        DisplayErrores.setText("You made " + errores + " mistakes");
        DisplayScore.setText("Your total score is " + scoreTotal + " points");

        updateLevelDisplay(scorePercentage);
        saveHighScore();
    }

    private void saveHighScore() {
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        int storedHighscore = prefs.getInt(KEY_HIGHSCORE, 0);

        // Add logs to display the current high score and the new score
        Log.d("ResultActivity", "Current high score: " + storedHighscore);
        Log.d("ResultActivity", "New score to compare: " + scoreTotal);

        if (scoreTotal > storedHighscore) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_HIGHSCORE, scoreTotal);
            editor.apply();

        }
    }

    private void updateLevelDisplay(int scorePercentage) {
        if (scorePercentage <= 50) {
            DisplayNivel.setText("IT'S PEOPLE LIKE YOU HOLDING AFRICA BACK");
            trofeo.setImageResource(R.drawable.beginer);
            applyFadeInBlinkingEffect(trofeo);
            playBeginnerSound();
        } else if (scorePercentage < 90) {
            DisplayNivel.setText("NOT BAD, BUT YOU COULD DO MORE FOR THE CONTINENT");
            trofeo.setImageResource(R.drawable.average);
            applyFadeInBlinkingEffect(trofeo);
            playAverageSound();
        } else {
            DisplayNivel.setText("FANTASTIC. WE NEED MORE AFRICANS LIKE YOU");
            trofeo.setImageResource(R.drawable.expert);
            applyFadeInBlinkingEffect(trofeo);
            playExpertSound();
        }
    }

    private void applyFadeInBlinkingEffect(ImageView imageView) {
        // Load the animations
        Animation pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse_animation); // Load the pulse animation

        // Fade in effect
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(800); // Duration for fade-in

        // Blinking effect
        ScaleAnimation blink = new ScaleAnimation(
                1.0f, 1.1f, 1.0f, 1.1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        blink.setDuration(300);
        blink.setRepeatCount(3);
        blink.setRepeatMode(Animation.REVERSE);

        // Combine animations
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(fadeIn);
        animationSet.addAnimation(blink);
        animationSet.addAnimation(pulseAnim); // Add the pulse animation

        // Start the animation
        imageView.startAnimation(animationSet);
    }

    private void playBeginnerSound() {
        if (beginnerMediaPlayer == null) {
            beginnerMediaPlayer = MediaPlayer.create(this, R.raw.beginner);

        }
        beginnerMediaPlayer.start();
    }

    private void playAverageSound() {
        if (averageMediaPlayer == null) {
            averageMediaPlayer = MediaPlayer.create(this, R.raw.average);
        }
        averageMediaPlayer.start();
    }

    private void playExpertSound() {
        if (expertMediaPlayer == null) {
            expertMediaPlayer = MediaPlayer.create(this, R.raw.expert);
        }
        expertMediaPlayer.start();
    }

    private void playSwoosh() {
        if (swooshPlayer != null) {
            swooshPlayer.start();
        }
    }

    private void restartGame() {
        batchManager.resetQuizProgress(); // Ensure game starts fresh
        navigateToMainMenu();
    }

    private void navigateToMenuModoLibre() {
        // Save the latest score to local storage
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("latest_score", scoreTotal);
        editor.apply();
        Intent intent = new Intent(ResultActivity.this, ModoLibre.class);
        playSwoosh();
        startActivity(intent);
        finish();
    }

    private void navigateToMainMenu() {
        Intent intent = new Intent(ResultActivity.this, Menuprincipal.class);
        playSwoosh();
        startActivity(intent);
        finish();
    }

    private void restartQuiz() {
        Intent intent = new Intent(ResultActivity.this, PreguntasModoLibre2.class);
        playSwoosh();
        intent.putExtra("loadNewBatch", true);
        startActivity(intent);
        finish();
    }

    private void showCompletionDialog() {
        // Inflate the custom dialog layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_custom_layout, null);

        // Create the dialog with the custom view
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setOnDismissListener(dialog1 -> restartGame())
                .create();

        // Set click listener for OK button
        Button okButton = dialogView.findViewById(R.id.dialog_ok_button);
        okButton.setOnClickListener(v -> {
            dialog.dismiss();
            restartGame();
        });

        // Show the dialog
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayers();
    }

    private void releaseMediaPlayers() {
        if (swooshPlayer != null) {
            swooshPlayer.release();
            swooshPlayer = null;
        }
        if (beginnerMediaPlayer != null) {
            beginnerMediaPlayer.release();
            beginnerMediaPlayer = null;
        }
        if (averageMediaPlayer != null) {
            averageMediaPlayer.release();
            averageMediaPlayer = null;
        }
        if (expertMediaPlayer != null) {
            expertMediaPlayer.release();
            expertMediaPlayer = null;
        }
    }
}
