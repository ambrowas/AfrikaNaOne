package com.iniciativaselebi.afrikanaone;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
                    prepareBatches(this::presentInitialBatch);
                } else {
                    presentInitialBatch();
                }
            } else {
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
        logShuffledBatches(questionBatches);

        if (completionCallback != null) {
            completionCallback.run();
        }
    }

    private void logShuffledBatches(Map<Integer, List<JsonQuestion>> questionBatches) {
        for (int batchIndex : shuffledBatchOrder) {
            List<JsonQuestion> batch = questionBatches.get(batchIndex);
            StringBuilder batchLog = new StringBuilder("Batch " + (batchIndex + 1) + ": ");
            for (JsonQuestion question : batch) {
                batchLog.append("Question Number: ").append(question.getNumber())
                        .append(" - ").append(question.getQuestion()).append("; ");
            }
            Log.d("batchFlow", batchLog.toString());
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
                    Toast.makeText(PreguntasModoLibre2.this, "Fear not. Make a choice.", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (questionCounter < QUESTIONS_PER_BATCH) {
                    buttonconfirmar.setText("CONFIRM");
                    showNextQuestion();
                } else {
                    finishQuiz();
                }
            }
        });
    }

    private void setupRadioButtonListeners() {
        radio_group.setOnCheckedChangeListener((group, checkedId) -> {
            for (int i = 0; i < group.getChildCount(); i++) {
                RadioButton radioButton = (RadioButton) group.getChildAt(i);
                radioButton.setBackgroundResource(radioButton.getId() == checkedId ? R.drawable.radio_selected : R.drawable.blackbackground);
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
                checkAnswer();
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
          //  Log.d("batchFlow", "Presenting question " + (questionCounter + 1) + " of batch " + (batchIndex + 1) + ": " + currentQuestion.getQuestion());


            updateUIWithCurrentQuestion();
            updateQuestionCounterUI();
            resetRadioButtonColors();
            startCountDown();
            answered = false;

            questionCounter++;  // Increment only after showing the question
        } else {
            Log.d("batchFlow", "Completed current batch. Moving to the next batch.");
            finishQuiz();  // Complete the quiz for the batch and proceed
        }
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
        if (selectedRadioButton != null && selectedRadioButton.getText().equals(currentQuestion.getAnswer())) {
            score++;
            scoretotal += 500;
            textviewpregunta.setText("YOU'RE ABSOLUTELY RIGHT!");
            playCorrectAnswerSound();
        } else {
            errores++;
            textviewpregunta.setText("NOPE. THAT'S NOT IT");
            playIncorrectAnswerSound();
        }
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
        radio_button1.setBackgroundResource(R.drawable.blackbackground);
        radio_button2.setBackgroundResource(R.drawable.blackbackground);
        radio_button3.setBackgroundResource(R.drawable.blackbackground);
    }

    private void finishQuiz() {
        // Mark the current batch as completed only after all questions in it have been answered
        batchManager.markBatchAsCompleted(batchIndex);

        Intent intent = new Intent(PreguntasModoLibre2.this, ResultActivity.class);
        intent.putExtra("all_batches_completed", batchManager.allBatchesCompleted());
        intent.putExtra("correct_answers", score);
        intent.putExtra("errors", errores);
        intent.putExtra("scoretotal", scoretotal);
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
