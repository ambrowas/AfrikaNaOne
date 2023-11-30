package iniciativaselebi.com.guinealogiaediciontrivial;

import static android.app.ProgressDialog.show;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;


public class Modocompeticion extends AppCompatActivity {
        private static final String TAG = "Modocompeticion";

        FirebaseAuth auth;
        Button button_jugar2, button_perfil, button_login, button_register, button_clasificacion, buttoncodigo;

        TextView TextViewSaludo2, TextViewRecord, TextViewVolver;
        FirebaseUser user;
        private FirebaseAuth.AuthStateListener authStateListener;
        private int newScore;
        private ValueAnimator animator;
        MediaPlayer swooshPlayer;
        private boolean isFetching = false;
    private List<QuestionModoCompeticion> unusedQuestions;
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    QuestionDataSource dataSource;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_modo_competicion);
            FirestoreQuestionManager manager = new FirestoreQuestionManager(this);


            checkAndFetchQuestionsIfNeeded();

            swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);

             auth = FirebaseAuth.getInstance();

             dataSource = new QuestionDataSource(this);

        button_clasificacion = findViewById(R.id.button_clasificacion);
        button_clasificacion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    playSwoosh();
                    Intent intent = new Intent(Modocompeticion.this, RankingActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    showAlert("Debes iniciar sesión para poder acceder al ranking.");
                }
            }
        });


        button_login = findViewById(R.id.button_login);

            button_perfil = findViewById(R.id.button_perfil);
            button_jugar2 = findViewById(R.id.button_jugar2);

            TextViewSaludo2 = findViewById(R.id.TextViewSaludo2);
            TextViewRecord = findViewById(R.id.TextViewRecord);
            TextViewVolver = findViewById(R.id.TextViewVolver);


            TextViewVolver.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playSwoosh();
                   Intent intent = new Intent(getApplicationContext(), Menuprincipal.class);
                    startActivity(intent);
                    finish();

                }
            });

                buttoncodigo = (Button)findViewById(R.id.buttoncodigo);
                buttoncodigo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playSwoosh();
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
            @Override
            public void onClick(View v) {
                if (auth.getCurrentUser() != null) {
                    confirmLogout();
                } else {
                    playSwoosh();
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
                    showAlert("Ya estás registrado. Presiona JUGAR para comenzar la partida");
                } else {
                    playSwoosh();
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
                    showAlert("Debes iniciar sesión para acceder a tu perfil");
                } else {
                    playSwoosh();
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
                    showAlert("Debes iniciar sesión para poder jugar");
                } else {
                    playSwoosh();
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


    private void showCustomAlertDialog(String title, String message, DialogInterface.OnClickListener onPositiveClickListener) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title) // Set the title
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, onPositiveClickListener)
                .setIcon(R.drawable.logotrivial) // Include the icon
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        dialog.show();
    }

    private void showAlert(String message) {
        showCustomAlertDialog("Atención", message, null);
    }

    private void confirmLogout() {
        DialogInterface.OnClickListener positiveClickListener = (dialog, which) -> {
            auth.signOut();
            playSwoosh();
            TextViewSaludo2.setText("INICIA SESION O REGISTRATE");
            TextViewRecord.setVisibility(View.GONE);
            showAlert("Usuario Desconectado");
        };

        DialogInterface.OnClickListener negativeClickListener = (dialog, which) -> dialog.dismiss();

        AlertDialog dialog = new AlertDialog.Builder(Modocompeticion.this)
                .setTitle("Atención")
                .setMessage("¿Seguro de que quieres cerrar la sesión?")
                .setPositiveButton("Sí", positiveClickListener)
                .setNegativeButton("No", negativeClickListener)
                .setIcon(R.drawable.logotrivial)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        dialog.show();
    }



    private void checkAndFetchQuestionsIfNeeded() {
        // Initialize FirestoreQuestionManager
        final FirestoreQuestionManager manager = new FirestoreQuestionManager(this);

        // Initialize your QuestionDataSource
        final QuestionDataSource dataSource = new QuestionDataSource(this);
        dataSource.open();
        int unusedQuestionsCount = dataSource.getUnusedQuestionsCount(); // assuming you have a method like this in QuestionDataSource
        dataSource.close();

        // Check if unusedQuestionsCount is < 5
        if (unusedQuestionsCount < 5) {
            if (!isFetching) {
                Log.d(TAG, "Starting the process to fetch questions because there is less than 5 unused questions left...");

                // Fetch shuffled batch of questions based on the user's current batch
                manager.fetchShuffledBatchQuestions(new FirestoreQuestionManager.QuestionsFetchCallback() {
                    @Override
                    public void onSuccess(List<QuestionModoCompeticion> fetchedQuestions) {

                    }

                    @Override
                    public void onQuestionsFetched(List<QuestionModoCompeticion> questions) {
                        isFetching = false;
                        Log.d(TAG, "Fetched " + questions.size() + " questions.");

                        // Save fetched questions to local SQLite database
                        dataSource.open();
                        for (QuestionModoCompeticion question : questions) {
                            dataSource.insertOrUpdateQuestion(question);
                        }
                        dataSource.close();
                    }

                    @Override
                    public void onQuestionsUpdated(List<QuestionModoCompeticion> updatedQuestions) {

                    }

                    @Override
                    public void onError(Exception exception) {
                        isFetching = false;
                        Log.e(TAG, "Error fetching questions: " + exception.getMessage());
                    }

                });

                isFetching = true;

            } else {
                Log.d(TAG, "Fetch operation already in progress...");
            }
        } else {
            Log.d(TAG, "There are enough unused questions in the database, no need to fetch now.");
        }
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

                        // If the user has reached 5 failures, hide the button_jugar2 and show the buttoncodigo
                        if (currentgameFallos == 5) {
                            button_jugar2.setVisibility(View.GONE);
                            buttoncodigo.setVisibility(View.VISIBLE);
                        } else {
                            // Ensure button_jugar2 is visible and buttoncodigo is hidden if currentgameFallos is not 5
                            button_jugar2.setVisibility(View.VISIBLE);
                            buttoncodigo.setVisibility(View.GONE);
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
