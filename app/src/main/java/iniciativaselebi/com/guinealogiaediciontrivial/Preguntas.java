package iniciativaselebi.com.guinealogiaediciontrivial;

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
import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preguntas);

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
                    int selectedRadioButtonId = radio_group.getCheckedRadioButtonId();
                    if (selectedRadioButtonId == -1) {
                        Toast.makeText(Preguntas.this, "Sin miedo, escoge una opción", Toast.LENGTH_SHORT).show();
                        isAlertDisplayed = true;

                        // Reset the isAlertDisplayed flag after the duration of the Toast
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                isAlertDisplayed = false;
                            }
                        }, 2000); // For LENGTH_SHORT, use 2000 ms delay

                        return;
                    }

                    if (timer != null) {
                        timer.cancel();
                    }

                    RadioButton selectedRadioButton = findViewById(selectedRadioButtonId);
                    if (selectedRadioButton != null) {
                        selectedRadioButton.setBackgroundResource(R.drawable.radio_normal3);
                    } else {
                        Toast.makeText(Preguntas.this, "Ha ocurrido un error inesperado.", Toast.LENGTH_SHORT).show();
                        isAlertDisplayed = true;

                        // Reset the isAlertDisplayed flag after the duration of the Toast
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                isAlertDisplayed = false;
                            }
                        }, 2000); // For LENGTH_SHORT, use 2000 ms delay

                        return;
                    }

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

        // Display the next question from the current batch
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

    private void displayQuestion()  {
        resetImageAndAnimation();
        if (currentQuestion == null) {
            Log.e("DisplayQuestionError", "No current question to display!");
            // Perhaps show an error message to the user or take some corrective action.
            return;
        }
        questionInProgress = true;
        // UI Setup
        textviewpregunta.setText(currentQuestion.getQUESTION());
        radio_button1.setText(currentQuestion.getOPTION_A());
        radio_button2.setText(currentQuestion.getOPTION_B());
        radio_button3.setText(currentQuestion.getOPTION_C());
        textviewcategoria.setText(currentQuestion.getCATEGORY());

        // Load Image
        String imageUrl = currentQuestion.getIMAGE();
        Glide.with(this)
                .load(imageUrl)
                .error(R.drawable.logotrivial)
                .into(quizImage);

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
                String timeLeftFormatted = (seconds < 10) ? String.format(Locale.getDefault(), "0%d", seconds)
                        : String.format(Locale.getDefault(), "%d", seconds);
                textviewtiempo.setText(timeLeftFormatted);

                if (seconds <= 10) {
                    textviewtiempo.setTextColor(Color.RED);
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
                processAnswer();
                showSolution();
                buttonconfirmar.setVisibility(View.GONE);
                buttonsiguiente.setVisibility(View.VISIBLE);
                buttonterminar.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    private void processAnswer() {
        if (currentQuestion == null) {
            Log.d("DebugFlow", "currentQuestion is null in processAnswer(). Moving to next question.");
            moveToNextQuestion();
            return;
        }

        int selectedRadioButtonId = radio_group.getCheckedRadioButtonId();
        if (selectedRadioButtonId == -1) {
            MediaPlayer mediaPlayer2 = MediaPlayer.create(getApplicationContext(), R.raw.notright);
            mediaPlayer2.start();
            score -= 500;
            incorrectAnswers++;
        } else {
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
        }

        currentQuestion.setUsed(true);
        questionDataSource.markQuestionAsUsed(currentQuestion.getNUMBER());
        questionInProgress = false;

        textviewaciertos.setText("ACIERTOS: " + correctAnswers);
        textviewpuntuacion.setText("PUNTUACION: " + score);
        textviewfallos.setText("FALLOS: " + incorrectAnswers);

        if (incorrectAnswers >= 4 && !hasShownToast) {
            textviewfallos.setTextColor(Color.RED);
            showCustomDialog("Atención", "4 errores; Uno más y se acabará la partida");
            hasShownToast = true;
        }

        if (incorrectAnswers >= 5) {
            showCustomDialog("Fin de Partida", "Acumulaste cinco fallos", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finishGameTerminar();
                }
            });
            hasShownToast = true;
        }
    }

    private void showCustomDialog(String title, String message, DialogInterface.OnClickListener positiveClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Preguntas.this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", positiveClickListener)
                .setIcon(R.drawable.logotrivial);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background); // Your custom shape drawable
        }
        dialog.show();
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
                textviewpregunta.setText("RESPUESTA CORRECTA");
                textviewpregunta.setTextColor(getColor(R.color.green));
                resultImage.setImageResource(R.drawable.baseline_check_24);
                resultImage.setColorFilter(getColor(R.color.green));
            } else {
                textviewpregunta.setText("RESPUESTA INCORRECTA");
                textviewpregunta.setTextColor(getColor(R.color.red));
                resultImage.setImageResource(R.drawable.baseline_clear_24);
                resultImage.setColorFilter(getColor(R.color.red));
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

    private void stopImageAnimation() {
        ImageView resultImage = findViewById(R.id.resultImage);
        resultImage.animate().cancel();

        resultImage.setScaleX(1f); // reset the scale
        resultImage.setScaleY(1f); // reset the scale

        resultImage.setVisibility(View.INVISIBLE);
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
                AlertDialog dialog = new AlertDialog.Builder(Preguntas.this)
                        .setTitle("Confirmar")
                        .setMessage("¿Seguro que quieres terminar la partida?")
                        .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // User clicked "Sí" button. Finish the game.
                                isProcessing = true;
                                finishGameTerminar();
                                isProcessing = false;
                            }
                        })
                        .setNegativeButton("No", null) // null listener results in dismissing the dialog without additional actions
                        .setIcon(R.drawable.logotrivial)
                        .create(); // create the AlertDialog to manipulate its window properties

                Window window = dialog.getWindow(); // Get the Window object of the dialog
                if (window != null) {
                    window.setBackgroundDrawableResource(R.drawable.dialog_background); // Set your custom background drawable here
                }

                dialog.show(); // Show the dialog
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
                    selectedRadioButton.setBackgroundResource(R.drawable.radio_normal3);
                    selectedRadioButton.setTextColor(Color.BLACK);
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
                            Toast.makeText(Preguntas.this, "Error al actualizar los valores acumulados. Intenta de nuevo.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
                Toast.makeText(Preguntas.this, "Error al leer los datos. Intenta de nuevo.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCurrentGameValues(int aciertos, int fallos, int puntuacion) {
        // Create a map for the game statistics
        Map<String, Object> gameStats = new HashMap<>();
        gameStats.put("currentGameAciertos", aciertos);
        gameStats.put("currentGameFallos", fallos);
        gameStats.put("currentGamePuntuacion", puntuacion);

        // Create a timestamp for the current time
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());

        // Add the LastPlay timestamp to the map
        gameStats.put("LastPlay", currentDateAndTime);

        // Update the database
        gameStatsRef.updateChildren(gameStats).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (!task.isSuccessful()) {
                    Toast.makeText(Preguntas.this, "Error al guardar los datos. Intenta de nuevo.", Toast.LENGTH_SHORT).show();
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
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        showExitConfirmation();
    }

    private void showExitConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar salida")
                .setMessage("¿Estás seguro de que quieres salir?")
                .setPositiveButton("Sí", (dialog, which) -> finish())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setIcon(R.drawable.logotrivial) // Set the icon here
                .create()
                .show();
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
        // App is going into the background
        isAppInBackground = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("ActivityLifecycle", "onResume: App returning to foreground");
        // Only call processPenaltyForLeavingApp if a question is in progress
        if (isAppInBackground && timerWasActive && questionInProgress && incorrectAnswers < 3 && !isAlertDisplayed) {
            Log.d("ActivityLifecycle", "onResume: Calling processPenaltyForLeavingApp");
            processPenaltyForLeavingApp();
        }
        isAppInBackground = false;
    }

    private void processPenaltyForLeavingApp() {
        Log.d("PenaltyLogic", "processPenaltyForLeavingApp: Triggered");

        if (questionInProgress && incorrectAnswers < 4) {
            // Pause the timer
            if (timer != null) {
                timer.cancel();
            }

            // Show an AlertDialog
            AlertDialog dialog = new AlertDialog.Builder(Preguntas.this)
                    .setTitle("Aviso")
                    .setMessage("No abandones la aplicación mientras haya una pregunta activa.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Restart the timer and apply the penalty when OK is clicked
                            applyPenalty();
                        }
                    })
                    .setCancelable(false)
                    .setIcon(R.drawable.logotrivial)
                    .create(); // Use create() here to be able to customize the dialog before showing it

