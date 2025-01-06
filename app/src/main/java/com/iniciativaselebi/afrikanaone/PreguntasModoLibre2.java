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
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
    private TextView textviewpuntuacion, textviewcontadorpreguntas, textviewaciertos, textviewtiempo, textviewpregunta;

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

    private void setupButtonListeners() {
        buttonconfirmar.setOnClickListener(view -> {
            if (!answered) {
                if (radio_button1.isChecked() || radio_button2.isChecked() || radio_button3.isChecked()) {
                    countDownTimer.cancel();
                    checkAnswer();
                    buttonconfirmar.setText(questionCounter < QUESTIONS_PER_BATCH ? "NEXT" : "FINISH");

                } else {
                    Sounds.playWarningSound(getApplicationContext());
                    Toast.makeText(PreguntasModoLibre2.this, "FEAR NOT. MAKE A CHOICE.", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (questionCounter < QUESTIONS_PER_BATCH) {
                    buttonconfirmar.setText("CONFIRM");
                    Sounds.playSwooshSound(getApplicationContext());
                    showNextQuestion();
                } else {
                    Sounds.playSwooshSound(getApplicationContext());
                    finishQuiz();
                }
            }
        });
    }

    private void setupRadioButtonListeners() {
        radio_group.setOnCheckedChangeListener((group, checkedId) -> {
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof RadioButton) {
                    RadioButton radioButton = (RadioButton) child;
                    if (radioButton.getId() == checkedId) {
                        // Apply the selected state
                        radioButton.setBackgroundResource(R.drawable.radio_normal2);
                    } else {
                        // Reset to default state
                        radioButton.setBackgroundResource(R.drawable.radio_selector);
                    }
                }
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
                int redColor = getResources().getColor(R.color.red); // Replace customRed with your defined red color in colors.xml
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
        textviewtiempo.setTextColor(seconds <= 5 ? ContextCompat.getColor(this, R.color.red) : ContextCompat.getColor(this, R.color.white));
    }

    private void showNextQuestion() {
        if (questionCounter < QUESTIONS_PER_BATCH) {  // Since questionCounter starts at 0, use < instead of <=
            currentQuestion = currentBatch.get(questionCounter);  // Directly use questionCounter as the index
            Log.d("batchFlow", "Presenting question number: " + currentQuestion.getNumber() + " from batch number: " + (batchIndex + 1));
            Log.d("questionFlow", "Current QuestionCounter: " + (questionCounter + 1) + " / Total Batch Questions: " + QUESTIONS_PER_BATCH);
            // Log.d("batchFlow", "Presenting question " + (questionCounter + 1) + " of batch " + (batchIndex + 1) + ": " + currentQuestion.getQuestion());

            resetFeedbackTextColor(); // Reset text color to black before showing the next question

            updateUIWithCurrentQuestion();
            updateQuestionCounterUI();
            resetRadioButtonColors();
            startCountDown();
            flipImage(quizImageLibre);
            answered = false;

            questionCounter++;  // Increment only after showing the question
        } else {
            Log.d("batchFlow", "Completed current batch. Moving to the next batch.");
            finishQuiz();  // Complete the quiz for the batch and proceed
        }
    }

    // Add a helper method to reset the text color
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

            feedbackColor = ContextCompat.getColor(this, R.color.red); // Use red color for incorrect answers
            textviewpregunta.setText(feedbackText);
            playIncorrectAnswerSound();
        }

        // Set the feedback color
        textviewpregunta.setTextColor(feedbackColor);

        // Update score and correct answers
        textviewpuntuacion.setText("SCORE: " + scoretotal);
        textviewaciertos.setText("CORRECT ANSWERS: " + score);
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
