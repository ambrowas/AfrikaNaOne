package com.iniciativaselebi.afrikanaone;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;


public class Menuprincipal extends AppCompatActivity {

        Button button_modolibre, button_modocompeticion, button_contactanos;
        ImageView logo;
        MediaPlayer swooshPlayer;
        private Animation pulseAnimation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menuprincipal);
        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);


        logo = (ImageView) findViewById(R.id.logo);
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation);
        logo.startAnimation(pulseAnimation);

            
            button_modolibre = findViewById(R.id.button_modolibre);
            button_modocompeticion = findViewById(R.id.button_modocompeticion);
            button_contactanos = findViewById(R.id.button_contactanos);
            button_contactanos.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playSwoosh();
                    Intent intent = new Intent(Menuprincipal.this, ContactanosActivity.class);
                    startActivity(intent);
                }
            });

            button_modocompeticion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playSwoosh();
                    Intent intent = new Intent(Menuprincipal.this, Modocompeticion.class);
                    startActivity(intent);
                }
            });


            button_modolibre.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    playSwoosh();
                    Intent intent = new Intent(Menuprincipal.this, ModoLibre.class);
                    startActivity(intent);
                }
            });
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
    }




}
