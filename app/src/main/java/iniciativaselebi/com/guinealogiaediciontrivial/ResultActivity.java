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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    MediaPlayer swooshPlayer;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);

        sp = getSharedPreferences("MyUserPrefs", Context.MODE_PRIVATE);
        highscore = sp.getInt("score", 0);
        DisplayNivel = (TextView) findViewById(R.id.DisplayNivel);

        JsonQuestionManager questionManager = new JsonQuestionManager(this);
        List<JsonQuestion> questionList = questionManager.getRandomQuestions(10);
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
            Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse_animation);

// Start the pulse animation on the DisplayNivel view
            DisplayNivel.startAnimation(pulse);
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.noluck);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mediaPlayer.start();
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.guinealogo_mediocre);
            trofeo.setImageDrawable(drawable);
            trofeo.startAnimation(pulse);


        }
        if (C >= 51 && C < 89) {
            DisplayNivel.setText("NO ESTA MAL PERO PODRIAS  HACERLO MEJOR");
            Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse_animation);

// Start the pulse animation on the DisplayNivel view
            DisplayNivel.startAnimation(pulse);
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.mixkit);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mediaPlayer.start();
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.guinealogo_intermedio);
            trofeo.setImageDrawable(drawable);
            trofeo.startAnimation(pulse);

        }
        if (C >= 90) {
            DisplayNivel.setText("NECESITAMOS MAS GUINEAN@S COMO TU");
            Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse_animation);

// Start the pulse animation on the DisplayNivel view
            DisplayNivel.startAnimation(pulse);
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.hallelujah);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mediaPlayer.start();
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.guinealogoexperto);
            trofeo.setImageDrawable(drawable);
            trofeo.startAnimation(pulse);

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
        playSwoosh();
        Intent intent = new Intent(ResultActivity.this, PreguntasModoLibre.class);
        startActivity(intent);
        finish();
    }

    private void playSwoosh() {
        if (swooshPlayer != null) {
            swooshPlayer.seekTo(0);
            swooshPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        if (swooshPlayer != null) {
            swooshPlayer.release();
            swooshPlayer = null;
        }
        super.onDestroy();
    }

    private void salir() {
        SharedPreferences prefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("marcador", marcador);
        editor.apply();
        playSwoosh();
        Intent intent1 = new Intent(ResultActivity.this, ModoLibre.class);
        intent1.putExtra("score", score);

        startActivity(intent1);
        finish();


    }}





