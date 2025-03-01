package com.iniciativaselebi.afrikanaone;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


    public class Preguntas extends AppCompatActivity {
            private static final int BATCH_SIZE = 50;
            private ImageView quizImage, resultImage;
            private TextView textviewpregunta, textviewcategoria, textviewaciertos, textviewpuntuacion, textviewfallos, textviewtiempo;
            private RadioButton radio_button1, radio_button2, radio_button3;
            private Button buttonconfirmar, buttonsiguiente, buttonterminar;
            private RadioGroup radio_group;
            RadioButton selectedRadioButton;
            private FirebaseFirestore db;
            private List<Map<String, Object>> preguntasList;
            private QuestionModoCompeticion currentQuestion;
            private int currentQuestionIndex;
            private int score, correctAnswers, incorrectAnswers;
            private boolean isSolutionDisplayed;
            private CountDownTimer timer;
            private MediaPlayer mediaPlayer;
            private boolean doubleClick = false;
            private static final int DOUBLE_CLICK_DELAY = 2000; // Delay in milliseconds
            private boolean isBackPressed = false;
            private Handler backButtonHandler;
            int currentGameAciertos;
            int currentGameFallos;
            int currentGamePuntuacion;
            private FirebaseAuth mAuth;
            String userId;
            private DatabaseReference gameStatsRef;
            private boolean isProcessing = false;
            boolean hasShownToast = false;
            private boolean continueAnimation = true;
            private QuestionDataSource questionDataSource;
            private List<QuestionModoCompeticion> unusedQuestions;
            private FirestoreQuestionManager firestoreQuestionManager;
            private static final String SHARED_PREF_NAME = "UsedQuestionsSharedPreferences";
            private static final String USED_QUESTIONS_KEY = "usedQuestions";
            private SharedPreferences sharedPreferences;
            private boolean isFetching = false;
             private boolean isBatchPreparationStarted = false;
        private boolean timerWasActive = false;
        private boolean questionInProgress = false;
        private boolean isAlertDisplayed = false;
        private boolean isAppInBackground = false;

        MediaPlayer swooshPlayer;

        private int currentBatchId; // Class level field

        private WarningManager warningManager;
        private boolean gameOverTriggered = false;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_preguntas);

            warningManager = new WarningManager();

            swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);

            timerWasActive = false;
            questionInProgress = false;
            incorrectAnswers = 0;

            initializeUIElements();

            FirebaseApp.initializeApp(this);
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            mAuth = FirebaseAuth.getInstance();
            firestoreQuestionManager = new FirestoreQuestionManager(this);

            userId = mAuth.getCurrentUser().getUid(); // Replace this with the actual user ID if you have a different way to get it
            DatabaseReference userRef = database.getReference("user").child(userId);
            gameStatsRef = database.getReference("user").child(userId);


            currentGameAciertos = 0;
            currentGameFallos = 0;
            currentGamePuntuacion = 0;

            db = FirebaseFirestore.getInstance();
            questionDataSource = new QuestionDataSource(this);
            questionDataSource.open();
            resetTextView();
            moveToNextQuestion();


            buttonconfirmar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isSolutionDisplayed) {
                        // Check if an answer is selected
                        int selectedRadioButtonId = radio_group.getCheckedRadioButtonId();
                        if (selectedRadioButtonId == -1) {
                            // Show the "Fear Not, Make a Choice" dialog with sound effects
                            showCustomDialogWithSounds(
                                    "ATTENTION",
                                    "Fear not, make a choice",
                                    null // No action required on dismissal
                            );
                            return; // Prevent further execution if no answer is selected
                        }

                        // Cancel the timer if it's running
                        if (timer != null) {
                            timer.cancel();
                        }

                        // Update the appearance of the selected RadioButton
                        RadioButton selectedRadioButton = findViewById(selectedRadioButtonId);
                        if (selectedRadioButton != null) {
                            selectedRadioButton.setBackgroundResource(R.drawable.toast_background);
                        } else {
                            // Handle unexpected error: Show a warning dialog
                            Warning errorWarning = new Warning(
                                    Preguntas.this,
                                    1, // Low priority because this is unexpected but not critical
                                    "Error",
                                    "Unexpected error occurred. Please try again."
                            );
                            warningManager.addWarning(errorWarning);
                            return; // Prevent further execution on error
                        }

                        // Process the answer, show the solution, and update button visibility
                        processAnswer();
                        showSolution();
                        isSolutionDisplayed = true;
                        buttonconfirmar.setVisibility(View.GONE);
                        buttonsiguiente.setVisibility(View.VISIBLE);
                        buttonterminar.setVisibility(View.VISIBLE);
                    }
                }
            });


            buttonsiguiente.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playSwoosh();
                    if (isSolutionDisplayed) {

                        moveToNextQuestion();
                    }
                    isSolutionDisplayed = false;
                    adjustButtonVisibility();

                }


            });
        }

        public void moveToNextQuestion() {
            int unusedQuestionsCount = questionDataSource.getUnusedQuestionsCount();
            Log.d("QuestionFlow", "Checking unused questions count: " + unusedQuestionsCount);

            resetPenaltyState();
            displayNextUnusedQuestion();

            // If we are at the threshold to prepare the next batch, do so
            if (unusedQuestionsCount == 8) {
                Log.d("QuestionFlow", "Threshold for preparing next batch reached at count: " + unusedQuestionsCount);
                prepareNextBatch();
            }

            // If we have reached the count where we need to fetch the new questions, do so
            if (unusedQuestionsCount == 5) {
                Log.d("QuestionFlow", "Low unused questions count reached, count: " + unusedQuestionsCount + ". Fetching new batch.");
                fetchAndSaveNewQuestions();
            }
        }

        private void prepareNextBatch() {
            firestoreQuestionManager.prepareNextBatchIfRequired(userId, new FirestoreQuestionManager.BatchPreparationCallback() {
                @Override
                public void onBatchPreparationComplete() {
                    Log.d("QuestionFlow", "Next batch preparation complete.");
                    // Logic here if needed right after preparation
                }

                @Override
                public void onBatchPreparationFailed(Exception e) {
                    Log.e("QuestionFlow", "Failed to prepare next batch.", e);
                    // Handle batch preparation error
                }
            });
        }

        private void fetchAndSaveNewQuestions() {
            firestoreQuestionManager.fetchShuffledBatchQuestions(new FirestoreQuestionManager.QuestionsFetchCallback() {
                @Override
                public void onSuccess(List<QuestionModoCompeticion> fetchedQuestions) {

                }

                @Override
                public void onQuestionsFetched(List<QuestionModoCompeticion> fetchedQuestions) {
                    // Assuming currentBatchId is accessible here and represents the batch that the questions belong to
                    int completedBatchId = currentBatchId; // currentBatchId should be the ID of the current batch
                    saveFetchedQuestionsToLocalDatabase(fetchedQuestions, completedBatchId);
                }

                @Override
                public void onQuestionsUpdated(List<QuestionModoCompeticion> updatedQuestions) {
                    // This method might not be used here if it's intended for after the questions have been saved to the database
                    // If so, you could leave this implementation empty, or consider removing this method from the callback
                    // if it's not necessary for the interface contract in all use cases.
                }

                @Override
                public void onError(Exception e) {
                    Log.e("QuestionFlow", "Error fetching new batch of questions", e);
                    // Handle the fetch error
                }
            });
        }

        private void saveFetchedQuestionsToLocalDatabase(List<QuestionModoCompeticion> fetchedQuestions, int completedBatchId) {
            firestoreQuestionManager.updateLocalDatabaseWithNewQuestions(fetchedQuestions, completedBatchId, new FirestoreQuestionManager.QuestionsFetchCallback() {
                @Override
                public void onQuestionsUpdated(List<QuestionModoCompeticion> questions) {
                    Log.d("QuestionFlow", "Local database updated with new batch of questions.");
                    // Now that the questions are updated and the batch is considered complete,
                    // you can mark the batch as completed here if needed.
                }

                @Override
                public void onSuccess(List<QuestionModoCompeticion> fetchedQuestions) {
                    // onSuccess is not used in this context, you might consider removing it from the interface
                    // if it doesn't serve any purpose elsewhere.
                }

                @Override
                public void onQuestionsFetched(List<QuestionModoCompeticion> newQuestions) {
                    // This method is not used here, but it's required by the interface. Consider if it's needed at all.
                }

                @Override
                public void onError(Exception e) {
                    Log.e("QuestionFlow", "Failed to update local database with new questions.", e);
                    // Handle the error
                }
            });
        }
        private void displayNextUnusedQuestion() {
            List<QuestionModoCompeticion> questions = questionDataSource.fetchRandomUnusedQuestions(1);
            if (!questions.isEmpty()) {
                currentQuestion = questions.get(0);
                Log.d("QuestionFlow", "Displaying next unused question.");
                displayQuestion();
                questionDataSource.markQuestionAsUsed(currentQuestion.getNUMBER());
            } else {
                Log.e("QuestionFlow", "No unused questions available. This should not happen if the previous checks are correct.");
                // Handle the scenario when no unused questions are available
            }
        }

        private void displayQuestion() {
            resetImageAndAnimation();

            if (currentQuestion == null) {
                Log.e("DisplayQuestionError", "No current question to display!");
                // Optional: Show a user-friendly error message or load a fallback question.
                return;
            }

            questionInProgress = true;

            // Update UI with question data
            textviewpregunta.setText(currentQuestion.getQUESTION());
            radio_button1.setText(currentQuestion.getOPTION_A());
            radio_button2.setText(currentQuestion.getOPTION_B());
            radio_button3.setText(currentQuestion.getOPTION_C());
            textviewcategoria.setText(currentQuestion.getCATEGORY());

            // Load Image (Use dynamic URL from question)
            String gsUrl = currentQuestion.getIMAGE(); // Ensure the question object provides a valid image URL
            if (gsUrl != null && !gsUrl.isEmpty()) {
                displayQuestionImage(gsUrl, quizImage);
            } else {
                Log.w("DisplayQuestion", "Image URL is empty or null. Setting default image.");
                quizImage.setImageResource(R.drawable.afrikanaonelogo); // Show placeholder image if URL is missing
            }

            // Reset Radio Buttons
            radio_group.clearCheck();
            resetRadioButtonColors();
            for (int i = 0; i < radio_group.getChildCount(); i++) {
                radio_group.getChildAt(i).setEnabled(true);
            }

            // Timer Setup
            if (timer != null) {
                timer.cancel();
            }
            timerWasActive = true; // Timer is about to start
            timer = new CountDownTimer(16000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int seconds = (int) (millisUntilFinished / 1000) % 60;
                    String timeLeftFormatted = (seconds < 10)
                            ? String.format(Locale.getDefault(), "0%d", seconds)
                            : String.format(Locale.getDefault(), "%d", seconds);
                    textviewtiempo.setText(timeLeftFormatted);

                    if (seconds <= 10) {
                        textviewtiempo.setTextColor(ContextCompat.getColor(Preguntas.this, R.color.toastBackgroundColor));
                    } else {
                        textviewtiempo.setTextColor(Color.BLACK);
                    }

                    if (seconds <= 5) {
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                        }
                        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.countdown);
                        mediaPlayer.start();
                    }
                }
                @Override
                public void onFinish() {
                    timerWasActive = false;

                    // Show "Time's Up!" dialog with warning sound
                    showCustomDialogWithSounds(
                            "TIME'S UP!",
                            "You need to make up your mind faster!",
                            () -> {
                                // Apply penalty after dialog is dismissed
                                //applyPenalty();

                                // Process answer and show solution after the penalty is applied
                                processAnswer();
                                showSolution();
                                buttonconfirmar.setVisibility(View.GONE);
                                buttonsiguiente.setVisibility(View.VISIBLE);
                                buttonterminar.setVisibility(View.VISIBLE);
                            }
                    );
                }
            }.start();
        }

        private void displayQuestionImage(String gsUrl, ImageView quizImageView) {
            // Validate the URL
            if (gsUrl == null || gsUrl.isEmpty()) {
                Log.e("ImageLoading", "Invalid or missing Firebase Storage URL");
                quizImageView.setImageResource(R.drawable.afrikanaonelogo); // Placeholder image
                return;
            }

            try {
                // Get reference from Firebase Storage
                StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(gsUrl);

                // Fetch the download URL
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    Log.d("ImageLoading", "Download URL retrieved: " + imageUrl);

                    // Load the image with Glide
                    Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.afrikanaonelogo) // Placeholder while loading
                            .error(R.drawable.afrikanaonelogo) // Fallback image on error
                            .into(quizImageView);
                }).addOnFailureListener(e -> {
                    Log.e("ImageLoading", "Failed to fetch image from Firebase Storage: " + e.getMessage());
                    quizImageView.setImageResource(R.drawable.afrikanaonelogo); // Fallback image
                });
            } catch (IllegalArgumentException e) {
                Log.e("ImageLoading", "Invalid Firebase Storage URL format: " + e.getMessage());
                quizImageView.setImageResource(R.drawable.afrikanaonelogo); // Fallback image
            } catch (Exception e) {
                Log.e("ImageLoading", "Unexpected error during image loading: " + e.getMessage());
                quizImageView.setImageResource(R.drawable.afrikanaonelogo); // Fallback image
            }
        }

        private void processAnswer() {
            if (currentQuestion == null) {
                Log.d("DebugFlow", "currentQuestion is null in processAnswer(). Moving to next question.");
                moveToNextQuestion();
                return;
            }

            int selectedRadioButtonId = radio_group.getCheckedRadioButtonId();
            if (selectedRadioButtonId == -1) {
                // No answer selected - apply penalty
                applyPenalty();
                return;
            }

            RadioButton selectedRadioButton = findViewById(selectedRadioButtonId);
            if (selectedRadioButton != null) {
                String selectedOption = selectedRadioButton.getText().toString();
                if (selectedOption.equals(currentQuestion.getANSWER())) {
                    correctAnswers++;
                    currentGameAciertos++;
                    MediaPlayer mediaPlayer1 = MediaPlayer.create(getApplicationContext(), R.raw.right);
                    mediaPlayer1.start();
                    score += 500;
                } else {
                    incorrectAnswers++;
                    currentGameFallos++;
                    MediaPlayer mediaPlayer2 = MediaPlayer.create(getApplicationContext(), R.raw.notright);
                    mediaPlayer2.start();
                    score -= 500;
                }
            }

            // Mark the question as used and update UI
            currentQuestion.setUsed(true);
            questionDataSource.markQuestionAsUsed(currentQuestion.getNUMBER());
            questionInProgress = false;

            textviewfallos.setText("INCORRECT ANSWERS: " + incorrectAnswers);
            textviewaciertos.setText("CORRECT ANSWERS: " + correctAnswers);
            textviewpuntuacion.setText("SCORE: " + score);

            if (incorrectAnswers >= 4) {
                textviewfallos.setTextColor(Color.RED);
            } else {
                textviewfallos.setTextColor(Color.BLACK);
            }

            disableOptionButtons();

            // Handle 4 or 5 mistake alerts
            if (incorrectAnswers == 4 && !hasShownToast) {
                hasShownToast = true;
                showFourErrorsAlert();
                return;
            }

            if (incorrectAnswers >= 5) {
                showFiveErrorsWarning();
                return;
            }

            adjustButtonVisibilityForNext();
        }

        private void disableOptionButtons() {
            for (int i = 0; i < radio_group.getChildCount(); i++) {
                radio_group.getChildAt(i).setEnabled(false);
            }
        }

        private void adjustButtonVisibilityForNext() {
            buttonconfirmar.setVisibility(View.GONE); // Hide "Confirm"
            buttonsiguiente.setVisibility(View.VISIBLE); // Show "Next"
            buttonterminar.setVisibility(View.VISIBLE); // Show "Finish"
        }

        private void showCustomDialog(String title, String message, DialogInterface.OnClickListener positiveClickListener) {
            Sounds.playWarningSound(getApplicationContext());

            AlertDialog.Builder builder = new AlertDialog.Builder(Preguntas.this, R.style.CustomAlertDialogTheme); // ✅ Apply custom theme
            builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", positiveClickListener)
                    .setIcon(R.drawable.afrikanaonelogo);

            AlertDialog dialog = builder.create();

            // ✅ Apply custom background
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
            }

            dialog.show();

            // ✅ Set button text color manually
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(Color.WHITE);
            }
        }

        private void showCustomDialog(String title, String message) {
            showCustomDialog(title, message, null);
        }
        private void playSwoosh() {
            if (swooshPlayer != null) {
                swooshPlayer.seekTo(0);
                swooshPlayer.start();
            }
        }

        private void showSolution() {
                int selectedRadioButtonId = radio_group.getCheckedRadioButtonId();
                RadioButton selectedRadioButton = findViewById(selectedRadioButtonId);
                String selectedOption = selectedRadioButton != null ? selectedRadioButton.getText().toString() : null;
                // Use the getter method to access the answer property
                String correctAnswer = currentQuestion.getANSWER();

                ImageView resultImage = findViewById(R.id.resultImage);

                if (selectedOption != null && selectedOption.equals(correctAnswer)) {
                    textviewpregunta.setText("CORRECT ANSWER");
                    textviewpregunta.setTextColor(getColor(R.color.selected_radio_button_background));
                    resultImage.setImageResource(R.drawable.baseline_check_24);
                    resultImage.setColorFilter(getColor(R.color.selected_radio_button_background));
                } else {
                    textviewpregunta.setText("INCORRECT ANSWER");
                    textviewpregunta.setTextColor(getColor(R.color.toastBackgroundColor));
                    resultImage.setImageResource(R.drawable.baseline_clear_24);
                    resultImage.setColorFilter(getColor(R.color.toastBackgroundColor));
                }

                // Animation for image
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(resultImage, "scaleX", 0.5f, 1f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(resultImage, "scaleY", 0.5f, 1f);
                AnimatorSet scaleSet = new AnimatorSet();
                scaleSet.play(scaleX).with(scaleY);
                scaleSet.setDuration(500);
                scaleSet.start();

                resultImage.setVisibility(View.VISIBLE);  // Now show the image

                textviewpregunta.setTypeface(null, Typeface.BOLD);
                textviewpregunta.setGravity(Gravity.CENTER);

                isSolutionDisplayed = true;
                buttonconfirmar.setVisibility(View.GONE);
                buttonsiguiente.setVisibility(View.VISIBLE);
                buttonterminar.setVisibility(View.VISIBLE);
                resetTextView();
            }

        private void initializeUIElements() {
            quizImage = findViewById(R.id.quizImage);
            textviewpregunta = findViewById(R.id.textviewpregunta);
            textviewcategoria = findViewById(R.id.textviewcategoria);
            textviewaciertos = findViewById(R.id.textviewaciertos);
            textviewpuntuacion = findViewById(R.id.textviewpuntuacion);
            textviewfallos = findViewById(R.id.textviewfallos);
            textviewtiempo = findViewById(R.id.textviewtiempo);
            radio_button1 = findViewById(R.id.radio_button1);
            radio_button2 = findViewById(R.id.radio_button2);
            radio_button3 = findViewById(R.id.radio_button3);
            buttonconfirmar = findViewById(R.id.buttonconfirmar);
            radio_group = findViewById(R.id.radio_group);
            buttonsiguiente = findViewById(R.id.buttonsiguiente);
            resultImage = findViewById(R.id.resultImage);
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.grow_animation);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                    if (continueAnimation) {
                        resultImage.startAnimation(animation);  // Restart the animation only if flag is set
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });

            resultImage.startAnimation(animation);


            buttonterminar = findViewById(R.id.buttonterminar);
            buttonterminar.setVisibility(View.GONE);
            buttonterminar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isProcessing) {
                        return; // Do nothing if a click is being processed
                    }

                    // Show an AlertDialog when buttonterminar is pressed
                    // Show an AlertDialog when buttonterminar is pressed
                    Sounds.playWarningSound(getApplicationContext());
                    AlertDialog dialog = new AlertDialog.Builder(Preguntas.this, R.style.CustomAlertDialogTheme) // Apply custom theme
                            .setTitle("Confirm")
                            .setMessage("You sure you want to end the game?")
                            .setPositiveButton("Yes", (dialogInterface, which) -> {
                                // User clicked "Yes" button. Finish the game.
                                isProcessing = true;
                                finishGameStats();
                                isProcessing = false;
                            })
                            .setNegativeButton("No", null) // Null listener dismisses the dialog
                            .setIcon(R.drawable.afrikanaonelogo)
                            .create(); // Create the AlertDialog

