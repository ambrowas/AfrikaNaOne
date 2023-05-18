package iniciativaselebi.com.guinealogiaediciontrivial;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ModoLibre extends AppCompatActivity {

    SharedPreferences sp;

    private static final int REQUEST_CODE_PUNTAJE = 1;
    public static final int REQUEST_CODE_PREGUNTAS_MODO_LIBRE = 1;

    public static final String SHARED_PUTAJE = "sharedPuntaje";

    public static final String KEY_HIGHPUNTAJE = "keyHighpuntaje";

    private int highpuntaje;

    Button button_jugar;
    Button button_salir;
    Button button_reset;
    Button button_save;

    ImageView logo;
    TextView TextViewSaludo2;
    TextView TextViewRecord;
    EditText editTextNombre;
    String saludo2;
    String record;
    int score;
    int scoretotal;
    int marcador;
    int highscore;

    SharedPreferences prefs;
    SharedPreferences my_preferences;
    SharedPreferences myPrefs;
    private int newScore;
    private Typewriter writer;
    private SharedPreferences sharedPreferences;
    private ValueAnimator animator;
    String savedName;
    String message;
    String highScoreText;

    int highScore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_modo_libre);



        sharedPreferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
        savedName = sharedPreferences.getString("nombre", "usuario");
        message = "Bienvenid@ " + savedName;
        TextViewSaludo2 = findViewById(R.id.TextViewSaludo2);
        TextViewSaludo2.setText(message);

        editTextNombre = findViewById(R.id.editTextNombre);


        button_save = (Button)findViewById(R.id.button_save);
        button_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editTextNombre = findViewById(R.id.editTextNombre);
                String nombre = editTextNombre.getText().toString();
                SharedPreferences sharedPreferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("nombre", nombre);
                editor.apply();

                Toast.makeText(getApplicationContext(), "nombre guardado", Toast.LENGTH_SHORT).show();

                TextView textViewSaludo2 = findViewById(R.id.TextViewSaludo2);
                String message = "Bienvenid@ " + sharedPreferences.getString("nombre", "usuario");
                textViewSaludo2.setText(message);
            }
        });

        sharedPreferences = getSharedPreferences("my_preferences", MODE_PRIVATE);
        scoretotal = sharedPreferences.getInt("scoretotal", 0);
        highScore = sharedPreferences.getInt("highscore", 0);

        if (scoretotal > highScore) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("highscore", scoretotal);
            editor.apply();
            highScore = scoretotal;
        }

        TextViewRecord = findViewById(R.id.TextViewRecord);
        final int[] colors = {Color.RED, Color.GREEN, Color.BLUE}; // array of colors to cycle through
        animator = ValueAnimator.ofInt(colors); // create value animator with the array of colors
        animator.setDuration(1000); // set the duration for each color change
        animator.setEvaluator(new ArgbEvaluator()); // set the evaluator to interpolate between the colors
        animator.setRepeatCount(ValueAnimator.INFINITE); // set the repeat count to infinite
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int color = (int) animator.getAnimatedValue(); // get the current color from the animator
                TextViewRecord.setTextColor(color); // set the text color to the current color
            }
        });
        animator.start(); // start the animator
            highScoreText = "EL RECORD ACTUAL ES DE " + highScore + " PUNTOS";
            TextViewRecord.setText(highScoreText);


        button_jugar = (Button) findViewById(R.id.button_jugar);
        button_jugar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ModoLibre.this, PreguntasModoLibre.class);
                startActivity(intent);
                finish();
            }
        });

        button_salir = (Button) findViewById(R.id.button_salir);
        button_salir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ModoLibre.this, Menuprincipal.class);
                startActivity(intent);
                finish();
            }
        });

;}}
