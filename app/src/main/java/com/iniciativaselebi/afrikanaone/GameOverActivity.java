package com.iniciativaselebi.afrikanaone;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


public class GameOverActivity extends AppCompatActivity {

    private Button gameOverText;
    TextView TextViewClicAca;
    private MediaPlayer swooshPlayer, mediaPlayer;
    private Animation disintegratingAnimation, pulseAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);

        initializeMediaPlayers();
        initializeAnimations();
        setupClickListeners();
    }

    private void initializeMediaPlayers() {
        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);
        mediaPlayer = MediaPlayer.create(this, R.raw.gameover);
        mediaPlayer.start();
    }

    private void initializeAnimations() {
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation);
        gameOverText = findViewById(R.id.gameOverText);
        disintegratingAnimation = AnimationUtils.loadAnimation(this, R.anim.disintegrating_animation);

        disintegratingAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Logic for animation start, if needed
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                gameOverText.startAnimation(pulseAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Logic for animation repeat, if needed
            }
        });

        gameOverText.startAnimation(disintegratingAnimation);
    }

    private void setupClickListeners() {
        TextViewClicAca = findViewById(R.id.TextViewClicAca);
        new Handler().postDelayed(() -> {
            if (!isFinishing()) {
                TextViewClicAca.setVisibility(View.VISIBLE);
            }
        }, 7000); // Delay of 5 seconds

        View rootView = findViewById(android.R.id.content);
        rootView.setOnClickListener(v -> {
            playSwoosh();
            navigateToNextActivity();
        });
    }

    private void navigateToNextActivity() {
        int aciertos = getIntent().getIntExtra("aciertos", 0);
        int puntuacion = getIntent().getIntExtra("puntuacion", 0);
        int errores = getIntent().getIntExtra("errores", 0);

        saveToSharedPreferences(aciertos, puntuacion, errores);

        Intent intent = new Intent(GameOverActivity.this, ClassficationActivity.class);
        intent.putExtra("aciertos", aciertos);
        intent.putExtra("puntuacion", puntuacion);
        intent.putExtra("errores", errores);
        startActivity(intent);
        finish();
    }

    private void saveToSharedPreferences(int aciertos, int puntuacion, int errores) {
        SharedPreferences sharedPreferences = getSharedPreferences("GameStats", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("aciertos", aciertos);
        editor.putInt("puntuacion", puntuacion);
        editor.putInt("errores", errores);
        editor.apply();
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
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}
