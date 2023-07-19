package iniciativaselebi.com.guinealogiaediciontrivial;

import static java.lang.Integer.parseInt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import iniciativaselebi.com.guinealogiaediciontrivial.R;


import java.util.ArrayList;
import java.util.Collections;

public class ResultActivity extends AppCompatActivity {

    public static final String EXTRA_PUNTAJE = "extrapuntaje";
    private static final int REQUEST_CODE_PREGUNTAS_MODO_LIBRE = 1;
    private TextView DisplayNivel;
    private TextView DisplayPuntos;
    private TextView DisplayScore;
    private TextView DisplayErrores;

    private TextView TextViewRecord;

    int score;
    int marcador;

    int highscore;

    private String puntaje;
    private String errores;
    private String fallos;
    private  String scoretotal;


    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String KEY_HIGHSCORE = "keyHighscore";
    public static final String KEY_PUNTUACION = "keyPuntuacion";
    int A;
    int B;
    int C;
    SharedPreferences sp;

    private Typewriter writer;
    private ArrayList<Question> questionList;

    private static final int REQUEST_CODE_QUIZ = 2;

    Button jugarotravez;
    Button salir;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        sp = getSharedPreferences("MyUserPrefs", Context.MODE_PRIVATE);
        highscore = sp.getInt("score", 0);
        DisplayNivel = (TextView) findViewById(R.id.DisplayNivel);

        QuizDBHelper dbHelper = new QuizDBHelper(this);
        questionList = dbHelper.getRandomQuestions(10);
        int questionCountTotal = questionList.size();
        Collections.shuffle(questionList);

        puntaje = (getIntent().getExtras().get("puntaje").toString());
        A = parseInt(puntaje);
        B = questionCountTotal;
        C = A * 100 / B;

        ImageView trofeo = (ImageView) findViewById(R.id.trofeo);
        Rotate3dAnimation animation = new Rotate3dAnimation();
        animation.setDuration(1000);
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        trofeo.startAnimation(animation);


        if (C <=50) {
            DisplayNivel.setText("POR GENTE COMO TU GUINEA NO AVANZA");
            Animation growAndShrink = new ScaleAnimation(
                    1f, 1.5f, // fromX, toX
                    1f, 1.5f, // fromY, toY
                    Animation.RELATIVE_TO_SELF, 0.5f, // pivotXType, pivotXValue
                    Animation.RELATIVE_TO_SELF, 0.5f // pivotYType, pivotYValue
            );
            growAndShrink.setDuration(500); // set the duration of the animation
            growAndShrink.setRepeatCount(Animation.INFINITE); // set the number of times the animation will repeat
            growAndShrink.setRepeatMode(Animation.REVERSE); // set the repeat mode to reverse
            DisplayNivel.startAnimation(growAndShrink);
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.noluck);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mediaPlayer.start();
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.guinealogo_mediocre);
            trofeo.setImageDrawable(drawable);
            AlphaAnimation fadeInAnimation = new AlphaAnimation(0.0f, 1.0f);
            fadeInAnimation.setDuration(4000);
            trofeo.startAnimation(fadeInAnimation);

        }
        if (C >= 51 && C < 89) {
            DisplayNivel.setText("NO ESTA MAL PERO PODRIAS  HACERLO MEJOR");
            Animation growAndShrink = new ScaleAnimation(
                    1f, 1.5f, // fromX, toX
                    1f, 1.5f, // fromY, toY
                    Animation.RELATIVE_TO_SELF, 0.5f, // pivotXType, pivotXValue
                    Animation.RELATIVE_TO_SELF, 0.5f // pivotYType, pivotYValue
            );
            growAndShrink.setDuration(500); // set the duration of the animation
            growAndShrink.setRepeatCount(Animation.INFINITE); // set the number of times the animation will repeat
            growAndShrink.setRepeatMode(Animation.REVERSE); // set the repeat mode to reverse
            DisplayNivel.startAnimation(growAndShrink);
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.mixkit);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mediaPlayer.start();
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.guinealogo_intermedio);
            trofeo.setImageDrawable(drawable);
            AlphaAnimation fadeInAnimation = new AlphaAnimation(0.0f, 1.0f);
            fadeInAnimation.setDuration(4000);
            trofeo.startAnimation(fadeInAnimation);

        }
        if (C >= 90) {
            DisplayNivel.setText("NECESITAMOS MAS GUINEAN@S COMO TU");
            Animation growAndShrink = new ScaleAnimation(
                    1f, 1.5f, // fromX, toX
                    1f, 1.5f, // fromY, toY
                    Animation.RELATIVE_TO_SELF, 0.5f, // pivotXType, pivotXValue
                    Animation.RELATIVE_TO_SELF, 0.5f // pivotYType, pivotYValue
            );
            growAndShrink.setDuration(500); // set the duration of the animation
            growAndShrink.setRepeatCount(Animation.INFINITE); // set the number of times the animation will repeat
            growAndShrink.setRepeatMode(Animation.REVERSE); // set the repeat mode to reverse
            DisplayNivel.startAnimation(growAndShrink);
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.hallelujah);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mediaPlayer.start();
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.guinealogoexperto);
            trofeo.setImageDrawable(drawable);
            trofeo.setImageDrawable(drawable);
            AlphaAnimation fadeInAnimation = new AlphaAnimation(0.0f, 1.0f);
            fadeInAnimation.setDuration(4000);
            trofeo.startAnimation(fadeInAnimation);

        }

        SharedPreferences sharedPreferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
        DisplayPuntos = (TextView) findViewById(R.id.displayPuntos);
        puntaje = getIntent().getExtras().get("puntaje").toString();
        DisplayPuntos.setText("PREGUNTAS ACERTADAS: " + puntaje);

        DisplayErrores = (TextView) findViewById(R.id.displayErrores);
        errores = getIntent().getExtras().get("errores").toString();
        DisplayErrores.setText("ERRORES COMETIDOS: " + errores);


        DisplayScore = (TextView) findViewById(R.id.displayScore);
        scoretotal = String.valueOf(parseInt(getIntent().getExtras().get("scoretotal").toString()));
        DisplayScore.setText("PUNTUACION OBTENIDA: " + scoretotal);

        jugarotravez = (Button) findViewById(R.id.jugarotravez);
        jugarotravez.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reiniciar();
            }
        });

        salir = (Button) findViewById(R.id.salir);
        salir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {salir(); }
        });
    }

    private void reiniciar() {
        Intent intent = new Intent(ResultActivity.this, PreguntasModoLibre.class);
        startActivity(intent);
        finish();
    }

    private void salir() {
        SharedPreferences prefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("marcador", marcador);
        editor.apply();
        Intent intent1 = new Intent(ResultActivity.this, ModoLibre.class);
        intent1.putExtra("score", score);

        startActivity(intent1);
        finish();


    }}





