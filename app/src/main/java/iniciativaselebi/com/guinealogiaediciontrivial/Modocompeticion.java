package iniciativaselebi.com.guinealogiaediciontrivial;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;


public class Modocompeticion extends AppCompatActivity {
        private static final String TAG = "Modocompeticion";

        FirebaseAuth auth;
        Button button_jugar2, button_perfil, button_login, button_register, button_clasificacion, buttoncodigo, updateupdateFirestoreButton;

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

        //    manager.assignBatchForNewUser();

            checkAndFetchQuestionsIfNeeded();

            swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);

             auth = FirebaseAuth.getInstance();

             dataSource = new QuestionDataSource(this);
            Button updateupdateFirestoreButton = (Button) findViewById(R.id.updateFirestoreButton);
            updateupdateFirestoreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dataSource.markRandomQuestionsAsUsed(40, true);
                }
            });

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
                        Toast.makeText(Modocompeticion.this, "Debe iniciar sesión para poder acceder al ranking.", Toast.LENGTH_SHORT).show();
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
                private long lastClickTime = 0;

                @Override
                public void onClick(View v) {
                    if (auth.getCurrentUser() != null) {
                        if (System.currentTimeMillis() - lastClickTime < 2000) { // 2000 ms = 2 s
                            auth.signOut();
                            playSwoosh();
                            Toast.makeText(getApplicationContext(), "Usuario Desconectado", Toast.LENGTH_SHORT).show();
                            TextViewSaludo2.setText("INICIA SESION O REGISTRATE");
                            TextViewRecord.setVisibility(View.GONE);
                        } else {
                            lastClickTime = System.currentTimeMillis();
                            Toast.makeText(getApplicationContext(), "Presione de nuevo para cerrar sesión", Toast.LENGTH_SHORT).show();
                        }
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
                        Toast.makeText(getApplicationContext(), "Ya estás registrado. Presiona JUGAR para comenzar la partida", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(getApplicationContext(), "Debe iniciar sesión para acceder a su perfil", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(getApplicationContext(), "Debe iniciar sesión para poder jugar", Toast.LENGTH_SHORT).show();
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


    //public void fetchUnusedQuestionsFromDatabase() {
//        Log.d("FetchQuestions", "fetchUnusedQuestionsFromDatabase called");
//        if (isFetching) {
//            Log.d("FetchQuestions", "Fetching already in progress, returning early");
//            return;
//        }// Exit if already fetching
//        isFetching = true;
//
//        FirestoreQuestionManager manager = new FirestoreQuestionManager(this);
//
//        manager.fetchQuestionsBatch(new FirestoreQuestionManager.QuestionsFetchCallback() {
//            @Override
//            public void onQuestionsFetched(List<QuestionModoCompeticion> newQuestions) {
//                Log.d("FetchQuestions", "onQuestionsFetched callback entered");
//                Log.d("FetchQuestions", "Total questions fetched from Firestore: " + newQuestions.size());
//                for (QuestionModoCompeticion question : newQuestions) {
//                    Log.d("FetchQuestionsDetail", "Question content: " + question.toString());
//                }
//                isFetching = false;
//
//                if (!newQuestions.isEmpty()) {  // Check if the newQuestions list isn't empty
//                    // Delay for 2 seconds (2000 milliseconds)
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            // moveToNextQuestion();
//                        }
//                    }, 1000);
//                } else {
//                    Log.d("FetchQuestions", "No new questions fetched from Firestore.");
//                }
//            }
//
//            @Override
//            public void onError(Exception e) {
//                Log.e("FetchQuestions", "onError callback entered. Error fetching questions: " + e.getMessage());
//                Log.e("FetchQuestions", "Error fetching questions: " + e.getMessage());
//               // Toast.makeText(Modocompeticion.this, "Error fetching new questions. Please try again.", Toast.LENGTH_SHORT).show();
//                isFetching = false;
//            }
//        });
//    }

//    private void fetchShuffledBatchQuestions(int currentUserId, final FirestoreQuestionManager.QuestionsFetchCallback callback) {
//        // Fetch current batch for the user from Firebase Realtime Database
//        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(String.valueOf(currentUserId));
//        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                if (dataSnapshot.exists()) {
//                    int currentBatch = dataSnapshot.child("currentBatch").getValue(Integer.class);
//
//                    // Retrieve the shuffled order from Firestore
//                    firestore.collection("metadata").document("questionOrder")
//                            .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
//                                @Override
//                                public void onSuccess(DocumentSnapshot documentSnapshot) {
//                                    List<Integer> shuffledOrder = (List<Integer>) documentSnapshot.get("order");
//                                    if (shuffledOrder == null || shuffledOrder.isEmpty()) {
//                                        callback.onError(new Exception("No shuffled order found"));
//                                        return;
//                                    }
//
//                                    int batchSize = 50;
//                                    int startIndex = (currentBatch - 1) * batchSize;
//                                    int endIndex = startIndex + batchSize;
//
//                                    if (startIndex >= shuffledOrder.size()) {
//                                        callback.onError(new Exception("All questions have been fetched."));
//                                        return;
//                                    }
//
//                                    List<Integer> batchIndices = shuffledOrder.subList(startIndex, Math.min(endIndex, shuffledOrder.size()));
//
//                                    // Fetch the batch of questions from Firestore based on the batchIndices
//                                    firestore.collection("PREGUNTAS")
//                                            .whereIn("id", batchIndices)
//                                            .get()
//                                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
//                                                @Override
//                                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
//                                                    List<QuestionModoCompeticion> updatedQuestionsList = new ArrayList<>();
//                                                    for (DocumentSnapshot document : queryDocumentSnapshots) {
//                                                        updatedQuestionsList.add(document.toObject(QuestionModoCompeticion.class));
//                                                    }
//
//                                                    // Save to the local database and update Firebase Realtime Database with new batch
//                                                    int nextBatch = (endIndex >= shuffledOrder.size()) ? 1 : currentBatch + 1;
//                                                    setCurrentBatchForUser(currentUserId, nextBatch);
//                                                    updateLocalAndDatabaseWithNewQuestions(updatedQuestionsList, new FirestoreQuestionManager.QuestionsFetchCallback() {
//                                                        @Override
//                                                        public void onQuestionsFetched(List<QuestionModoCompeticion> newQuestions) {
//                                                            callback.onQuestionsFetched(newQuestions);
//                                                        }
//
//                                                        @Override
//                                                        public void onError(Exception e) {
//                                                            callback.onError(e);
//                                                        }
//                                                    });
//                                                }
//                                            })
//                                            .addOnFailureListener(new OnFailureListener() {
//                                                @Override
//                                                public void onFailure(@NonNull Exception e) {
//                                                    callback.onError(e);
//                                                }
//                                            });
//                                }
//                            })
//                            .addOnFailureListener(new OnFailureListener() {
//                                @Override
//                                public void onFailure(@NonNull Exception e) {
//                                    callback.onError(e);
//                                }
//                            });
//                } else {
//                    callback.onError(new Exception("User not found"));
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                callback.onError(databaseError.toException());
//            }
//        });
//    }


    private void updateFirestoreData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetch all documents from PREGUNTAS
        db.collection("PREGUNTAS")
                .get()
                .addOnSuccessListener(allDocs -> {
                    WriteBatch batch = db.batch();  // Use a batch to group updates for efficiency

                    for (DocumentSnapshot document : allDocs.getDocuments()) {
                        DocumentReference docRef = db.collection("PREGUNTAS").document(document.getId());

                        // Get the current fields with spaces
                        String optionA = document.getString("OPTION A");
                        String optionB = document.getString("OPTION B");
                        String optionC = document.getString("OPTION C");

                        // Remove fields with spaces
                        batch.update(docRef, "OPTION A", FieldValue.delete());
                        batch.update(docRef, "OPTION B", FieldValue.delete());
                        batch.update(docRef, "OPTION C", FieldValue.delete());

                        // Add fields without spaces
                        if (optionA != null) {
                            batch.update(docRef, "OPTION_A", optionA);
                        }
                        if (optionB != null) {
                            batch.update(docRef, "OPTION_B", optionB);
                        }
                        if (optionC != null) {
                            batch.update(docRef, "OPTION_C", optionC);
                        }
                    }

                    // Commit the batch
                    batch.commit().addOnSuccessListener(aVoid -> {
                        Log.d("FirestoreUpdate", "Successfully updated fields in all documents.");
                    }).addOnFailureListener(e -> {
                        Log.e("FirestoreUpdate", "Error updating fields", e);
                    });

                })
                .addOnFailureListener(e -> Log.e("FirestoreUpdate", "Error fetching all documents", e));
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
