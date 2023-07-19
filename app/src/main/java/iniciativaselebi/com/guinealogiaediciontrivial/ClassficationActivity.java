package iniciativaselebi.com.guinealogiaediciontrivial;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
        Button buttonranking2, buttonmenuprincipal, buttoncodigocobro;
        SharedPreferences preferences;
        String profilePictureUri;
        int aciertos, puntuacion, ganancias, redColor, blueColor, errores, code;
        String uui, name;
        int position;
        long lastClickTime;
        int grandtotal;
        FirebaseDatabase database;
        DatabaseReference myRef;
        private ValueAnimator animator;


    @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_classfication);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            aciertos = extras.getInt("aciertos", 0);
            puntuacion = extras.getInt("puntuacion", 0);
            errores  = extras.getInt("errores", 0);
        } else {
            SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            aciertos = sharedPreferences.getInt("aciertos", 0);
            puntuacion = sharedPreferences.getInt("puntuacion", 0);
            errores  = sharedPreferences.getInt("errores", 0);
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
            preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            profilePictureUri = getIntent().getStringExtra("profilePictureUri");


            buttonranking2 = (Button) findViewById(R.id.buttonranking2);
            buttonranking2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ClassficationActivity.this, RankingActivity.class);
                    startActivity(intent);

                }
            });

            buttonmenuprincipal = (Button) findViewById(R.id.buttonmenuprincipal);
            buttonmenuprincipal.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ClassficationActivity.this, Modocompeticion.class);
                    startActivity(intent);

                }
            });

             String code = generateRandomAlphanumericCode(12);


            buttoncodigocobro = (Button) findViewById(R.id.buttoncodigocobro);
            buttoncodigocobro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (System.currentTimeMillis() - lastClickTime < 2000) {
// Double click detected.
                    Intent intent = new Intent(ClassficationActivity.this, QRcodeActivity.class);
                    intent.putExtra("aciertos", aciertos);
                    intent.putExtra("puntuacion", puntuacion);
                    intent.putExtra("name", name);
                    intent.putExtra("code", code);
                    startActivity(intent);
                } else {
// Single click detected.
                    Toast.makeText(ClassficationActivity.this, "pulsa otra ver para generar tu código de cobro", Toast.LENGTH_SHORT).show();
                    lastClickTime = System.currentTimeMillis();
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
            TextViewFallos.setText("HAS TENIDO " + errores + " ERRORES");


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
        private String generateRandomAlphanumericCode(int length) {
        SecureRandom random = new SecureRandom();
        return new BigInteger(60, random).toString(32).toUpperCase().substring(0, length);

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
                    textviewrecord.setText("PUNTUACION MAS ALTA: " + (highestScore == null ? 0 : highestScore + " PUNTOS"));
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

         @Override
        public void onBackPressed() {
        // Do nothing
          }}

