package com.iniciativaselebi.afrikanaone;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.math.BigInteger;
import java.security.SecureRandom;

import Model.User;
import de.hdodenhof.circleimageview.CircleImageView;

public class ClassficationActivity extends AppCompatActivity {

        FirebaseFirestore firestore;
        TextView textviewrecord, textiviewganancias, textviewaciertos2, textviewpuntuacion2, textviewhola, textviewclasificacion, TextViewFallos,
        textviewgrandtotal;
        CircleImageView profilepic;
        Button  buttonmenuprincipal, buttoncodigocobro;

        String profilePictureUri;
        int aciertos, puntuacion, ganancias, redColor, blueColor, errores, highestScore;
        String uui, name;
        int position;
        long lastClickTime;
        int grandtotal;
        private ValueAnimator animator;
        MediaPlayer swooshPlayer;
       SharedPreferences sharedPreferences;
     private long lastSavedTimestamp = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classfication);

        initializeUIElements();
        retrieveGameStatsFromIntent();
        initializeColorAnimator();
        fetchFirebaseUserInfo();
        setupButtonListeners();
        loadProfilePicture();
        updateHighestScore(puntuacion);
    }


    private void initializeUIElements() {
        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);
        textviewrecord = findViewById(R.id.textviewrecord);
        textiviewganancias = findViewById(R.id.textviewganancias);
        textviewaciertos2 = findViewById(R.id.textviewaciertos2);
        textviewpuntuacion2 = findViewById(R.id.textviewpuntuacion2);
        textviewhola = findViewById(R.id.textviewhola);
        profilepic = findViewById(R.id.profilepic);
        textviewclasificacion = findViewById(R.id.textviewclasificacion);
        TextViewFallos = findViewById(R.id.TextViewFallos);
        textviewgrandtotal = findViewById(R.id.textviewgrandtotal);
        buttonmenuprincipal = findViewById(R.id.buttonmenuprincipal);
        buttoncodigocobro = findViewById(R.id.buttoncodigocobro);
    }

    private void retrieveGameStatsFromIntent() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            aciertos = extras.getInt("aciertos", 0);
            puntuacion = extras.getInt("puntuacion", 0);
            errores = extras.getInt("errores", 0);
        }

        Log.d("DEBUG_TAG", "Aciertos: " + aciertos);
        Log.d("DEBUG_TAG", "Puntuacion: " + puntuacion);
        Log.d("DEBUG_TAG", "Errores: " + errores);

        textviewaciertos2.setText("YOU GOT " + aciertos + " CORRECT ANSWERS");
        TextViewFallos.setText("YOU MADE " + errores + " ERRORS");
        textviewpuntuacion2.setText("YOUR SCORE IS " + puntuacion + " POINTS");
        textiviewganancias.setText("YOUR EARNED " + puntuacion + " AFROS");
    }

    private void initializeColorAnimator() {
        final int[] colors = {Color.RED, Color.GREEN, Color.BLUE};
        animator = ValueAnimator.ofInt(colors);
        animator.setDuration(1000);
        animator.setEvaluator(new ArgbEvaluator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animator -> {
            int color = (int) animator.getAnimatedValue();
            textviewclasificacion.setTextColor(color);
        });
        animator.start();
    }

    private void fetchFirebaseUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(userId);

            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User userData = dataSnapshot.getValue(User.class);
                    if (userData != null) {
                        position = userData.getPositionInLeaderboard();
                        textviewclasificacion.setText(String.valueOf(position));
                        grandtotal = userData.getAccumulatedPuntuacion();
                        textviewgrandtotal.setText("TOTAL EARNINGS: " + grandtotal + " AFROS");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(getApplicationContext(), "Error retrieving ranking position", Toast.LENGTH_SHORT).show();
                }
            });

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User userData = dataSnapshot.getValue(User.class);
                    if (userData != null) {
                        name = userData.getFullname();
                        textviewhola.setText("@ " + name);

                        String profilePicUrl = dataSnapshot.child("profilePicture").getValue(String.class);

                        if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                            // Load the URL into the CircleImageView
                            Glide.with(ClassficationActivity.this)
                                    .load(profilePicUrl)
                                    .into(profilepic);
                        } else {
                            // Set a default placeholder or error image when the URL is null or empty
                            profilepic.setImageResource(R.drawable.baseline_account_circle_24);

                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle error
                }
            });
        }
    }

    private void setupButtonListeners() {
        buttonmenuprincipal.setOnClickListener(v -> showTerminationDialog());

        buttoncodigocobro.setOnClickListener(v -> {
            if (isWithinCoolDownPeriod()) {
                showCooldownAlertDialog();
                return;
            }

            if (puntuacion < 2500) {
                showMinimumCobroDialog();
            } else {
                String code = generateRandomAlphanumericCode(12);
                Intent intent = new Intent(ClassficationActivity.this, QRcodeActivity.class);
                intent.putExtra("aciertos", aciertos);
                intent.putExtra("puntuacion", puntuacion);
                intent.putExtra("name", name);
                intent.putExtra("code", code);
                saveLastSavedTimestamp();
                startActivity(intent);
            }
        });
    }

    private void loadProfilePicture() {
        profilePictureUri = getIntent().getStringExtra("profilePictureUri");
        if (profilePictureUri != null) {
            Glide.with(this)
                    .load(profilePictureUri)
                    .placeholder(R.drawable.baseline_account_circle_24)
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            profilepic.setBackground(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            profilepic.setBackground(placeholder);
                        }
                    });
        }
    }

    private void saveGameStats(int aciertos, int puntuacion, int errores) {
        SharedPreferences sharedPreferences = getSharedPreferences("GameStats", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("aciertos", aciertos);
        editor.putInt("puntuacion", puntuacion);
        editor.putInt("errores", errores);
        editor.apply();
    }

    private boolean isWithinCoolDownPeriod() {
        long currentTimeMillis = System.currentTimeMillis();
        long lastSavedTimestamp = getLastSavedTimestamp();
        long threeMinutesInMillis = 3 * 60 * 1000;

        return (currentTimeMillis - lastSavedTimestamp) < threeMinutesInMillis;
    }

    private void showTerminationDialog() {
        AlertDialog dialog = new AlertDialog.Builder(ClassficationActivity.this)
                .setTitle("EXIT")
                .setMessage("You sure you out?")
                .setPositiveButton("YEP", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        playSwoosh();
                        Intent intent = new Intent(ClassficationActivity.this, Modocompeticion.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton("NOPE", null)
                .setIcon(R.drawable.afrikanaonelogo)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        dialog.show();
    }

    private void showMinimumCobroDialog() {
        AlertDialog dialog = new AlertDialog.Builder(ClassficationActivity.this)
                .setTitle("Minimum Checkout")
                .setMessage("You've got to make at least 3000 AFROS to check out.")
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(R.drawable.afrikanaonelogo)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        dialog.show();
    }

    private void showCooldownAlertDialog() {
        AlertDialog dialog = new AlertDialog.Builder(ClassficationActivity.this)
                .setTitle("Attention")
                .setMessage("You've already generated this code.")
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(R.drawable.afrikanaonelogo)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        dialog.show();
    }

    private void saveLastSavedTimestamp() {
        SharedPreferences sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("last_saved_timestamp", System.currentTimeMillis());
        editor.apply();
    }

    private long getLastSavedTimestamp() {
        SharedPreferences sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        return sharedPreferences.getLong("last_saved_timestamp", 0);
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

    private String generateRandomAlphanumericCode(int length) {
        SecureRandom random = new SecureRandom();
        String generatedString = new BigInteger(60, random).toString(32).toUpperCase();

        // If the generated string is shorter than the desired length,
        // return the entire string. Otherwise, return the substring.
        if (generatedString.length() < length) {
            return generatedString;
        } else {
            return generatedString.substring(0, length);
        }
    }

    private void updateHighestScore(int newScore) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String userId = user.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Integer highestScore = dataSnapshot.child("highestScore").getValue(Integer.class);
                    if (highestScore == null || newScore > highestScore) {
                        userRef.child("highestScore").setValue(newScore);
                        Toast.makeText(ClassficationActivity.this, "New record achieved!", Toast.LENGTH_SHORT).show();
                        highestScore = newScore; // Update the variable with the new score
                    }
                    textviewrecord.setText("RECORD: " + (highestScore == null ? 0 : highestScore + " POINTS"));
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update text views with the values retrieved from the intent during onCreate()
        updateTextViewsFromIntent();
    }

    private void updateTextViewsFromIntent() {
        textviewaciertos2.setText("YOU GOT " + aciertos + " CORRECT ANSWERS");
        TextViewFallos.setText("YOU MADE " + errores + " ERRORS");
        textviewpuntuacion2.setText("YOUR SCORE IS " + puntuacion + " POINTS");
        textiviewganancias.setText("YOUR EARNINGS ARE " + puntuacion + " AFROS");
    }


    @Override
        public void onBackPressed() {
        // Do nothing
        super.onBackPressed();
    }}

