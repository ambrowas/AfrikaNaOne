package iniciativaselebi.com.guinealogiaediciontrivial;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Modocompeticion extends AppCompatActivity {
        private static final String TAG = "Modocompeticion";

        FirebaseAuth auth;
        Button button_jugar2, button_perfil, button_login, button_register, button_clasificacion, buttoncodigo;
        ImageButton button_back;
        TextView TextViewSaludo2, TextViewRecord;
        FirebaseUser user;
        private FirebaseAuth.AuthStateListener authStateListener;
        private int newScore;
        private ValueAnimator animator;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_modo_competicion);

            button_back = (ImageButton) findViewById(R.id.button_back);
            button_back.setOnClickListener(new View.OnClickListener() {
                private long lastClickTime = 0;

                @Override
                public void onClick(View v) {
                    if (System.currentTimeMillis() - lastClickTime < 2000) {
                        Intent intent = new Intent(Modocompeticion.this, Menuprincipal.class);
                        startActivity(intent);
                        finish();
                    } else {
                        lastClickTime = System.currentTimeMillis();
                        Toast.makeText(getApplicationContext(), "Clickea otra vez para volver", Toast.LENGTH_SHORT).show();

                    }
                }
            });


            auth = FirebaseAuth.getInstance();
            button_clasificacion = findViewById(R.id.button_clasificacion);
            button_clasificacion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        // User is signed in, can proceed to the ranking
                        Intent intent = new Intent(Modocompeticion.this, RankingActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // No user is signed in, show a Toast message
                        Toast.makeText(Modocompeticion.this, "Debe iniciar sesión para poder acceder al ranking.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            button_login = findViewById(R.id.button_login);

            button_perfil = findViewById(R.id.button_perfil);
            button_jugar2 = findViewById(R.id.button_jugar2);
            button_jugar2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        Toast.makeText(getApplicationContext(), "Debe iniciar sesión para poder jugar", Toast.LENGTH_SHORT).show();
                    } else {
                        Intent intent = new Intent(getApplicationContext(), Preguntas.class);
                        startActivity(intent);
                        finish();

            }
                }
            });
            TextViewSaludo2 = findViewById(R.id.TextViewSaludo2);
            TextViewRecord = findViewById(R.id.TextViewRecord);

            buttoncodigo = (Button)findViewById(R.id.buttoncodigo);
            buttoncodigo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), CheckCodigoActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
            button_register = (Button)findViewById(R.id.button_register);

            loadUserHighestScore();
            checkGameFallos();


            authStateListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        button_login.setText("CERRAR SESION");
                    } else {
                        button_login.setText("INICIAR SESION");
                    }
                }
            };

            button_login.setOnClickListener(new View.OnClickListener() {
                private long lastClickTime = 0;

                @Override
                public void onClick(View v) {
                    if (auth.getCurrentUser() != null) {
                        if (System.currentTimeMillis() - lastClickTime < 2000) { // 2000 ms = 2 s
                            auth.signOut();
                            Toast.makeText(getApplicationContext(), "Usuario Desconectado", Toast.LENGTH_SHORT).show();
                            TextViewSaludo2.setText("INICIA SESION O REGISTRATE");
                            TextViewRecord.setVisibility(View.GONE);
                        } else {
                            lastClickTime = System.currentTimeMillis();
                            Toast.makeText(getApplicationContext(), "Presione de nuevo para cerrar sesión", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Intent intent = new Intent(getApplicationContext(), Login.class);
                        startActivity(intent);
                        finish();
                    }
                }
            });


            button_register.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        Toast.makeText(getApplicationContext(), "Ya estás registrado. Presiona JUGAR para comenzar la partida", Toast.LENGTH_SHORT).show();
                    } else {
                        Intent intent = new Intent(getApplicationContext(), Register.class);
                        startActivity(intent);
                        finish();
                    }

                }
            });

            button_perfil.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (auth.getCurrentUser() == null) {
                        Toast.makeText(getApplicationContext(), "Debe iniciar sesión para acceder a su perfil", Toast.LENGTH_SHORT).show();
                    } else {
                        Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            });

            button_jugar2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        Toast.makeText(getApplicationContext(), "Debe iniciar sesión para poder jugar", Toast.LENGTH_SHORT).show();
                    } else {
                        Intent intent = new Intent(getApplicationContext(), Preguntas.class);
                        startActivity(intent);
                        finish();
                    }
                }
            });

            if (auth.getCurrentUser() != null) {
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user").child(auth.getCurrentUser().getUid());
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.hasChild("fullname")) {
                            String fullname = snapshot.child("fullname").getValue(String.class);
                            TextViewSaludo2.setText("¡Bienvenid@, " + fullname + "!");
                        } else {
                            TextViewSaludo2.setText("¡Bienvenid@!");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d(TAG, "onCancelled: " + error.getMessage());
                    }
                });
            }
        }

        private void checkGameFallos() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Retrieve the user's currentgameFallos from the Realtime Database
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user").child(user.getUid());
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.hasChild("currentGameFallos")) {
                        int currentgameFallos = snapshot.child("currentGameFallos").getValue(Integer.class);
                        // If the user has reached 5 failures, show the buttoncodigo
                        if (currentgameFallos >= 5) {
                            buttoncodigo.setVisibility(View.VISIBLE);
                            button_jugar2.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Toast.makeText(getApplicationContext(), "        FIN DE PARTIDA.\nINTRODUCE UN NUEVO \n  CODIGO DE JUEGO.", Toast.LENGTH_LONG).show();

                                }
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.d(TAG, "onCancelled: " + error.getMessage());
                }
            });
        }
    }


        @Override
        protected void onStart() {
            super.onStart();
            auth.addAuthStateListener(authStateListener);
        }

        @Override
        protected void onStop() {
            super.onStop();
            if (authStateListener != null) {
                auth.removeAuthStateListener(authStateListener);
            }
        }

        private void loadUserHighestScore() {
        DatabaseReference leaderboardRef = FirebaseDatabase.getInstance().getReference("user");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String userId = user.getUid();
            leaderboardRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Integer highestScore = dataSnapshot.child("highestScore").getValue(Integer.class);
                    TextViewRecord.setText("TU RECORD ACTUAL ES DE " + (highestScore == null ? 0 : highestScore) + " PUNTOS");
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
                }


                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }
    }

    }
