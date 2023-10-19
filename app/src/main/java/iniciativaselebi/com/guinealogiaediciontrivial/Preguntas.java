package iniciativaselebi.com.guinealogiaediciontrivial;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
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
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


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







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preguntas);

        initializeUIElements();



        sharedPreferences = getSharedPreferences("UsedQuestionsSharedPreferences", MODE_PRIVATE);

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
        preguntasList = new ArrayList<>();



        questionDataSource = new QuestionDataSource(this);
        questionDataSource.open();
        unusedQuestions = questionDataSource.fetchRandomUnusedQuestions(10);

        // Call moveToNextQuestion directly, as it will handle fetching if needed
        moveToNextQuestion();

        buttonconfirmar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSolutionDisplayed) {
                    int selectedRadioButtonId = radio_group.getCheckedRadioButtonId();
                    if (selectedRadioButtonId == -1) {
                        Toast.makeText(Preguntas.this, "Sin miedo, escoge una opción", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (timer != null) {
                        timer.cancel();
                    }

                    // Get the selected RadioButton using the selectedRadioButtonId
                    selectedRadioButton = findViewById(selectedRadioButtonId);

                    // Make sure the selectedRadioButton is not null before trying to modify it
                    if (selectedRadioButton != null) {
                        selectedRadioButton.setBackgroundResource(R.drawable.radio_normal3);
                    } else {
                        // Handle the unexpected case where the RadioButton is still null
                        Toast.makeText(Preguntas.this, "Ha ocurrido un error inesperado.", Toast.LENGTH_SHORT).show();
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
                if (isSolutionDisplayed) {
                    moveToNextQuestion();
                }
                isSolutionDisplayed = false;
                adjustButtonVisibility();
            }
        });
    }

     private void moveToNextQuestion() {
        Log.d("DebugFlow", "Entering moveToNextQuestion() with unusedQuestions size: " + unusedQuestions.size());
        stopImageAnimation();
        if (!unusedQuestions.isEmpty()) {
           currentQuestion = unusedQuestions.remove(0);
            Log.d("DebugFlow", "Removed a question. New size of unusedQuestions: " + unusedQuestions.size());
            displayQuestion();
        } else {
            fetchUnusedQuestionsFromDatabase();
        }
      //  resetTextView();
        resetImageAndAnimation();
    }


    private void fetchUnusedQuestionsFromDatabase() {
        // Log the size of the unusedQuestions list before fetching
        Log.d("UnusedQuestions", "Current unusedQuestions list size before fetch: " + unusedQuestions.size());

        // Check if there are enough unused questions in the list
        if (unusedQuestions.size() < 2) {
            // Log a message indicating that new questions are being fetched
            Log.d("FetchQuestions", "Fetching new questions because there are less than two unused questions.");

            // Create an instance of FirestoreQuestionManager
            FirestoreQuestionManager manager = new FirestoreQuestionManager(this);

            // Fetch a new batch of questions from Firestore
            manager.fetchQuestionsBatch(new FirestoreQuestionManager.QuestionsFetchCallback() {
                @Override
                public void onQuestionsFetched(List<QuestionModoCompeticion> newQuestions) {
                    clearUsedQuestionsSharedPreferences();
                    // Add the newly fetched questions to the unusedQuestions list
                    unusedQuestions.addAll(newQuestions);
                    Log.d("FetchQuestions", "Added " + newQuestions.size() + " new questions to the unusedQuestions list.");

                    // Optionally, move to the next question or update your UI here
                    moveToNextQuestion();
                }

                @Override
                public void onError(Exception e) {
                    // Handle the error, e.g., show an error message to the user
                    Log.e("FetchQuestions", "Error fetching questions: " + e.getMessage());
                    Toast.makeText(Preguntas.this, "Error fetching new questions. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Log the content of the unusedQuestions list
            for (QuestionModoCompeticion question : unusedQuestions) {
                Log.d("UnusedQuestions", "Question: " + question.getQuestion());
            }
        }
    }

    private void clearUsedQuestionsSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("UsedQuestionsSharedPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        Log.d("UsedQuestionsSharedPreferences", "All used question IDs cleared from SharedPreferences.");
    }

    @SuppressLint("SuspiciousIndentation")
    private void displayQuestion() {
            Log.d("DebugFlow", "Entering displayQuestion() with unusedQuestions size: " + unusedQuestions.size());

            if (unusedQuestions.isEmpty()) {
                fetchUnusedQuestionsFromDatabase();
                return;
            }

             currentQuestion = unusedQuestions.get(0); // Peek, don't remove yet.

            if (isQuestionUsed(currentQuestion.getNumber())) {
                Log.d("DebugFlow", "Question already marked as used. Skipping and calling displayQuestion again.");
                unusedQuestions.remove(0); // Remove the question from the list.
                unusedQuestions.add(currentQuestion); // Add it to the end.
                displayQuestion(); // Recursive call.
                return;
            }

            // If we've reached here, we have an unused question. Remove it from the list.
            unusedQuestions.remove(0);

            // UI Setup
            Log.d("UnusedQuestions", "Displaying question: " + currentQuestion.getQuestion());

            textviewpregunta.setText(currentQuestion.getQuestion());
            radio_button1.setText(currentQuestion.getOptionA());
            radio_button2.setText(currentQuestion.getOptionB());
            radio_button3.setText(currentQuestion.getOptionC());
            textviewcategoria.setText(currentQuestion.getCategory());

            String imageUrl = currentQuestion.getImageUrl();
            Glide.with(this)
                    .load(imageUrl)
                    .error(R.drawable.logotrivial)
                    .into(quizImage);

            radio_group.clearCheck();
            resetRadioButtonColors();

            for (int i = 0; i < radio_group.getChildCount(); i++) {
                radio_group.getChildAt(i).setEnabled(true);
            }

            // Timer
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
                    processAnswer();
                    showSolution();
                    buttonconfirmar.setVisibility(View.GONE);
                    buttonsiguiente.setVisibility(View.VISIBLE);
                    buttonterminar.setVisibility(View.VISIBLE);
                }
            }.start();

            Log.d("CurrentQuestion", "Current Question after assignment: " + currentQuestion);
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
                    // Use the getter method to access the answer property
                    if (selectedOption.equals(currentQuestion.getAnswer())) {
                        Log.d("DebugFlow", "Answer is correct.");
                        correctAnswers++;
                        currentGameAciertos++;
                        MediaPlayer mediaPlayer1 = MediaPlayer.create(getApplicationContext(), R.raw.right);
                        mediaPlayer1.start();
                        score += 500;
                    } else {
                        Log.d("DebugFlow", "Answer is incorrect.");
                        incorrectAnswers++;
                        currentGameFallos++;
                        MediaPlayer mediaPlayer2 = MediaPlayer.create(getApplicationContext(), R.raw.notright);
                        mediaPlayer2.start();
                        score -= 500;
                    }
                }
            }
            // Log that the question is marked as used

            currentQuestion.setUsed(true);
            saveUsedQuestionToSharedPreferences(currentQuestion.getNumber());
            Log.d("QuestionMarkedUsed", "Question marked as used: " + currentQuestion.getQuestion());
            Log.d("QuestionProcess", "Current unusedQuestions list size: " + unusedQuestions.size());

            // Log the content of unusedQuestions
            for (QuestionModoCompeticion q : unusedQuestions) {
                Log.d("QuestionProcess", "Question in list: " + q.getQuestion()); // assuming getQuestion() gets the question text
            }


            textviewaciertos.setText("ACIERTOS: " + correctAnswers);
            textviewpuntuacion.setText("PUNTUACION: " + score);
            textviewfallos.setText("FALLOS: " + incorrectAnswers);

            if (incorrectAnswers >= 4 && !hasShownToast) {
                textviewfallos.setTextColor(Color.RED);
                Toast.makeText(Preguntas.this, "Atención: 4 errores, uno más y se acabará la partida", Toast.LENGTH_LONG).show();
                hasShownToast = true;
            }

            if (incorrectAnswers >= 5) {
                Toast.makeText(getApplicationContext(), "ACUMULASTE CINCO FALLOS.\n     " +
                        "        FIN DE PARTIDA", Toast.LENGTH_LONG).show();

                finishGameTerminar();

                if (areAllQuestionsUsed()) {
                    firestoreQuestionManager.fetchQuestionsBatch(callback);

                }
            }


            }
    public void saveUsedQuestionToSharedPreferences(String questionId) {
        Log.d("DebugFlow", "Entering saveUsedQuestionToSharedPreferences() with questionId: " + questionId);
        Set<String> originalSet = sharedPreferences.getStringSet("usedQuestions", new HashSet<>());
        // Create a copy of the set
        Set<String> usedQuestionsSet = new HashSet<>(originalSet);
        // Add the new questionId to the copied set
        usedQuestionsSet.add(questionId);
        // Save the updated set back to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("usedQuestions", usedQuestionsSet);
        editor.apply();
        Log.d("DebugFlow", "Exiting saveUsedQuestionToSharedPreferences(). Question saved: " + questionId);
    }

    private boolean isQuestionUsed(String questionId) {
        Set<String> usedQuestionsSet = sharedPreferences.getStringSet("usedQuestions", new HashSet<>());
        return usedQuestionsSet.contains(questionId);
    }

    public boolean areAllQuestionsUsed() {
        Set<String> usedQuestionsSet = sharedPreferences.getStringSet("usedQuestions", new HashSet<>());

        for (QuestionModoCompeticion question : unusedQuestions) {
            if (!usedQuestionsSet.contains(question.getNumber())) {
                return false;
            }
        }
        return true;
    }

    private void showSolution() {
            int selectedRadioButtonId = radio_group.getCheckedRadioButtonId();
            RadioButton selectedRadioButton = findViewById(selectedRadioButtonId);
            String selectedOption = selectedRadioButton != null ? selectedRadioButton.getText().toString() : null;
            // Use the getter method to access the answer property
            String correctAnswer = currentQuestion.getAnswer();

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
        }

    FirestoreQuestionManager.QuestionsFetchCallback callback = new FirestoreQuestionManager.QuestionsFetchCallback() {
        @Override
        public void onQuestionsFetched(List<QuestionModoCompeticion> newQuestions) {
            // Handle the new questions here
            // Example:
            unusedQuestions.addAll(newQuestions);
            moveToNextQuestion();
        }

        @Override
        public void onError(Exception e) {
            // Handle the error, e.g., show an error message to the user
            Log.e("FetchQuestions", "Error fetching questions: " + e.getMessage());
            Toast.makeText(Preguntas.this, "Error fetching new questions. Please try again.", Toast.LENGTH_SHORT).show();
        }
    };

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

                if (doubleClick) {
                    isProcessing = true;
                    finishGameTerminar();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            doubleClick = false;
                            isProcessing = false;
                        }
                    }, DOUBLE_CLICK_DELAY);
                } else {
                    doubleClick = true;
                    Toast.makeText(Preguntas.this, "Para terminar la partida pulsa otra vez", Toast.LENGTH_SHORT).show();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            doubleClick = false;
                        }
                    }, DOUBLE_CLICK_DELAY);
                }
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

//        private void resetTextView() {
//        textviewpregunta.setText("");
//        textviewpregunta.setTextColor(getColor(R.color.black)); // replace with your default color
//        textviewpregunta.setTypeface(null, Typeface.NORMAL);
//        textviewpregunta.setGravity(Gravity.START);
//    }

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
        Map<String, Object> gameStats = new HashMap<>();
        gameStats.put("currentGameAciertos", aciertos);
        gameStats.put("currentGameFallos", fallos);
        gameStats.put("currentGamePuntuacion", puntuacion);

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
        if (isBackPressed) {
            backButtonHandler.removeCallbacks(backButtonRunnable);
            finish();
        } else {

            isBackPressed = true;
            Toast.makeText(this, "Pulsa otra vez para salir", Toast.LENGTH_SHORT).show();
            backButtonHandler = new Handler();
            backButtonHandler.postDelayed(backButtonRunnable, 2000);
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
    }

}

