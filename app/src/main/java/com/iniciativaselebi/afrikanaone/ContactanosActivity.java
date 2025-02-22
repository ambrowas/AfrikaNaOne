package com.iniciativaselebi.afrikanaone;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


public class ContactanosActivity extends AppCompatActivity {

    ImageButton imagebuttonwhatsap;

    Button buttonvolver3;

    TextView textview_contactanos;
    MediaPlayer swooshPlayer;
    private Animation pulseAnimation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contactanos);

        TextView textview_contactanos = (TextView) findViewById(R.id.textview_contactanos);
        Typewriter typewriter = new Typewriter(textview_contactanos);
        typewriter.animateText("For questions, comments and/or suggestions,  kindly press the icon below to contact us via whatsapp.We will try to fix it. " +
                "Thanks for the support");

        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);
        buttonvolver3 = (Button) findViewById(R.id.buttonvolver3);
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation);


        // 🔹 Reset the button completely
        buttonvolver3.setBackground(null); // Removes any previous styling
        buttonvolver3.setBackgroundColor(Color.TRANSPARENT); // Ensures no default color

// 🔹 Apply Rounded Borders to the Return Button
        GradientDrawable border = new GradientDrawable();
        border.setShape(GradientDrawable.RECTANGLE);
        border.setCornerRadius(25); // Rounded corners
        border.setStroke(5, Color.WHITE); // White border with 5dp thickness
        border.setColor(ContextCompat.getColor(this, R.color.black)); // Correct Red Background

        buttonvolver3.setBackground(border);

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