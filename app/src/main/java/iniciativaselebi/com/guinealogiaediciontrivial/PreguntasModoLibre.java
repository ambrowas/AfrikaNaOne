package iniciativaselebi.com.guinealogiaediciontrivial;

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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import iniciativaselebi.com.guinealogiaediciontrivial.R;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PreguntasModoLibre extends AppCompatActivity {


    public static final long COUNTDOWN_IN_MILLIS = 16000;
    private TextView textviewpuntuacion;
    private TextView textviewcontadorpreguntas;
    private TextView textviewaciertos;
    private TextView textviewtiempo;
    private TextView textviewpregunta;
    private RadioGroup radio_group;
    private RadioButton radio_button1;
    private RadioButton radio_button2;
    private RadioButton radio_button3;
    private Button buttonconfirmar;
    private ColorStateList textColorDefaultRb;
    private ColorStateList textColorDefaultCd;
    private CountDownTimer countDownTimerII;
    private long timeLeftInMillis;
    int errores;
    private List<Question> questionList;
    public int questionCounter;
    private int questionCountTotal;
    private Question currentQuestion;
    int score;
    int scoretotal;
    private boolean answered;
    private long backPressedTime;
    private MediaPlayer mediaPlayer;
    RadioButton selectedRadioButton;
    MediaPlayer mediaPlayerCountdown;
    MediaPlayer mediaPlayerRight;
    MediaPlayer mediaPlayerNotRight;

    MediaPlayer swooshPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preguntas_modo_libre);

        textviewaciertos = findViewById(R.id.textviewaciertos);
        textviewcontadorpreguntas = findViewById(R.id.textviewcontadorpreguntas);
        textviewpregunta = findViewById(R.id.textviewpregunta);
        textviewpuntuacion = findViewById(R.id.textviewpuntuacion);
        textviewtiempo = findViewById(R.id.textviewtiempo);
        radio_group = findViewById(R.id.radio_group);
        radio_button1 = findViewById(R.id.radio_button1);
        radio_button2 = findViewById(R.id.radio_button2);
        radio_button3 = findViewById(R.id.radio_button3);
        buttonconfirmar = findViewById(R.id.buttonconfirmar);
        textColorDefaultRb = radio_button1.getTextColors();
        textColorDefaultCd = textviewtiempo.getTextColors();
        score = 0;
        scoretotal = 0;
        errores = 0;
        mediaPlayerCountdown = MediaPlayer.create(this, R.raw.countdown);
        mediaPlayerRight = MediaPlayer.create(this, R.raw.right);
        mediaPlayerNotRight = MediaPlayer.create(this, R.raw.notright);
        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);

        QuizDBHelper dbHelper = new QuizDBHelper(this);
        ArrayList<Question> questions = dbHelper.getRandomQuestions(10);
        questionList = dbHelper.getRandomQuestions(10);
        questionCountTotal = questions.size();
        Collections.shuffle(questionList);

        showNextQuestion();

        buttonconfirmar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!answered) {
                    if (radio_button1.isChecked() || radio_button2.isChecked() || radio_button3.isChecked()) {
                        checkAnswer();
                    } else {
                        Toast.makeText(PreguntasModoLibre.this, "Sin miedo, escoje una opción", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showNextQuestion();
                }
            }

        });

    }

    private void showNextQuestion() {

        textviewpregunta.setText("");
        textviewpregunta.setTextColor(Color.BLACK);
        textviewpregunta.setTypeface(null, Typeface.NORMAL);
        textviewpregunta.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);


        radio_button1.setTextColor(textColorDefaultRb);
        radio_button2.setTextColor(textColorDefaultRb);
        radio_button3.setTextColor(textColorDefaultRb);
        radio_group.clearCheck();
        if (questionCounter < questionCountTotal) {
            currentQuestion = questionList.get(questionCounter);
            textviewpregunta.setText(currentQuestion.getQuestion());
            radio_button1.setText(currentQuestion.getOption1());
            radio_button2.setText(currentQuestion.getOption2());
            radio_button3.setText(currentQuestion.getOption3());
            questionCounter++;
            textviewcontadorpreguntas.setText("PREGUNTA: " + questionCounter + "/" + questionCountTotal);
            answered = false;
            buttonconfirmar.setText("CONFIRMAR");
            timeLeftInMillis = COUNTDOWN_IN_MILLIS;
            startCountDown();
        } else {
            finishQuiz();
        }
        radio_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // Get the selected RadioButton
                selectedRadioButton = findViewById(checkedId);

                resetRadioButtonColors();

                if (selectedRadioButton != null) {
                    selectedRadioButton.setBackgroundResource(R.drawable.blackbackground);
                    selectedRadioButton.setTextColor(Color.WHITE);
                }
            }

            private void resetRadioButtonColors() {
                radio_button1.setBackgroundResource(R.drawable.radio_selector);
                radio_button1.setTextColor(Color.WHITE);
                radio_button2.setBackgroundResource(R.drawable.radio_selector);
                radio_button2.setTextColor(Color.WHITE);
                radio_button3.setBackgroundResource(R.drawable.radio_selector);
                radio_button3.setTextColor(Color.WHITE);
            }
        });

    }

    private void startCountDown() {
        countDownTimerII = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountdownText();
            }

            @Override
            public void onFinish() {
                timeLeftInMillis = 0;
                updateCountdownText();
                checkAnswer();
            }
        }.start();
    }


    private void updateCountdownText() {
        int seconds = (int) (timeLeftInMillis / 1000);
        String timeFormatted = String.format(Locale.getDefault(), "%02d", seconds);
        textviewtiempo.setText(timeFormatted);

        if (seconds <= 5 && !mediaPlayerCountdown.isPlaying()) {
            mediaPlayerCountdown.start();
        }

        if (timeLeftInMillis < 10000) {
            textviewtiempo.setTextColor(Color.RED);
        } else {
            textviewtiempo.setTextColor(textColorDefaultCd);
        }
    }


        private void checkAnswer () {
            answered = true;
            countDownTimerII.cancel();

            RadioButton rbSelected = findViewById(radio_group.getCheckedRadioButtonId());
            int answerNr = radio_group.indexOfChild(rbSelected) + 1;

            if (answerNr == currentQuestion.getAnswerNr()) {
                score = score + 1;
                scoretotal = scoretotal + 500;
                mediaPlayerRight.start();
                textviewpregunta.setText("RESPUESTA CORRECTA");
                textviewpregunta.setTextColor(getColor(R.color.green));
                textviewpregunta.setTypeface(null, Typeface.BOLD);
                textviewpregunta.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            } else {
                mediaPlayerNotRight.start();
                errores = errores + 1;




                textviewpregunta.setText("RESPUESTA INCORRECTA");
                textviewpregunta.setTextColor(getColor(R.color.red));
                textviewpregunta.setTypeface(null, Typeface.BOLD);
                textviewpregunta.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            }

            textviewpuntuacion.setText("PUNTUACION: " + scoretotal);
            textviewaciertos.setText("ACIERTOS: " + score);
            showSolution();
        }


    private void showSolution() {
        if (questionCounter < questionCountTotal) {
            buttonconfirmar.setText("SIGUIENTE");
        } else {
            buttonconfirmar.setText("TERMINAR");
        }
    }


    private void finishQuiz() {

        SharedPreferences sharedPreferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("scoretotal", scoretotal);
        editor.apply();
        int highscore = sharedPreferences.getInt("highscore", 0);
        if (scoretotal > highscore) {
            Toast.makeText(this, "¡Nuevo récord establecido!", Toast.LENGTH_LONG).show();
        }
        Intent intent = new Intent(PreguntasModoLibre.this, ResultActivity.class);
        intent.putExtra("scoretotal", scoretotal);
        intent.putExtra("puntaje", score);
        intent.putExtra("errores", errores);
        setResult(RESULT_OK, intent);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        showExitConfirmation();
    }

        private void showExitConfirmation() {
            new AlertDialog.Builder(this)
                    .setTitle("Confirmar salida")
                    .setMessage("¿Estás seguro de que quieres finalizar?")
                    .setPositiveButton("Sí", (dialog, which) -> finishQuiz())
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .setIcon(R.drawable.logotrivial) // Set the icon here
                    .create()
                    .show();
        }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimerII != null) {
            countDownTimerII.cancel();
        }

        if (mediaPlayerCountdown != null) {
            mediaPlayerCountdown.release();
        }
        if (mediaPlayerRight != null) {
            mediaPlayerRight.release();
        }
        if (mediaPlayerNotRight != null) {
            mediaPlayerNotRight.release();
        }
    }
}