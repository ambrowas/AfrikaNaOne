package com.iniciativaselebi.afrikanaone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.common.reflect.TypeToken;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PreguntasModoLibre2 extends AppCompatActivity {
    private BatchManager batchManager;
    private SharedPreferenceHelper prefHelper;

    private List<JsonQuestion> questionList = new ArrayList<>();
    private List<JsonQuestion> currentBatch;
    private List<Integer> shuffledBatchIndices = new ArrayList<>();
    private int batchIndex;
    private int questionCounter;

    private boolean answered = false;

    // Constants
    private static final int QUESTIONS_PER_BATCH = 10;
    private static final long COUNTDOWN_IN_MILLIS = 15000; // Countdown time in milliseconds

    // UI elements
    private TextView textviewpuntuacion, textviewcontadorpreguntas, textviewaciertos, textviewtiempo, textviewpregunta, textViewExplanation;

    private ImageView quizImageLibre;
    private RadioGroup radio_group;
    private RadioButton radio_button1, radio_button2, radio_button3;
    private Button buttonconfirmar;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;
    private MediaPlayer mediaPlayerCountdown, mediaPlayerRight, mediaPlayerNotRight;
    private JsonQuestion currentQuestion;

    private int scoretotal, score, totalBatches, errores;

    private Map<Integer, List<JsonQuestion>> questionBatches = new HashMap<>();

    private List<Integer> shuffledBatchOrder = new ArrayList<>();

    private Dialog progressDialog;

    private Handler vibrationHandler = new Handler(Looper.getMainLooper());
    private Runnable vibrationRunnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preguntas_modo_libre);



        batchManager = new BatchManager(this);
        batchManager.saveTotalBatches(totalBatches, questionBatches);

        prefHelper = new SharedPreferenceHelper(this);
        initViews();
        setupButtonListeners();
        setupRadioButtonListeners();

        Intent intent = getIntent();
        boolean loadNewBatch = intent.getBooleanExtra("loadNewBatch", false);

        if (loadNewBatch) {
            Log.d("batchFlow", "Restarting quiz and loading a new batch.");
            handleNewBatchLoad();
        } else {
            checkForLocalQuestions();
        }
    }
    private void startVibrationReminder() {
        vibrationRunnable = new Runnable() {
            private boolean hasVibrated = false; // ✅ Ensure vibration happens only once after 10 seconds
            private int elapsedTime = 0; // ✅ Track time elapsed

            @Override
            public void run() {
                if (!answered) { // ✅ Continue effect only if user hasn't confirmed
                    Log.d("VibrationReminder", "Flashing confirm button. Time elapsed: " + elapsedTime);

                    // ✅ Flash confirm button (Scaling Effect)
                    buttonconfirmar.animate()
                            .scaleX(1.1f).scaleY(1.1f).setDuration(300)
                            .withEndAction(() -> buttonconfirmar.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300));

                    elapsedTime += 3000; // ✅ Increase elapsed time by 3 seconds

                    // ✅ Trigger vibration after 10 seconds (only once)
                    if (!hasVibrated && elapsedTime >= 10000) {
                        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                        if (vibrator != null) {
                            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                        }
                        hasVibrated = true; // ✅ Prevent further vibrations
                        Log.d("VibrationReminder", "Vibration triggered at 10 seconds.");
                    }

                    // ✅ Continue effect every 3 seconds
                    vibrationHandler.postDelayed(this, 3000);
                }
            }
        };

        // ✅ Initial delay before the first effect starts
        vibrationHandler.postDelayed(vibrationRunnable, 3000);
    }

    // Function to stop the vibration reminder
    private void stopVibrationReminder() {
        vibrationHandler.removeCallbacks(vibrationRunnable); // ✅ Stop pending callbacks
        Log.d("VibrationReminder", "Vibration effect stopped.");
    }
    // Modify `setupButtonListeners` to integrate the vibration feature
    private void setupButtonListeners() {
        buttonconfirmar.setOnClickListener(view -> {
            if (!answered) { // ✅ User is confirming answer
                if (radio_button1.isChecked() || radio_button2.isChecked() || radio_button3.isChecked()) {
                    countDownTimer.cancel();
                    checkAnswer();
                    stopVibrationReminder(); // ✅ Stop vibration when confirmed
                    buttonconfirmar.setText(questionCounter < QUESTIONS_PER_BATCH ? "NEXT" : "FINISH");
                } else {
                    Sounds.playWarningSound(getApplicationContext());
                    showCustomToast("FEAR NOT. MAKE A CHOICE.");
                }
            } else { // ✅ User is proceeding to next question
                if (questionCounter < QUESTIONS_PER_BATCH) {
                    buttonconfirmar.setText("CONFIRM");
                    Sounds.playSwooshSound(getApplicationContext());
                    showNextQuestion();
                    stopVibrationReminder(); // ✅ Reset vibration before the next question
                } else {
                    Sounds.playSwooshSound(getApplicationContext());
                    finishQuiz();
                }
            }
        });
    }

    private void showCustomToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_custom, findViewById(R.id.toast_layout));

        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    private void initViews() {
        quizImageLibre = findViewById(R.id.quizImageLibre);
        textviewpuntuacion = findViewById(R.id.textviewpuntuacion);
        textviewcontadorpreguntas = findViewById(R.id.textviewcontadorpreguntas);
        textviewaciertos = findViewById(R.id.textviewaciertos);
        textviewtiempo = findViewById(R.id.textviewtiempo);
        textviewpregunta = findViewById(R.id.textviewpregunta);
        radio_group = findViewById(R.id.radio_group);
        radio_button1 = findViewById(R.id.radio_button1);
        radio_button2 = findViewById(R.id.radio_button2);
        radio_button3 = findViewById(R.id.radio_button3);
        buttonconfirmar = findViewById(R.id.buttonconfirmar);
        textViewExplanation = findViewById(R.id.textViewExplanation);
    }

    private void showProgressDialog() {
        // Inflate and set up the custom progress dialog layout
        progressDialog = new Dialog(this);
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // Hide the title bar
        progressDialog.setContentView(R.layout.progress_dialog);      // Set the custom layout
        progressDialog.setCancelable(false);                          // Prevent dismissing by tapping outside

        // Show the dialog
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void checkForLocalQuestions() {
        List<JsonQuestion> savedQuestions = prefHelper.getQuestions();

        if (savedQuestions == null || savedQuestions.isEmpty()) {
            Log.d("batchFlow", "No saved questions found in SharedPreferences. Fetching from Firestore...");
            fetchQuestionsFromFirestore();
        } else {
            Log.d("batchFlow", "Questions loaded from SharedPreferences. Size: " + savedQuestions.size());
            questionList = savedQuestions;

            if (batchManager.getTotalBatches() == 0) {
                prepareBatches(this::presentInitialBatch);
            } else {
                totalBatches = batchManager.getTotalBatches();
                presentInitialBatch();
            }
        }
    }

    private void fetchQuestionsFromFirestore() {
        showProgressDialog();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("SINGLEMODEQUESTIONS").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    JsonQuestion question = document.toObject(JsonQuestion.class);
                    //  LOG QUESTION DATA (including explanation)
                    Log.d("Firestore", "Question: " + question.getQuestion() +
                            " | Explanation: " + question.getExplanation());
                    questionList.add(question);
                }

                // Shuffle the question list to ensure randomness
                Collections.shuffle(questionList);

                // Save questions to preferences and prepare batches only once
                prefHelper.saveQuestions(questionList);

                if (batchManager.getTotalBatches() == 0) {
                    dismissProgressDialog();
                    prepareBatches(this::presentInitialBatch);
                } else {
                    dismissProgressDialog();
                    presentInitialBatch();
                }
            } else {
                dismissProgressDialog();
                Log.e("batchFlow", "Failed to fetch questions: ", task.getException());
                Toast.makeText(this, "Error loading questions or no questions available.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void prepareBatches(Runnable completionCallback) {
        totalBatches = (int) Math.ceil((double) questionList.size() / QUESTIONS_PER_BATCH);

        // Initialize questionBatches map
        questionBatches.clear();
        for (int i = 0; i < totalBatches; i++) {
            int startIndex = i * QUESTIONS_PER_BATCH;
            int endIndex = Math.min(startIndex + QUESTIONS_PER_BATCH, questionList.size());

            List<JsonQuestion> batchQuestions = questionList.subList(startIndex, endIndex);
            questionBatches.put(i, new ArrayList<>(batchQuestions));
        }

        // Save total batches and question batches to the batch manager
        batchManager.saveTotalBatches(totalBatches, questionBatches);

        // Shuffle batch indices for random order presentation
        shuffledBatchOrder.clear();
        for (int i = 0; i < totalBatches; i++) {
            shuffledBatchOrder.add(i);
        }
        Collections.shuffle(shuffledBatchOrder);

        Log.d("batchFlow", "Batches prepared. Total number of batches: " + totalBatches);

        // Log the shuffled batches
      //  logShuffledBatches(questionBatches);

        if (completionCallback != null) {
            completionCallback.run();
        }
    }

    private void loadNextAvailableBatch() {
        int nextBatchIndex = batchManager.getNextAvailableBatchIndex();

        if (nextBatchIndex != -1) {
            displayBatch(nextBatchIndex); // Present questions for this batch
        } else {
            // All batches completed - navigate to ResultActivity
            navigateToResultActivityWithCompletion();
        }
    }

    private void displayBatch(int batchIndex) {
        currentBatch = getQuestionsForBatch(batchIndex);
        Log.d("batchFlow", "Displaying batch " + (batchIndex + 1) + " with " + (currentBatch != null ? currentBatch.size() : 0) + " questions.");


        if (currentBatch != null && currentBatch.size() == QUESTIONS_PER_BATCH) {
            questionCounter = 0;
            this.batchIndex = batchIndex;
            showNextQuestion();
        } else {
            Log.e("batchFlow", "Batch " + batchIndex + " has an incomplete set of questions.");
            // Handle the error, maybe retry or log and proceed
        }
    }

    private List<JsonQuestion> getQuestionsForBatch(int batchIndex) {
        int startIndex = batchIndex * QUESTIONS_PER_BATCH;
        int endIndex = Math.min(startIndex + QUESTIONS_PER_BATCH, questionList.size());
        List<JsonQuestion> batch = new ArrayList<>(questionList.subList(startIndex, endIndex));
        Log.d("batchFlow", "Retrieved batch " + (batchIndex + 1) + " with " + batch.size() + " questions.");


        if (batch.size() < QUESTIONS_PER_BATCH) {
            Log.w("batchFlow", "Batch " + (batchIndex + 1) + " has only " + batch.size() + " questions instead of 10.");
        }
        return batch;
    }

    private void presentInitialBatch() {
        // Check if all batches are used before attempting to load a new one
        if (batchManager.allBatchesCompleted()) {
            Log.d("batchFlow", "All batches completed. Showing completion dialog.");
            navigateToResultActivityWithCompletion(); // Send to results to confirm completion
        } else {
            loadNextAvailableBatch(); // Proceed normally if there are batches left
        }
    }

    private void navigateToResultActivityWithCompletion() {
        Intent intent = new Intent(PreguntasModoLibre2.this, ResultActivity.class);
        intent.putExtra("all_batches_completed", true); // Notify result activity of completion
        intent.putExtra("correct_answers", score);
        intent.putExtra("errors", errores);
        intent.putExtra("scoretotal", scoretotal);
        startActivity(intent);
        finish();
    }

    private void setupRadioButtonListeners() {
        radio_group.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) { // ✅ Start effect only after a selection is made
                Log.d("RadioGroup", "Selection made, starting vibration reminder.");

                // ✅ Reset all radio buttons to default appearance
                resetRadioButtonColors();

                // ✅ Highlight selected option
                RadioButton selectedRadioButton = findViewById(checkedId); // ✅ Declare variable here
                if (selectedRadioButton != null) {
                    selectedRadioButton.setBackgroundResource(R.drawable.radio_normal2);
                }

                // ✅ Stop any previous reminders before starting a new one
                stopVibrationReminder();

                // ✅ Start the reminder effect
                startVibrationReminder();
            }
        });
    }

    private void startCountDown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(COUNTDOWN_IN_MILLIS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountdownText();
                if (millisUntilFinished <= 5000) {
                    playCountdownSound();
                }
            }

            @Override
            public void onFinish() {
                timeLeftInMillis = 0;
                updateCountdownText();

                answered = true;

                // Create bold text for "TOO SLOW. TIME IS UP"
                // Create bold and red text for "TOO SLOW. TIME IS UP"
                SpannableString timeoutText = new SpannableString("TOO SLOW. TIME IS UP");

// Apply bold styling
                timeoutText.setSpan(new StyleSpan(Typeface.BOLD), 0, timeoutText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

// Apply red color to the text
                int redColor = getResources().getColor(R.color.incorrect_answer_background); // Replace customRed with your defined red color in colors.xml
                timeoutText.setSpan(new ForegroundColorSpan(redColor), 0, timeoutText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

// Set the formatted text to textviewpregunta
                textviewpregunta.setText(timeoutText);

// Play the incorrect answer sound
                playIncorrectAnswerSound();
                // Increment errors
                errores++;

                // Update the UI to reflect the timeout result
                textviewaciertos.setText("CORRECT ANSWERS: " + score);
                textviewpuntuacion.setText("SCORE: " + scoretotal);

                // Reset the confirm button for the next question or finish the quiz
                buttonconfirmar.setText(questionCounter < QUESTIONS_PER_BATCH ? "NEXT" : "FINISH");
                buttonconfirmar.setVisibility(View.VISIBLE);

            }
        }.start();
    }

    private void playCountdownSound() {
        if (mediaPlayerCountdown != null) {
            mediaPlayerCountdown.release();
            mediaPlayerCountdown = null;
        }

        mediaPlayerCountdown = MediaPlayer.create(this, R.raw.countdown);

        if (mediaPlayerCountdown != null) {
            try {
                mediaPlayerCountdown.start();
                mediaPlayerCountdown.setOnCompletionListener(mp -> {
                    mp.release();
                    mediaPlayerCountdown = null;
                });
            } catch (IllegalStateException e) {
                Log.e("mediaPlayer", "Failed to start countdown sound", e);
            }
        }
    }

    private void updateCountdownText() {
        int seconds = (int) (timeLeftInMillis / 1000);
        textviewtiempo.setText(String.format(Locale.getDefault(), "%02d", seconds));
        textviewtiempo.setTextColor(seconds <= 5 ? ContextCompat.getColor(this, R.color.incorrect_answer_background) : ContextCompat.getColor(this, R.color.white));
    }

    private void resetFeedbackTextColor() {
        textviewpregunta.setTextColor(ContextCompat.getColor(this, R.color.black)); // Resets to default black
    }
    private void flipImage(ImageView quizImageLibre) {
        // Create ObjectAnimator to flip the image vertically
        ObjectAnimator flipOut = ObjectAnimator.ofFloat(quizImageLibre, "rotationX", 0f, 90f);
        ObjectAnimator flipIn = ObjectAnimator.ofFloat(quizImageLibre, "rotationX", -90f, 0f);

        // Set the duration for each flip
        flipOut.setDuration(300);
        flipIn.setDuration(300);

        // Set an AnimatorListener to update the image at the midpoint
        flipOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // You can update the image resource here if needed
                quizImageLibre.setRotationX(90f); // Set the rotation halfway
            }
        });

        // Combine the animations in sequence
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(flipOut, flipIn);
        animatorSet.start();
    }
    private void updateQuestionCounterUI() {
        // Add 1 to questionCounter to display "1/10" for the first question
        textviewcontadorpreguntas.setText("QUESTION " + (questionCounter + 1) + "/" + QUESTIONS_PER_BATCH);
    }

    private void handleNewBatchLoad() {
        questionCounter = 0; // Start from question 1 instead of 0
        batchIndex = 0;
        checkForLocalQuestions(); // Only loads questions if none are available in SharedPreferences
        presentInitialBatch();
    }

    private void updateUIWithCurrentQuestion() {
        textviewpregunta.setText(currentQuestion.getQuestion());
        radio_button1.setText(currentQuestion.getOPTION_A());
        radio_button2.setText(currentQuestion.getOPTION_B());
        radio_button3.setText(currentQuestion.getOPTION_C());
        radio_group.clearCheck();
    }

    private void showNextQuestion() {
        RadioGroup radioGroup = findViewById(R.id.radio_group);
        TextView textViewExplanation = findViewById(R.id.textViewExplanation);
        Button buttonConfirmar = findViewById(R.id.buttonconfirmar);

        // Ensure smooth removal of explanation
        if (textViewExplanation.getVisibility() == View.VISIBLE) {
            Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    textViewExplanation.setVisibility(View.GONE);

                    // ✅ Move button immediately back under radioGroup
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) buttonConfirmar.getLayoutParams();
                    params.addRule(RelativeLayout.BELOW, R.id.radio_group);
                    buttonConfirmar.setLayoutParams(params);

                    // ✅ Restore radio group with a fade-in effect
                    radioGroup.setVisibility(View.VISIBLE);
                    Animation fadeIn = AnimationUtils.loadAnimation(PreguntasModoLibre2.this, R.anim.fade_in);
                    radioGroup.startAnimation(fadeIn);
                }

                @Override public void onAnimationStart(Animation animation) {}
                @Override public void onAnimationRepeat(Animation animation) {}
            });

            textViewExplanation.startAnimation(fadeOut);
        } else {
            // If no explanation was shown, directly reposition button and show radio buttons
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) buttonConfirmar.getLayoutParams();
            params.addRule(RelativeLayout.BELOW, R.id.radio_group);
            buttonConfirmar.setLayoutParams(params);
            radioGroup.setVisibility(View.VISIBLE);
        }

        // Proceed to the next question
        if (questionCounter < QUESTIONS_PER_BATCH) {
            currentQuestion = currentBatch.get(questionCounter);
            stopVibrationReminder();
            updateUIWithCurrentQuestion();
            updateQuestionCounterUI();
            resetFeedbackTextColor();
            resetRadioButtonColors();
            startCountDown();
            flipImage(quizImageLibre);
            answered = false;
            questionCounter++;
        } else {
            finishQuiz();
        }
    }

    private void checkAnswer() {
        answered = true;
        RadioButton selectedRadioButton = findViewById(radio_group.getCheckedRadioButtonId());

        SpannableString feedbackText; // Variable to hold styled text
        int feedbackColor; // Variable to hold the color for feedback

        if (selectedRadioButton != null && selectedRadioButton.getText().equals(currentQuestion.getAnswer())) {
            score++;
            scoretotal += 500;

            // Create bold text for "YOU'RE ABSOLUTELY RIGHT!"
            feedbackText = new SpannableString("YOU'RE ABSOLUTELY RIGHT!");
            feedbackText.setSpan(new StyleSpan(Typeface.BOLD), 0, feedbackText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            feedbackColor = ContextCompat.getColor(this, R.color.correct_answer_background);
            textviewpregunta.setText(feedbackText);
            playCorrectAnswerSound();
        } else {
            errores++;

            // Create bold text for "NOPE. THAT'S NOT IT"
            feedbackText = new SpannableString("NOPE. THAT'S NOT IT");
            feedbackText.setSpan(new StyleSpan(Typeface.BOLD), 0, feedbackText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            feedbackColor = ContextCompat.getColor(this, R.color.incorrect_answer_background); // Use red color for incorrect answers
            textviewpregunta.setText(feedbackText);
            playIncorrectAnswerSound();
        }

        // Set the feedback color
        textviewpregunta.setTextColor(feedbackColor);

        // Update score and correct answers
        textviewpuntuacion.setText("SCORE: " + scoretotal);
        textviewaciertos.setText("CORRECT ANSWERS: " + score);

        showExplanation();
    }

    private void showExplanation() {
        TextView textViewExplanation = findViewById(R.id.textViewExplanation);
        Button buttonConfirmar = findViewById(R.id.buttonconfirmar);
        RadioGroup radioGroup = findViewById(R.id.radio_group);

        if (currentQuestion.getExplanation() != null && !currentQuestion.getExplanation().isEmpty()) {
            // Fade out radio buttons smoothly
            Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    radioGroup.setVisibility(View.GONE);
                    textViewExplanation.setText(currentQuestion.getExplanation());

                    // Now fade in the explanation
                    textViewExplanation.setVisibility(View.VISIBLE);
                    Animation fadeIn = AnimationUtils.loadAnimation(PreguntasModoLibre2.this, R.anim.fade_in);
                    textViewExplanation.startAnimation(fadeIn);

                    // ✅ Delay repositioning button slightly to avoid overlap
                    new Handler().postDelayed(() -> {
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) buttonConfirmar.getLayoutParams();
                        params.addRule(RelativeLayout.BELOW, R.id.textViewExplanation);
                        buttonConfirmar.setLayoutParams(params);
                    }, 150); // 150ms delay
                }

                @Override
                public void onAnimationStart(Animation animation) {}
                @Override
                public void onAnimationRepeat(Animation animation) {}
            });

            radioGroup.startAnimation(fadeOut);
        }
    }
    private void playCorrectAnswerSound() {
        if (mediaPlayerRight == null) {
            mediaPlayerRight = MediaPlayer.create(this, R.raw.right);
        }
        mediaPlayerRight.start();
    }

    private void playIncorrectAnswerSound() {
        if (mediaPlayerNotRight == null) {
            mediaPlayerNotRight = MediaPlayer.create(this, R.raw.notright);
        }
        mediaPlayerNotRight.start();
    }

    private void resetRadioButtonColors() {
        // Set the background resource to your drawable for all radio buttons
        radio_button1.setButtonDrawable(R.drawable.radio_selector);
        radio_button2.setButtonDrawable(R.drawable.radio_selector);
        radio_button3.setButtonDrawable(R.drawable.radio_selector);

    }


    private void finishQuiz() {
        // Mark the current batch as completed only after all questions in it have been answered
        batchManager.markBatchAsCompleted(batchIndex);

        // Log current batch status
        Log.d("finishQuiz", "Marking batch as completed. Current batch index: " + batchIndex);
        Log.d("finishQuiz", "All batches completed: " + batchManager.allBatchesCompleted());

        // Log current batch progress by counting marked completed batches
        Set<Integer> completedBatches = batchManager.getCompletedBatches();
        Log.d("finishQuiz", "Completed batches: " + completedBatches.size()
                + "/" + batchManager.getTotalBatches());

        Intent intent = new Intent(PreguntasModoLibre2.this, ResultActivity.class);
        intent.putExtra("all_batches_completed", batchManager.allBatchesCompleted());
        intent.putExtra("correct_answers", score);
        intent.putExtra("errors", errores);
        intent.putExtra("scoretotal", scoretotal);

        // Log scoring details
        Log.d("finishQuiz", "Correct answers: " + score);
        Log.d("finishQuiz", "Errors: " + errores);
        Log.d("finishQuiz", "Total score: " + scoretotal);

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mediaPlayerCountdown != null) {
            mediaPlayerCountdown.stop();
            mediaPlayerCountdown.release();
            mediaPlayerCountdown = null;
        }

        if (mediaPlayerRight != null) {
            mediaPlayerRight.stop();
            mediaPlayerRight.release();
            mediaPlayerRight = null;
        }

        if (mediaPlayerNotRight != null) {
            mediaPlayerNotRight.stop();
            mediaPlayerNotRight.release();
            mediaPlayerNotRight = null;
        }
    }
}
