package com.iniciativaselebi.afrikanaone;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;


public class ContactanosActivity extends AppCompatActivity {

    ImageButton imagebuttonwhatsap;

    Button buttonvolver3;

    MediaPlayer swooshPlayer;
    private Animation pulseAnimation;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contactanos);

        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);
        buttonvolver3 = (Button) findViewById(R.id.buttonvolver3);
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation);
        buttonvolver3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSwoosh();
                Intent intent = new Intent(ContactanosActivity.this, Menuprincipal.class);
                startActivity(intent);
            }
        });
        imagebuttonwhatsap = (ImageButton) findViewById(R.id.imagebuttonwhatsap);

        imagebuttonwhatsap.startAnimation(pulseAnimation);
        imagebuttonwhatsap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            openWhatsapp();
            }
        });
    }

    private void openWhatsapp() {
        playSwoosh();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String url = "https://api.whatsapp.com/send?phone=" + "+240222780886"; // Replace 'your_phone_number' with the phone number you want to open the chat with
        intent.setData(Uri.parse(url));
        startActivity(intent);
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



}