package iniciativaselebi.com.guinealogiaediciontrivial;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public class ModoLibre extends AppCompatActivity {
    Button button_jugar;
    Button button_save, buttonvolver;
    TextView TextViewSaludo2;
    TextView TextViewRecord;
    EditText editTextNombre;
    int scoretotal;
    private SharedPreferences sharedPreferences;
    private ValueAnimator animator;
    String savedName;
    String message;
    String highScoreText;
    int highScore;
    MediaPlayer swooshPlayer;
    ImageView logo;
    private Animation pulseAnimation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_modo_libre);


        editTextNombre = (EditText) findViewById(R.id.editTextNombre);
        TextViewRecord = (TextView) findViewById(R.id.TextViewRecord);
        button_save = (Button) findViewById(R.id.button_save);
        logo = (ImageView) findViewById(R.id.logo);
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation);
        logo.startAnimation(pulseAnimation);

        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);
        sharedPreferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
        savedName = sharedPreferences.getString("nombre", "");
        TextViewSaludo2 = findViewById(R.id.TextViewSaludo2);

        if (!savedName.isEmpty()) {
            // Display the greeting and record
            message = "¡Mbolan " + savedName + "!";
            TextViewSaludo2.setText(message);
            button_save.setText("CAMBIAR JUGADOR");
            highScore = sharedPreferences.getInt("highscore", 0);
            highScoreText = "EL RECORD ACTUAL ES DE " + highScore + " PUNTOS";
            TextViewRecord.setText(highScoreText);
            editTextNombre.setVisibility(View.GONE); // Hide the EditText
        } else {
            // Display the EditText for name input
            button_save.setText("GUARDAR");
            editTextNombre.setVisibility(View.VISIBLE); // Show the EditText
        }

        button_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (button_save.getText().toString().equals("CAMBIAR JUGADOR")) {
                    // Clear the name from Shared Preferences and update the UI
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.remove("nombre");
                    editor.apply();

                    TextViewSaludo2.setText("");
                    TextViewRecord.setText("EL RECORD ACTUAL ES DE 0 PUNTOS");
                    editTextNombre.setText("");
                    editTextNombre.setVisibility(View.VISIBLE); // Show the EditText

                    button_save.setText("GUARDAR");
                    return;
                }

                // If button says "GUARDAR", save the name and update the UI
                String nombre = editTextNombre.getText().toString();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("nombre", nombre);
                editor.apply();

                Toast.makeText(getApplicationContext(), "Nombre Guardado", Toast.LENGTH_SHORT).show();
                playSwoosh();

                String message = "¡Mbolan " + nombre + " !";
                TextViewSaludo2.setText(message);
                editTextNombre.setVisibility(View.GONE); // Hide the EditText

                highScore = sharedPreferences.getInt("highscore", 0);
                highScoreText = "EL RECORD ACTUAL ES DE " + highScore + " PUNTOS";
                TextViewRecord.setText(highScoreText);

                button_save.setText("CAMBIAR JUGADOR");
            }
        });



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
                playSwoosh();
                Intent intent = new Intent(ModoLibre.this, PreguntasModoLibre.class);
                startActivity(intent);
                finish();
            }
        });

        buttonvolver = (Button) findViewById(R.id.buttonvolver);
        buttonvolver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSwoosh();
                Intent intent = new Intent(ModoLibre.this, Menuprincipal.class);
                startActivity(intent);
                finish();
            }
        });

        ;
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

    @Override
    protected void onPause() {
        super.onPause();

        if (logo != null && pulseAnimation != null) {
            logo.clearAnimation();
        }

        if (animator != null) {
            animator.cancel();
        }
    }

}