// Access the window of the dialog to set the background drawable
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(R.drawable.dialog_background); // Your custom shape drawable
            }

// Finally, show the dialog
            dialog.show();
            isAlertDisplayed = true; // Indicate that an alert is being displayed
        }
    }

    private void applyPenalty() {
        // Play sound effect for incorrect answer
        MediaPlayer mediaPlayer2 = MediaPlayer.create(getApplicationContext(), R.raw.notright);
        mediaPlayer2.start();

        // Update the score and incorrect answers count
        score -= 500;
        incorrectAnswers++;

        // Update the UI to reflect the penalty
        textviewaciertos.setText("ACIERTOS: " + correctAnswers);
        textviewpuntuacion.setText("PUNTUACION: " + score);
        textviewfallos.setText("FALLOS: " + incorrectAnswers);

        // Check if the game over condition is met
        if (incorrectAnswers >= 5) {
            Toast.makeText(getApplicationContext(), "ACUMULASTE CINCO FALLOS.\n" +
                    "FIN DE PARTIDA", Toast.LENGTH_LONG).show();
            finishGameTerminar();
        } else {
            // Mark the current question as answered incorrectly
            if (currentQuestion != null) {
                currentQuestion.setUsed(true);
                questionDataSource.markQuestionAsUsed(currentQuestion.getNUMBER());
            }

            // Prepare UI for the next question (without moving to it immediately)
            showSolution();
            buttonconfirmar.setVisibility(View.GONE);
            buttonsiguiente.setVisibility(View.VISIBLE);
            buttonterminar.setVisibility(View.VISIBLE);
        }

        // Reset the questionInProgress flag
        questionInProgress = false;

        // Reset the isAlertDisplayed flag as the alert has been handled
        isAlertDisplayed = false;
    }

}

