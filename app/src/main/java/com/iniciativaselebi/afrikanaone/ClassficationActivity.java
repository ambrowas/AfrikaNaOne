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
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
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

public class    ClassficationActivity extends AppCompatActivity {

        FirebaseFirestore firestore;
        TextView textviewrecord, textiviewganancias, textviewaciertos2, textviewpuntuacion2, textviewhola, textviewclasificacion, TextViewFallos,
        textviewgrandtotal;
        CircleImageView profilepic;
        Button  buttonmenuprincipal, buttoncodigocobro, buttonleaderboard;

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
        buttonleaderboard = findViewById(R.id.buttonleaderboard);
    }

    private void retrieveGameStatsFromIntent() {
        // ✅ First, try to retrieve stats from the Intent (primary source)
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            aciertos = extras.getInt("aciertos", 0);
            puntuacion = extras.getInt("puntuacion", 0);
            errores = extras.getInt("errores", 0);

            // ✅ Save retrieved stats into SharedPreferences for persistence
            saveGameStats(aciertos, puntuacion, errores);
        } else {
            // ✅ If no Intent data, fallback to SharedPreferences
            loadGameStatsFromSharedPreferences();
        }

        Log.d("DEBUG_TAG", "Aciertos: " + aciertos);
        Log.d("DEBUG_TAG", "Puntuacion: " + puntuacion);
        Log.d("DEBUG_TAG", "Errores: " + errores);

        textviewaciertos2.setText("CORRECT ANSWERS " + aciertos);
        TextViewFallos.setText("INCORRECT ANSWERS " + errores);
        textviewpuntuacion2.setText("SCORE " + puntuacion + " POINTS");
        textiviewganancias.setText("CASH " + puntuacion + " AFROS");
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
                      //  textviewgrandtotal.setText("YOUR TOTAL CASH IS: " + grandtotal + " AFROS");
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
                        textviewhola.setText("SCORECARD FOR @ " + name.toUpperCase());

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
        buttonmenuprincipal.setOnClickListener(v -> {
            // ✅ Clear stats before returning to main menu
            clearGameStats();
            showTerminationDialog();
        });

        buttoncodigocobro.setOnClickListener(v -> {
            if (isWithinCoolDownPeriod()) {
                showCooldownAlertDialog();
                return;
            }

            if (puntuacion < 2500) {
                showMinimumCobroDialog();
            } else {
                String code = generateRandomAlphanumericCode(12);

                // ✅ Save latest stats before proceeding
                saveGameStats(aciertos, puntuacion, errores);

                Intent intent = new Intent(ClassficationActivity.this, QRcodeActivity.class);
                intent.putExtra("aciertos", aciertos);
                intent.putExtra("puntuacion", puntuacion);
                intent.putExtra("name", name);
                intent.putExtra("code", code);
                saveLastSavedTimestamp();
                startActivity(intent);
            }
        });

        buttonleaderboard.setOnClickListener(v -> {
            // ✅ Save stats before navigating to leaderboard
            saveGameStats(aciertos, puntuacion, errores);
            showLeaderboardDialog();
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
        editor.apply(); // ✅ Save to storage
    }

    private void loadGameStatsFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("GameStats", Context.MODE_PRIVATE);
        aciertos = sharedPreferences.getInt("aciertos", 0);
        puntuacion = sharedPreferences.getInt("puntuacion", 0);
        errores = sharedPreferences.getInt("errores", 0);
    }

    private boolean isWithinCoolDownPeriod() {
        long currentTimeMillis = System.currentTimeMillis();
        long lastSavedTimestamp = getLastSavedTimestamp();
        long threeMinutesInMillis = 3 * 60 * 1000;

        return (currentTimeMillis - lastSavedTimestamp) < threeMinutesInMillis;
    }
    private void showTerminationDialog() {
        showCustomAlertDialog(
                "EXIT",
                "You sure you want to end this game?",
                (dialog, which) -> {
                    Sounds.playSwooshSound(ClassficationActivity.this);
                    saveGameStats(aciertos, puntuacion, errores);
                    Intent intent = new Intent(ClassficationActivity.this, Modocompeticion.class);
                    startActivity(intent);
                    finish();
                },
                null,
                false
        );
    }

    private void showLeaderboardDialog() {
        showCustomAlertDialog(
                "LEADERBOARD",
                "U sure you want to leave?",
                (dialog, which) -> {
                    Sounds.playSwooshSound(ClassficationActivity.this);
                    Intent intent = new Intent(ClassficationActivity.this, RankingActivity.class);
                    startActivity(intent);
                    finish(); // Optional: Finish current activity
                },
                (dialog, which) -> dialog.dismiss(), // "NOPE" button just dismisses the dialog
                false
        );
    }

    private void showMinimumCobroDialog() {
        showCustomAlertDialog(
                "MINIMUM CHECKOUT",
                "You've got to make at least 2500 AFROS to cash out.",
                null,
                null,
                false
        );
    }

    private void showCooldownAlertDialog() {
        showCustomAlertDialog(
                "Attention",
                "You've already generated this code.",
                null,
                null,
                false
        );
    }

    private void showCustomAlertDialog(String title, String message,
                                       DialogInterface.OnClickListener positiveClickListener,
                                       DialogInterface.OnClickListener negativeClickListener,
                                       boolean isSuccess) {
        // 🔊 Play success or warning sound
        if (isSuccess) {
            Sounds.playMagicalSound(getApplicationContext()); // 🎶 Success sound
        } else {
            Sounds.playWarningSound(getApplicationContext()); // ⚠ Warning sound
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("YES", positiveClickListener)
                .setIcon(R.drawable.afrikanaonelogo);

        if (negativeClickListener != null) {
            builder.setNegativeButton("NOPE", negativeClickListener);
        }

        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(dialogInterface -> Sounds.playSwooshSound(getApplicationContext()));
        customizeDialogBackground(dialog);
        dialog.show();
        setDialogButtonColors(dialog); // Ensure white text
    }

    private void customizeDialogBackground(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }
    }

    private void setDialogButtonColors(AlertDialog dialog) {
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(Color.WHITE);
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(Color.WHITE);
        }
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

    private void clearGameStats() {
        SharedPreferences sharedPreferences = getSharedPreferences("GameStats", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear(); // ✅ Clears saved stats
        editor.apply();
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
                        // Update the score in the database
                        userRef.child("highestScore").setValue(newScore);

                        // Play the magic sound
                        Sounds.playMagicalSound(ClassficationActivity.this);

                        // Show the toast and play swoosh when it's dismissed
                        //Toast toast = Toast.makeText(ClassficationActivity.this, "New record achieved!", Toast.LENGTH_SHORT);
                       // toast.show();

                        // Attach a dismiss listener for the swoosh sound
                       // new Handler().postDelayed(() -> Sounds.playSwooshSound(ClassficationActivity.this), toast.getDuration());

                        highestScore = newScore; // Update the variable with the new score
                    }

                    // Update the TextView with the record
                    textviewrecord.setText("HIGHSCORE:                        " + (highestScore == null ? 0 : highestScore + " POINTS"));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("updateHighestScore", "Database error: " + databaseError.getMessage());
                }
            });
        }
    }

private void updateTextViewsFromIntent() {
    textviewaciertos2.setText("CORRECT ANSWERS                                " + aciertos);
    textviewaciertos2.setGravity(Gravity.LEFT);

    TextViewFallos.setText("INCORRECT ANSWERS                            " + errores);
    TextViewFallos.setGravity(Gravity.LEFT);

    textviewpuntuacion2.setText("SCORE                                   " + puntuacion + " POINTS");
    textviewpuntuacion2.setGravity(Gravity.LEFT);

    textiviewganancias.setText("CASH                                      " + puntuacion + " AFROS");
    textiviewganancias.setGravity(Gravity.LEFT);
}

    @Override
    protected void onPause() {
        super.onPause();

        // ✅ Save game stats when the activity is paused
        saveGameStats(aciertos, puntuacion, errores);
    }

    @Override
    protected void onDestroy() {
        if (swooshPlayer != null) {
            swooshPlayer.release();
            swooshPlayer = null;
        }
        super.onDestroy();
        saveGameStats(aciertos, puntuacion, errores);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update text views with the values retrieved from the intent during onCreate()
        updateTextViewsFromIntent();
    }


    @Override
        public void onBackPressed() {
        // Do nothing
        super.onBackPressed();
    }}

