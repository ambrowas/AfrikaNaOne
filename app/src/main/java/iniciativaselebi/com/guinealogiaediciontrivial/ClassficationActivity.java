package iniciativaselebi.com.guinealogiaediciontrivial;

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
import iniciativaselebi.com.guinealogiaediciontrivial.R;

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

public class ClassficationActivity extends AppCompatActivity {

        FirebaseFirestore firestore;
        TextView textviewrecord, textiviewganancias, textviewaciertos2, textviewpuntuacion2, textviewhola, textviewclasificacion, TextViewFallos,
        textviewgrandtotal;
        ImageView profilepicture;
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



        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            aciertos = extras.getInt("aciertos", 0);
            puntuacion = extras.getInt("puntuacion", 0);
            errores = extras.getInt("errores", 0);

            // Log values from intent
            Log.d("DEBUG_TAG", "Aciertos from intent: " + aciertos);
            Log.d("DEBUG_TAG", "Puntuacion from intent: " + puntuacion);
            Log.d("DEBUG_TAG", "Errores from intent: " + errores);

            // Since you mentioned ganancias and highestScore, ensure you're getting those values either from intent or SharedPreferences.

        } else {
            SharedPreferences sharedPreferences = getSharedPreferences("GameStats", Context.MODE_PRIVATE);

            aciertos = sharedPreferences.getInt("aciertos", 0);
            puntuacion = sharedPreferences.getInt("puntuacion", 0);
            errores = sharedPreferences.getInt("errores", 0);

            // Log values from SharedPreferences
            Log.d("DEBUG_TAG", "Aciertos from SharedPreferences: " + aciertos);
            Log.d("DEBUG_TAG", "Puntuacion from SharedPreferences: " + puntuacion);
            Log.d("DEBUG_TAG", "Errores from SharedPreferences: " + errores);

            // If ganancias and highestScore are important, make sure to retrieve and log them here too.
        }


        redColor = Color.parseColor("#FF0000");
        blueColor = Color.parseColor("#0000FF");

        textviewclasificacion = (TextView)findViewById(R.id.textviewclasificacion);
        final int[] colors = {Color.RED, Color.GREEN, Color.BLUE}; // array of colors to cycle through
        animator = ValueAnimator.ofInt(colors); // create value animator with the array of colors
        animator.setDuration(1000); // set the duration for each color change
        animator.setEvaluator(new ArgbEvaluator()); // set the evaluator to interpolate between the colors
        animator.setRepeatCount(ValueAnimator.INFINITE); // set the repeat count to infinite
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int color = (int) animator.getAnimatedValue(); // get the current color from the animator
                textviewclasificacion.setTextColor(color); // set the text color to the current color
            }
        });
        animator.start(); // start the animator
        TextViewFallos = (TextView)findViewById(R.id.TextViewFallos);
        textviewgrandtotal = (TextView) findViewById(R.id.textviewgrandtotal);

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
                        textviewclasificacion.setText( String.valueOf(position));
                        grandtotal = userData.getAccumulatedPuntuacion();
                        textviewgrandtotal.setText("GANANCIAS ACUMULADAS: " + String.valueOf(grandtotal) + " FCFA");

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(getApplicationContext(), "Error al recoger el puesto", Toast.LENGTH_SHORT).show();
                }
            });
        }

            firestore = FirebaseFirestore.getInstance();
            textviewrecord = (TextView) findViewById(R.id.textviewrecord);
            textiviewganancias = (TextView) findViewById(R.id.textviewganancias);
            textviewaciertos2 = (TextView) findViewById(R.id.textviewaciertos2);
            textviewpuntuacion2 = (TextView) findViewById(R.id.textviewpuntuacion2);
            textviewhola = (TextView) findViewById(R.id.textviewhola);
            profilepicture = (ImageView) findViewById(R.id.profilepicture);
         SharedPreferences sharedPreferences = getSharedPreferences("GameStats", Context.MODE_PRIVATE);
        profilePictureUri = getIntent().getStringExtra("profilePictureUri");

        buttonmenuprincipal = (Button) findViewById(R.id.buttonmenuprincipal);
        buttonmenuprincipal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ClassficationActivity.this)
                        .setTitle("TERMINAR")
                        .setMessage("¿Seguro que quieres concluir la partida?")
                        .setPositiveButton("SI", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // User clicked SI
                                playSwoosh();
                                Intent intent = new Intent(ClassficationActivity.this, Modocompeticion.class);
                                startActivity(intent);
                                finish();  // Close the current activity
                            }
                        })
                        .setNegativeButton("NO", null) // User clicked NO, just dismiss the alert
                        .show();
            }
        });

        String code = generateRandomAlphanumericCode(12);


        buttoncodigocobro = (Button) findViewById(R.id.buttoncodigocobro);
        buttoncodigocobro = (Button) findViewById(R.id.buttoncodigocobro);
        buttoncodigocobro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Check if the user is within the cooldown period
                if (isWithinCoolDownPeriod()) {
                    Toast.makeText(ClassficationActivity.this, "Debes esperar 3 minutos antes de poder generar otro código QR.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if puntuacion is less than 2500
                if (puntuacion < 2500) {
                    new AlertDialog.Builder(ClassficationActivity.this)
                            .setTitle("Cobro Mínimo")
                            .setMessage("Debes ganar al menos 2500 FCFA para poder generar un cobro.")
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                } else {
                    // If puntuacion is 2500 or more, proceed with the current logic

                    Intent intent = new Intent(ClassficationActivity.this, QRcodeActivity.class);
                    intent.putExtra("aciertos", aciertos);
                    intent.putExtra("puntuacion", puntuacion);
                    intent.putExtra("name", name);
                    intent.putExtra("code", code);
                    saveLastSavedTimestamp();
                    startActivity(intent);
                }
            }
        });



        Glide.with(this)
                    .load(profilePictureUri)
                    .placeholder(R.drawable.baseline_account_circle_24)
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            profilepicture.setBackground(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            profilepicture.setBackground(placeholder);
                        }
                    });



            TextView textviewaciertos2 = findViewById(R.id.textviewaciertos2);
            TextView textviewpuntuacion2 = findViewById(R.id.textviewpuntuacion2);
            TextView textiviewganancias = findViewById(R.id.textviewganancias);
            textviewaciertos2.setText("HAS OBTENIDO " + aciertos + " ACIERTOS");
            textviewpuntuacion2.setText("TU PUNTUACION ES DE  " + puntuacion + " PUNTOS");
            textiviewganancias.setText("TUS GANANCIAS SON DE  " + puntuacion + " FCFA");
            TextViewFallos.setText("ACUMULASTE " + errores + " ERRORES");



        if (user != null) {
                String userId = user.getUid();
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(userId);

                userRef.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        User userData = dataSnapshot.getValue(User.class);
                        if (userData != null) {
                            name = userData.getFullname();
                            textviewhola.setText("@ " + name);

                            String profilePicUrl = dataSnapshot.child("profilePicture").getValue(String.class);
                            if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                                Glide.with(ClassficationActivity.this).load(profilePicUrl).into(profilepicture);
                            }

                            saveClassificationData(aciertos, puntuacion, ganancias, uui, name);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle error
                    }

                    private void saveClassificationData(int aciertos, int puntuacion, int ganancias, String uui, String username) {

                    }

                });
                updateHighestScore(puntuacion);
            }
        }

    private boolean isWithinCoolDownPeriod() {
        long currentTimeMillis = System.currentTimeMillis();
        long lastSavedTimestamp = getLastSavedTimestamp();
        long threeMinutesInMillis = 3 * 60 * 1000;

        return (currentTimeMillis - lastSavedTimestamp) < threeMinutesInMillis;
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
                        Toast.makeText(ClassficationActivity.this, "¡Nuevo récord alcanzado!", Toast.LENGTH_SHORT).show();
                        highestScore = newScore; // Update the variable with the new score
                    }
                    textviewrecord.setText("RECORD: " + (highestScore == null ? 0 : highestScore + " PUNTOS"));
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

        // Initialize SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("GameStats", Context.MODE_PRIVATE);

        // Fetch values from SharedPreferences
        int aciertos = sharedPreferences.getInt("aciertos", 0);
        int puntuacion = sharedPreferences.getInt("puntuacion", 0);
        int errores = sharedPreferences.getInt("errores", 0);
        // ... fetch other values ...

        // Now, update your UI elements with these values

        TextView textviewaciertos2 = findViewById(R.id.textviewaciertos2);
        TextView textviewpuntuacion2 = findViewById(R.id.textviewpuntuacion2);
        TextView textiviewganancias = findViewById(R.id.textviewganancias);
        TextViewFallos = (TextView)findViewById(R.id.TextViewFallos);
        textviewaciertos2.setText("HAS OBTENIDO " + aciertos + " ACIERTOS");
        textviewpuntuacion2.setText("TU PUNTUACION ES DE  " + puntuacion + " PUNTOS");
        textiviewganancias.setText("TUS GANANCIAS SON DE  " + puntuacion + " FCFA");
        TextViewFallos.setText("ACUMULASTE " + errores + " ERRORES");
    }



    @Override
        public void onBackPressed() {
        // Do nothing
          }}