// Apply background & button colors
                    setDialogBackground(dialog);
                    dialog.show();
                    setDialogButtonColors(dialog);
                }
            });


            radio_button1.setTextColor(Color.WHITE);
            radio_button2.setTextColor(Color.WHITE);
            radio_button3.setTextColor(Color.WHITE);

            radio_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    // Reset the background and text color of all the RadioButtons
                    radio_button1.setBackgroundResource(R.drawable.radio_selector);
                    radio_button1.setTextColor(Color.WHITE);
                    radio_button2.setBackgroundResource(R.drawable.radio_selector);
                    radio_button2.setTextColor(Color.WHITE);
                    radio_button3.setBackgroundResource(R.drawable.radio_selector);
                    radio_button3.setTextColor(Color.WHITE);

                    // Get the selected RadioButton
                    selectedRadioButton = findViewById(checkedId);

                    // Set the background of the selected RadioButton to radio_selector3, and text color to black, if not null
                    if (selectedRadioButton != null) {
                        selectedRadioButton.setBackgroundResource(R.drawable.toast_background);
                        selectedRadioButton.setTextColor(Color.WHITE);
                    }
                }
            });
        }

        private void resetImageAndAnimation() {
            ImageView resultImage = findViewById(R.id.resultImage);
            resultImage.setImageResource(0); // remove the previous image
            resultImage.animate().cancel();
            resultImage.setVisibility(View.INVISIBLE);
        }

        private void resetTextView() {
            textviewpregunta.setText("");
            textviewpregunta.setTextColor(getColor(R.color.black)); // replace with your default color
            textviewpregunta.setTypeface(null, Typeface.NORMAL);
            textviewpregunta.setGravity(Gravity.START);
        }

        private void adjustButtonVisibility() {
            if (isSolutionDisplayed) {
                buttonconfirmar.setVisibility(View.GONE);
                buttonsiguiente.setVisibility(View.VISIBLE);
                buttonterminar.setVisibility(View.VISIBLE);
            } else {
                buttonconfirmar.setVisibility(View.VISIBLE);
                buttonsiguiente.setVisibility(View.GONE);
                buttonterminar.setVisibility(View.GONE);
            }
        }

         private void resetRadioButtonColors() {
            radio_button1.setBackgroundResource(R.drawable.radio_selector);
            radio_button2.setBackgroundResource(R.drawable.radio_selector);
            radio_button3.setBackgroundResource(R.drawable.radio_selector);
        }

        private void updateAccumulatedValues(String userId, int newAciertos, int newFallos, int newPuntuacion) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    int updatedAciertos = newAciertos;
                    int updatedFallos = newFallos;
                    int updatedPuntuacion = newPuntuacion;

                    if (dataSnapshot.hasChild("accumulatedAciertos")) {
                        updatedAciertos += dataSnapshot.child("accumulatedAciertos").getValue(Integer.class);
                    }
                    if (dataSnapshot.hasChild("accumulatedFallos")) {
                        updatedFallos += dataSnapshot.child("accumulatedFallos").getValue(Integer.class);
                    }
                    if (dataSnapshot.hasChild("accumulatedPuntuacion")) {
                        updatedPuntuacion += dataSnapshot.child("accumulatedPuntuacion").getValue(Integer.class);
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("accumulatedAciertos", updatedAciertos);
                    updates.put("accumulatedFallos", updatedFallos);
                    updates.put("accumulatedPuntuacion", updatedPuntuacion);


                   userRef.updateChildren(updates).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                // The update was successful
                            //    Toast.makeText(Preguntas.this, "¡Valores acumulados actualizados!", Toast.LENGTH_SHORT).show();
                            } else {
                                // The update failed
                                Toast.makeText(Preguntas.this, "Something went wrong. Try again", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle error
                    Toast.makeText(Preguntas.this, "Something went wrong. Try again.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void updateCurrentGameValues(int aciertos, int fallos, int puntuacion) {
            // Create a map for the game statistics
            Map<String, Object> gameStats = new HashMap<>();
            gameStats.put("currentGameAciertos", aciertos);
            gameStats.put("currentGameFallos", fallos);
            gameStats.put("currentGamePuntuacion", puntuacion);

            // Create a timestamp for the current time in milliseconds since the Unix epoch
            long currentDateAndTime = System.currentTimeMillis();

            // Add the LastPlay timestamp to the map
            gameStats.put("LastPlay", currentDateAndTime);

            // Update the database
            gameStatsRef.updateChildren(gameStats).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (!task.isSuccessful()) {
                        Toast.makeText(Preguntas.this, "Something went wrong. Try again.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        private void finishGameTerminar() {
            updateAccumulatedValues(userId, correctAnswers, incorrectAnswers, score);
            updateCurrentGameValues(correctAnswers, incorrectAnswers, score);

            SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            Intent intent = new Intent(getApplicationContext(), GameOverActivity.class);
            intent.putExtra("aciertos", correctAnswers);
            intent.putExtra("puntuacion", score);
            intent.putExtra("errores", incorrectAnswers);
            Sounds.playSwooshSound(getApplicationContext());
            startActivity(intent);
            finish();
        }

        private void finishGameStats() {
            updateAccumulatedValues(userId, correctAnswers, incorrectAnswers, score);
            updateCurrentGameValues(correctAnswers, incorrectAnswers, score);

            SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            Intent intent = new Intent(getApplicationContext(), ClassficationActivity.class);
            intent.putExtra("aciertos", correctAnswers);
            intent.putExtra("puntuacion", score);
            intent.putExtra("errores", incorrectAnswers);
            Sounds.playSwooshSound(getApplicationContext());
            startActivity(intent);
            finish();
        }

        @Override
        public void onBackPressed() {
            super.onBackPressed();
            showExitConfirmation();
        }

        private void showExitConfirmation() {
            Sounds.playWarningSound(getApplicationContext());

            AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme) // ✅ Apply custom theme
                    .setTitle("Exit Confirmation")
                    .setMessage("You sure you want to exit?")
                    .setPositiveButton("Yes", (dialogInterface, which) -> finish())
                    .setNegativeButton("No", (dialogInterface, which) -> dialogInterface.dismiss())
                    .setIcon(R.drawable.afrikanaonelogo) // Set the icon here
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background); // ✅ Apply custom background
            }

            dialog.show(); // Display the dialog

            // ✅ Set button text color manually
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            if (positiveButton != null) {
                positiveButton.setTextColor(Color.WHITE);
            }
            if (negativeButton != null) {
                negativeButton.setTextColor(Color.WHITE);
            }
        }

        private Runnable backButtonRunnable = new Runnable() {
            @Override
            public void run() {
                isBackPressed = false;
            }
        };

        @Override
        protected void onDestroy() {
            super.onDestroy();

            // Existing code to handle backButtonHandler, timer, and mediaPlayer
            if (backButtonHandler != null) {
                backButtonHandler.removeCallbacks(backButtonRunnable);
            }
            if (timer != null) {
                timer.cancel();
            }
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            if (questionDataSource != null) {
                questionDataSource.close();
            }

            // Additional code to release the swooshPlayer
            if (swooshPlayer != null) {
                swooshPlayer.release();
                swooshPlayer = null;
            }
        }

        @Override
        protected void onPause() {
            super.onPause();

            isAppInBackground = true;
            Log.d("ActivityLifecycle", "onPause: App moved to background");

            // Log state variables
            Log.d("StateCheck", "questionInProgress: " + questionInProgress);
            Log.d("StateCheck", "timerWasActive: " + timerWasActive);
            Log.d("StateCheck", "incorrectAnswers: " + incorrectAnswers);
            Log.d("StateCheck", "isAlertDisplayed: " + isAlertDisplayed);

            // Flag penalty conditions if necessary
            if (questionInProgress && timerWasActive && incorrectAnswers < 5 && !isAlertDisplayed) {
                Log.d("ActivityLifecycle", "onPause: Penalty conditions met, flagging for onResume.");
                isAlertDisplayed = true; // Prevent duplicate dialogs
            } else {
                Log.d("ActivityLifecycle", "onPause: Penalty conditions not met.");
            }
        }
        @Override
        protected void onResume() {
            super.onResume();

            Log.d("ActivityLifecycle", "onResume: App returning to foreground");

            // Log state variables
            Log.d("StateCheck", "isAppInBackground: " + isAppInBackground);
            Log.d("StateCheck", "isAlertDisplayed: " + isAlertDisplayed);
            Log.d("StateCheck", "questionInProgress: " + questionInProgress);
            Log.d("StateCheck", "timerWasActive: " + timerWasActive);
            Log.d("StateCheck", "incorrectAnswers: " + incorrectAnswers);

            // Apply penalty only if flagged in onPause
            if (isAppInBackground && isAlertDisplayed && questionInProgress && timerWasActive) {
                if (incorrectAnswers < 5) {
                    Log.d("ActivityLifecycle", "onResume: Showing penalty dialog.");
                    showPenaltyDialog();
                } else {
                    Log.d("ActivityLifecycle", "onResume: Skipping penalty dialog due to game-over conditions.");
                }
            }

            isAppInBackground = false;
        }
        private void showFourErrorsAlert() {
            showCustomDialogWithSounds(
                    "ATTENTION",
                    "Four errors: One more and you're done!",
                    () -> {
                        // Ensure only "Next" and "Finish" buttons are visible
                        disableOptionButtons();
                        adjustButtonVisibilityForNext();

                        // Mark the question as "answered" and ready for next
                        isSolutionDisplayed = true;
                    }
            );
        }

        private void showFiveErrorsWarning() {
            showCustomDialogWithSounds(
                    "GAME OVER",
                    "5 errors; That's it, we are done here!",
                    this::finishGameTerminar // End the game after dismissal
            );
        }

        private void applyPenalty() {
            Log.d("PenaltyLogic", "Applying penalty.");

            // Play wrong answer sound
            MediaPlayer mediaPlayer2 = MediaPlayer.create(getApplicationContext(), R.raw.notright);
            mediaPlayer2.start();

            // Increment incorrect answers
            incorrectAnswers++;
            Log.d("PenaltyLogic", "Updated incorrectAnswers: " + incorrectAnswers);

            // Deduct points as part of the penalty
            score -= 500;
            Log.d("PenaltyLogic", "Points deducted. Updated score: " + score);

            // Update UI
            textviewfallos.setText("INCORRECT ANSWERS: " + incorrectAnswers);
            textviewpuntuacion.setText("SCORE: " + score);

            if (incorrectAnswers >= 4) {
                textviewfallos.setTextColor(Color.RED);
            }

            // Handle "4 Mistakes Alert"
            if (incorrectAnswers == 4 && !hasShownToast) {
                hasShownToast = true; // Ensure this alert is shown only once
                showFourErrorsAlert();
                return;
            }

            // Handle Game Over Logic
            if (incorrectAnswers >= 5) {
                showFiveErrorsWarning();
                return;
            }

            // Reset UI for the current question and enable NEXT/FINISH buttons
            resetTextView();
            resetRadioButtonColors();
            disableOptionButtons();
            adjustButtonVisibilityForNext(); // Show NEXT and FINISH buttons
            Log.d("PenaltyLogic", "Penalty applied, waiting for user action.");
        }

        private void showPenaltyDialog() {
            if (incorrectAnswers >= 5) {
                Log.d("PenaltyDialog", "Skipping penalty dialog due to game-over conditions.");
                return; // Skip penalty dialog if Game Over is imminent
            }

            if (timer != null) {
                timer.cancel();
                timerWasActive = false; // Mark the timer as inactive
            }

            showCustomDialogWithSounds(
                    "WARNING",
                    "Don't leave the app during an active question.",
                    () -> {
                        Log.d("PenaltyDialog", "Penalty dialog dismissed, applying penalty.");

                        // Play the wrong answer sound
                        MediaPlayer mediaPlayer2 = MediaPlayer.create(getApplicationContext(), R.raw.notright);
                        mediaPlayer2.start();

                        // Handle penalty as a wrong answer
                        handlePenaltyAsWrongAnswer();
                    }
            );
        }

        private void handlePenaltyAsWrongAnswer() {
            if (currentQuestion == null) {
                Log.e("PenaltyLogic", "No current question to process penalty.");
                return;
            }

            // Increment incorrect answers and deduct points
            incorrectAnswers++;
            score -= 500;
            Log.d("PenaltyLogic", "Penalty applied. Incorrect answers: " + incorrectAnswers + ", Score: " + score);

            // Update UI
            textviewfallos.setText("INCORRECT ANSWERS: " + incorrectAnswers);
            textviewpuntuacion.setText("SCORE: " + score);
            if (incorrectAnswers >= 4) {
                textviewfallos.setTextColor(Color.RED);
            }

            // Handle "4 Mistakes Alert"
            if (incorrectAnswers == 4 && !hasShownToast) {
                hasShownToast = true;
                showFourErrorsAlert();
                return;
            }

            // Handle Game Over if 5 mistakes
            if (incorrectAnswers >= 5) {
                showFiveErrorsWarning();
                return;
            }

            // Mark the question as used
            questionDataSource.markQuestionAsUsed(currentQuestion.getNUMBER());
            Log.d("PenaltyLogic", "Question marked as used: " + currentQuestion.getNUMBER());

            // Reset UI and update flags for the current question
            resetRadioButtonColors();
            disableOptionButtons();
            adjustButtonVisibilityForNext(); // Show NEXT and FINISH buttons
            isSolutionDisplayed = true; // Treat this as a "solution displayed"

            Log.d("PenaltyLogic", "Penalty applied, waiting for user action.");
        }

        private void resetPenaltyState() {
            if (incorrectAnswers == 0) { // Only reset when starting fresh
                hasShownToast = false;
            }
        }
        private void showCustomDialogWithSounds(String title, String message, Runnable onDismiss) {
            // Play the warning sound when the dialog appears
            Sounds.playWarningSound(getApplicationContext());

            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme) // Apply custom style
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", (dialog, which) -> {
                        // Play the transition sound when the dialog is dismissed
                        Sounds.playSwooshSound(getApplicationContext());

                        if (onDismiss != null) {
                            onDismiss.run(); // Perform the action on dismissal
                        }
                    })
                    .setIcon(R.drawable.afrikanaonelogo)
                    .setCancelable(false); // Ensure the dialog cannot be dismissed by clicking outside

            AlertDialog dialog = builder.create();

            // Apply custom background
            setDialogBackground(dialog);

            // Show the dialog
            dialog.show();

            // Ensure text and button colors are set correctly
            setDialogButtonColors(dialog);
        }
        private void setDialogBackground(AlertDialog dialog) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(R.drawable.dialog_background); // Ensure you have this drawable
            }
        }

        private void setDialogButtonColors(AlertDialog dialog) {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(Color.WHITE); // Set white text for OK button
            }
            if (negativeButton != null) {
                negativeButton.setTextColor(Color.WHITE); // Set white text for NO button (if exists)
            }
        }

    }

