package iniciativaselebi.com.guinealogiaediciontrivial;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import iniciativaselebi.com.guinealogiaediciontrivial.R;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class Preguntas extends AppCompatActivity {


    private ImageView quizImage;
    private TextView textviewpregunta, textviewcategoria, textviewaciertos, textviewpuntuacion, textviewfallos, textviewtiempo;
    private RadioButton radio_button1, radio_button2, radio_button3;
    private Button buttonconfirmar, buttonsiguiente, buttonterminar;
    private RadioGroup radio_group;
    RadioButton selectedRadioButton;
    private FirebaseFirestore db;
    private List<Map<String, Object>> preguntasList;
    private Map<String, Object> currentQuestion;
    private int currentQuestionIndex;
    private int score, correctAnswers, incorrectAnswers;
    private boolean isSolutionDisplayed;
    private CountDownTimer timer;
    private MediaPlayer mediaPlayer;
    private long remainingTimeInMillis;
    private boolean doubleClick = false;
    private static final int DOUBLE_CLICK_DELAY = 2000; // Delay in milliseconds

    private boolean isBackPressed = false;
    private Handler backButtonHandler;
    int currentGameAciertos;
    int currentGameFallos;
    int currentGamePuntuacion;
    private FirebaseAuth mAuth;
    private FirebaseDatabase firebaseDatabase;

    TableLayout leaderboardTable;
    int rowScore;
    TextView tvScore;
    String userId;
    int newAciertos;
    int newFallos;
    int newPuntuacion;
    private DatabaseReference gameStatsRef;
    private boolean isProcessing = false;
    boolean hasShownToast = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preguntas);

        FirebaseApp.initializeApp(this);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();

        userId = mAuth.getCurrentUser().getUid(); // Replace this with the actual user ID if you have a different way to get it
        DatabaseReference userRef = database.getReference("user").child(userId);
        gameStatsRef = database.getReference("user").child(userId);

        initializeUIElements();
        currentGameAciertos = 0;
        currentGameFallos = 0;
        currentGamePuntuacion = 0;

        db = FirebaseFirestore.getInstance();
        preguntasList = new ArrayList<>();
        fetchQuestionsFromFirestore();

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

                    processAnswer();


                    selectedRadioButton.setBackgroundResource(R.drawable.radio_normal3);
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
                    currentQuestionIndex++;
                    if (currentQuestionIndex < preguntasList.size()) {
                        displayQuestion();
                    } else {
                        // Show end game message or navigate to the results activity
                    }
                    isSolutionDisplayed = false;
                    buttonconfirmar.setVisibility(View.VISIBLE);
                    buttonsiguiente.setVisibility(View.GONE);
                    buttonterminar.setVisibility(View.GONE);
                }
            }
        });
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

    private void fetchQuestionsFromFirestore() {
        db.collection("PREGUNTAS").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                List<DocumentSnapshot> documents = querySnapshot.getDocuments();
                for (DocumentSnapshot document : documents) {
                    // Populate currentQuestion with the retrieved data
                    currentQuestion = new HashMap<>();
                    currentQuestion.put("question", document.getString("QUESTION"));

                    // Create a list of answer options and shuffle it
                    List<String> answerOptions = new ArrayList<>();
                    answerOptions.add(document.getString("OPTION A"));
                    answerOptions.add(document.getString("OPTION B"));
                    answerOptions.add(document.getString("OPTION C"));
                    Collections.shuffle(answerOptions);

                    currentQuestion.put("optionA", answerOptions.get(0));
                    currentQuestion.put("optionB", answerOptions.get(1));
                    currentQuestion.put("optionC", answerOptions.get(2));
                    currentQuestion.put("answer", document.getString("ANSWER"));
                    currentQuestion.put("category", document.getString("CATEGORY"));
                    currentQuestion.put("image", document.getString("IMAGE"));

                    preguntasList.add(currentQuestion);
                }

                // Shuffle the questions
                Collections.shuffle(preguntasList);

                score = 0;
                correctAnswers = 0;
                incorrectAnswers = 0;
                isSolutionDisplayed = false;

                displayQuestion();
            }
        });
    }

    private void displayQuestion() {

        resetTextView();
        currentQuestion = preguntasList.get(currentQuestionIndex);

        textviewpregunta.setText(currentQuestion.get("question").toString());
        radio_button1.setText(currentQuestion.get("optionA").toString());
        radio_button2.setText(currentQuestion.get("optionB").toString());
        radio_button3.setText(currentQuestion.get("optionC").toString());
        textviewcategoria.setText(currentQuestion.get("category").toString());

        String imageUrl = currentQuestion.get("image").toString();

        Glide.with(this)
                .load(imageUrl)
                .error(R.drawable.logotrivial) // Set a placeholder image or a fallback image
                .into(quizImage);

        radio_group.clearCheck();
        resetRadioButtonColors();

        for (int i = 0; i < radio_group.getChildCount(); i++) {
            radio_group.getChildAt(i).setEnabled(true);
        }

        // Set the countdown timer to 16 seconds and start it
        timer = new CountDownTimer(16000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                String timeLeftFormatted = (seconds < 10) ? String.format(Locale.getDefault(), "0%d", seconds)
                        : String.format(Locale.getDefault(), "%d", seconds);
                textviewtiempo.setText(timeLeftFormatted);

                if (seconds <= 10) {
                    textviewtiempo.setTextColor(Color.RED); // set font color to red
                } else {
                    textviewtiempo.setTextColor(Color.BLACK); // set font color to black
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
    }

    private void resetTextView() {
        textviewpregunta.setText("");
        textviewpregunta.setTextColor(getColor(R.color.black)); // replace with your default color
        textviewpregunta.setTypeface(null, Typeface.NORMAL);
        textviewpregunta.setGravity(Gravity.START);
    }

    private void processAnswer() {
        int selectedRadioButtonId = radio_group.getCheckedRadioButtonId();
        if (selectedRadioButtonId == -1) {
            MediaPlayer mediaPlayer2 = MediaPlayer.create(getApplicationContext(), R.raw.notright);
            mediaPlayer2.start();
            score -= 500;
            incorrectAnswers++;
        } else {
            RadioButton selectedRadioButton = findViewById(selectedRadioButtonId);
            String selectedOption = selectedRadioButton.getText().toString();
            if (selectedOption.equals(currentQuestion.get("answer"))) {
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

        textviewaciertos.setText("ACIERTOS: " + correctAnswers);
        textviewpuntuacion.setText("PUNTUACION: " + score);
        textviewfallos.setText("FALLOS: " + incorrectAnswers);

        if (incorrectAnswers >= 4 && !hasShownToast) {
            textviewfallos.setTextColor(Color.RED);
            Toast.makeText(Preguntas.this, "Atención: 4 errores, uno más y se acabará la partida", Toast.LENGTH_LONG).show();
            hasShownToast = true;
        }


        if (incorrectAnswers >= 5) {

            finishGame();

        }
    }

    private void resetRadioButtonColors() {
        radio_button1.setBackgroundResource(R.drawable.radio_selector);
        radio_button2.setBackgroundResource(R.drawable.radio_selector);
        radio_button3.setBackgroundResource(R.drawable.radio_selector);
    }

    private void showSolution() {
        int selectedRadioButtonId = radio_group.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedRadioButtonId);
        String selectedOption = selectedRadioButton != null ? selectedRadioButton.getText().toString() : null;

        String correctAnswer = currentQuestion.get("answer").toString();
        if (selectedOption != null && selectedOption.equals(correctAnswer)) {
            textviewpregunta.setText("RESPUESTA CORRECTA");
            textviewpregunta.setTextColor(getColor(R.color.green));
            textviewpregunta.setTypeface(null, Typeface.BOLD);
            textviewpregunta.setGravity(Gravity.CENTER);
        } else {
            textviewpregunta.setText("RESPUESTA INCORRECTA");
            textviewpregunta.setTextColor(getColor(R.color.red));
            textviewpregunta.setTypeface(null, Typeface.BOLD);
            textviewpregunta.setGravity(Gravity.CENTER);
        }

        isSolutionDisplayed = true;
        buttonconfirmar.setVisibility(View.GONE);
        buttonsiguiente.setVisibility(View.VISIBLE);
        buttonterminar.setVisibility(View.VISIBLE);
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


    private void finishGame() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("aciertos", correctAnswers);
        editor.putInt("puntuacion", score);
        editor.putInt("errores", incorrectAnswers);
        editor.apply();

        updateAccumulatedValues(userId, correctAnswers, incorrectAnswers, score);
        updateCurrentGameValues(correctAnswers, incorrectAnswers, score);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                showGameOverScreen();
            }
        }, 500); // 500ms = 0.5 seconds
    }
    private void finishGameTerminar() {

        updateAccumulatedValues(userId, correctAnswers, incorrectAnswers, score);
        updateCurrentGameValues(correctAnswers, incorrectAnswers, score);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Intent intent = new Intent(getApplicationContext(), ClassficationActivity.class);
        intent.putExtra("aciertos", correctAnswers);
        intent.putExtra("puntuacion", score);
        intent.putExtra("errores", incorrectAnswers);
        startActivity(intent);
        finish();



    }

    private void showGameOverScreen() {
        Intent intent = new Intent(getApplicationContext(), GameOverActivity.class);
        startActivity(intent);
        Toast.makeText(getApplicationContext(), "ACUMULASTE CINCO FALLOS.\n             FIN DE PARTIDA", Toast.LENGTH_LONG).show();
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

