package com.iniciativaselebi.afrikanaone;

import android.content.Context;
import android.content.DialogInterface;
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
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import com.iniciativaselebi.afrikanaone.JsonQuestion;
import com.iniciativaselebi.afrikanaone.JsonQuestionManager;
import com.iniciativaselebi.afrikanaone.Menuprincipal;
import com.iniciativaselebi.afrikanaone.R;
import com.iniciativaselebi.afrikanaone.ResultActivity;

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
    private List<JsonQuestion> questionList;
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

    private Handler blinkHandler = new Handler();
    private Runnable blinkRunnable;

    private JsonQuestionManager questionManager;
    private JsonQuestion currentQuestion;

    private int questionCounter;
    private int questionCountTotal;
    private AlertDialog dialog; // Class member for dialog


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_preguntas_modo_libre);

            questionManager = new JsonQuestionManager(this);
            questionCountTotal = questionManager.getQuestionCount();


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

            questionManager = new JsonQuestionManager(this);
            questionList = questionManager.getRandomQuestions(10); // Assuming you have a method like getRandomQuestions in your JsonQuestionManager
            questionCountTotal = questionList.size();
            Collections.shuffle(questionList);




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


            radio_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    resetRadioButtonColors();  // Reset colors for all buttons to default

                    RadioButton selectedRadioButton = findViewById(checkedId);
                    if (selectedRadioButton != null) {
                        // Set the background and text color for the selected RadioButton
                        selectedRadioButton.setBackgroundResource(R.drawable.radio_button_selected);
                        selectedRadioButton.setTextColor(Color.WHITE);
                    }
                }

                private void resetRadioButtonColors() {
                    // Assuming all RadioButtons have the same default style
                    radio_button1.setBackgroundResource(R.drawable.radio_selector);
                    radio_button1.setTextColor(Color.BLACK);  // Example default color
                    radio_button2.setBackgroundResource(R.drawable.radio_selector);
                    radio_button2.setTextColor(Color.BLACK);
                    radio_button3.setBackgroundResource(R.drawable.radio_selector);
                    radio_button3.setTextColor(Color.BLACK);
                }
            });

            showNextQuestion();

        }

        private void showNextQuestion() {
                if (blinkRunnable != null) {
                    blinkHandler.removeCallbacks(blinkRunnable);
                    resetRadioButtonsVisibility();
                }

                textviewpregunta.setText("");
                textviewpregunta.setTextColor(Color.BLACK);
                textviewpregunta.setTypeface(null, Typeface.NORMAL);
                textviewpregunta.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);

                radio_button1.setTextColor(textColorDefaultRb);
                radio_button2.setTextColor(textColorDefaultRb);
                radio_button3.setTextColor(textColorDefaultRb);
                radio_group.clearCheck();

            if (questionCounter < questionCountTotal) {
                currentQuestion = questionManager.getNextQuestion(this);
                if (currentQuestion == null) {
                    // This means either all questions are shown, or the dialog is being shown
                    return; // Exit the method to avoid further UI updates
                }

                    textviewpregunta.setText(currentQuestion.getQuestion());
                    radio_button1.setText(currentQuestion.getOPTION_A());
                    radio_button2.setText(currentQuestion.getOPTION_B());
                    radio_button3.setText(currentQuestion.getOPTION_C());


                    questionCounter++;
                    textviewcontadorpreguntas.setText("PREGUNTA: " + questionCounter + "/" + questionCountTotal);
                    answered = false;
                    buttonconfirmar.setText("CONFIRMAR");
                    timeLeftInMillis = COUNTDOWN_IN_MILLIS;
                    startCountDown();
                } else {
                    finishQuiz();
                }
            }

        private void resetRadioButtonsVisibility() {
            radio_button1.setVisibility(View.VISIBLE);
            radio_button2.setVisibility(View.VISIBLE);
            radio_button3.setVisibility(View.VISIBLE);
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

        private void checkAnswer() {
            answered = true;
            countDownTimerII.cancel();

            RadioButton rbSelected = findViewById(radio_group.getCheckedRadioButtonId());
            int answerNr = radio_group.indexOfChild(rbSelected) + 1;

            if (answerNr == currentQuestion.getAnswerNr()) {
                score++;
                scoretotal += 500;
                mediaPlayerRight.start();
                textviewpregunta.setText("RESPUESTA CORRECTA");
                textviewpregunta.setTextColor(getColor(R.color.green));
                textviewpregunta.setTypeface(null, Typeface.BOLD);
                textviewpregunta.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            } else {
                errores++;
                mediaPlayerNotRight.start();
                textviewpregunta.setText("RESPUESTA INCORRECTA");
                textviewpregunta.setTextColor(getColor(R.color.red));
                textviewpregunta.setTypeface(null, Typeface.BOLD);
                textviewpregunta.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                blinkCorrectAnswer();
            }

            textviewpuntuacion.setText("PUNTUACION: " + scoretotal);
            textviewaciertos.setText("ACIERTOS: " + score);
            showSolution();
        }

        private void blinkCorrectAnswer() {
            RadioButton correctRb = getCorrectRadioButton();
            final int blinkTimes = 6; // Total number of blinks
            final int blinkInterval = 500; // Milliseconds for each blink

            blinkRunnable = new Runnable() {
                private int times = 0;

                @Override
                public void run() {
                    if (times < blinkTimes) {
                        if (correctRb.getVisibility() == View.VISIBLE) {
                            correctRb.setVisibility(View.INVISIBLE);
                        } else {
                            correctRb.setVisibility(View.VISIBLE);
                        }
                        blinkHandler.postDelayed(this, blinkInterval);
                        times++;
                    } else {
                        correctRb.setVisibility(View.VISIBLE);
                    }
                }
            };

            blinkHandler.post(blinkRunnable);
        }

        private RadioButton getCorrectRadioButton() {
            switch (currentQuestion.getAnswerNr()) {
                case 1:
                    return radio_button1;
                case 2:
                    return radio_button2;
                case 3:
                    return radio_button3;
                default:
                    return null;
            }
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
            super.onBackPressed();
            showExitConfirmation();
        }

        private void showExitConfirmation() {
                new AlertDialog.Builder(this)
                        .setTitle("Confirmar salida")
                        .setMessage("¿Estás seguro de que quieres finalizar?")
                        .setPositiveButton("Sí", (dialog, which) -> finishQuiz())
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .setIcon(R.drawable.afrikanaonelogo) // Set the icon here
                        .create()
                        .show();
            }


        public void showAllQuestionsPlayedDialog() {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Felicidades")
                    .setMessage("Completaste el Modo Libre. Deberías probar el Modo Competición.")
                    .setPositiveButton("OK", (dialogInterface, which) -> {
                        dialogInterface.dismiss();
                        resetAndContinueGame();
                        Intent intent = new Intent(PreguntasModoLibre.this, Menuprincipal.class);
                        startActivity(intent);
                        finish();
                    })
                    .setIcon(R.drawable.afrikanaonelogo)
                    .setCancelable(false);

            dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background); // Your custom shape drawable
            }
            dialog.show();


            // Center the dialog
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.CENTER;
            dialog.getWindow().setAttributes(layoutParams);
        }


        private void resetAndContinueGame() {
            // Reset the question manager to start over
            questionManager.resetShownQuestions();

            // Continue with the game by showing the next question
            showNextQuestion();
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

            // Dismiss the dialog if it is showing
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }

            // Cancel the countdown timer if it is running
            if (countDownTimerII != null) {
                countDownTimerII.cancel();
            }

            // Release media players
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