package com.iniciativaselebi.afrikanaone;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;


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
    private boolean isGooglePlayServicesAvailable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_modo_libre);

       isGooglePlayServicesAvailable();

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
            message = "¡Hello there " + savedName + "!";
            TextViewSaludo2.setText(message);
            button_save.setText("SWTICH PLAYER");
            highScore = sharedPreferences.getInt("highscore", 0);
            highScoreText = "THE CURRENT RECORD IS " + highScore + " POINTS";
            TextViewRecord.setText(highScoreText);
            editTextNombre.setVisibility(View.GONE); // Hide the EditText
        } else {
            // Display the EditText for name input
            button_save.setText("SAVE");
            editTextNombre.setVisibility(View.VISIBLE); // Show the EditText
        }

        button_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (button_save.getText().toString().equals("SWITCH PLAYER")) {
                    // Clear the name from Shared Preferences and update the UI
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.remove("nombre");
                    editor.apply();

                    TextViewSaludo2.setText("");
                    TextViewRecord.setText("CURRENT RECORD IS O POINTS");
                    editTextNombre.setText("");
                    editTextNombre.setVisibility(View.VISIBLE); // Show the EditText

                    button_save.setText("SAVE");
                    return;
                }

                // If button says "GUARDAR", save the name and update the UI
                String nombre = editTextNombre.getText().toString();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("nombre", nombre);
                editor.apply();

                Toast.makeText(getApplicationContext(), "Name saved", Toast.LENGTH_SHORT).show();
                playSwoosh();

                String message = "¡Hello there " + nombre + " !";
                TextViewSaludo2.setText(message);
                editTextNombre.setVisibility(View.GONE); // Hide the EditText

                highScore = sharedPreferences.getInt("highscore", 0);
                highScoreText = "CURRENT RECORD IS " + highScore + " POINTS";
                TextViewRecord.setText(highScoreText);

                button_save.setText("SWITCH PLAYER");
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
        highScoreText = "CURRENT RECORD IS " + highScore + " POINTS";
        TextViewRecord.setText(highScoreText);


        button_jugar = (Button) findViewById(R.id.button_jugar);
        button_jugar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playSwoosh();
                Intent intent = new Intent(ModoLibre.this, PreguntasModoLibre2.class);
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

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, 2404).show();
            } else {
                Log.e("GooglePlayServices", "This device does not support Google Play Services.");
            }
            return false;
        }
        return true;
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
