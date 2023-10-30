package iniciativaselebi.com.guinealogiaediciontrivial;

import static com.google.firebase.crashlytics.internal.Logger.TAG;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;



    public class FirestoreQuestionManager {
            public static final int BATCH_SIZE = 50;
            private static final int MAX_EXCLUSION_CHUNK_SIZE = 10;

            private Context context;
            private QuestionDataSource dataSource;
            private FirebaseFirestore firestore;
            private FirebaseDatabase database = FirebaseDatabase.getInstance();
            private FirebaseAuth mAuth = FirebaseAuth.getInstance();
             private FirebaseUser currentUser;
            private ConcurrentLinkedQueue<QuestionModoCompeticion> concurrentQueue = new ConcurrentLinkedQueue<>();
            private QuestionDatabaseHelperII dbHelper;
            private static final int TOTAL_BATCHES = 10;





        public interface QuestionsFetchCallback {
                void onQuestionsFetched(List<QuestionModoCompeticion> newQuestions);
                void onError(Exception e);
            }
        public FirestoreQuestionManager(Context context) {
                this.context = context;
                this.dbHelper = new QuestionDatabaseHelperII(this.context);
                dataSource = new QuestionDataSource(context);
                firestore = FirebaseFirestore.getInstance();
                mAuth = FirebaseAuth.getInstance();
                currentUser = mAuth.getCurrentUser();
            }

        public void fetchShuffledBatchQuestions(final QuestionsFetchCallback callback) {
            Log.d(TAG, "Starting the process to fetch questions...");

            // Get current user's ID
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser mUser = mAuth.getCurrentUser();

            if (mUser == null) {
                Log.e(TAG, "User is not authenticated. Cannot fetch questions.");
                callback.onError(new Exception("User is not authenticated."));
                return;
            }

            String userId = mUser.getUid();

            FirebaseDatabase rtdb = FirebaseDatabase.getInstance();
            DatabaseReference batchRef = rtdb.getReference("Users").child(userId).child("currentBatch");

            batchRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        long currentBatch = dataSnapshot.getValue(Long.class);
                        Log.d(TAG, "Successfully fetched currentBatch: " + currentBatch + " from Realtime Database.");

                        // Access Firestore
                        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

                        // Fetch the shuffledOrder array for the current batch from the Metadata collection
                        DocumentReference batchReference = firestore.collection("Metadata").document("Batch_" + currentBatch);
                        batchReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                if (documentSnapshot.exists() && documentSnapshot.contains("shuffledOrder")) {
                                    List<String> shuffledOrder = (List<String>) documentSnapshot.get("shuffledOrder");

                                    // Fetch the actual questions from the PREGUNTAS collection based on the shuffledOrder list
                                    fetchQuestionsFromShuffledOrder(shuffledOrder, callback);

                                } else {
                                    Log.e(TAG, "Metadata document does not exist or shuffledOrder is missing.");
                                    callback.onError(new Exception("Metadata document does not exist or shuffledOrder is missing."));
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Failed fetching shuffledOrder for currentBatch from Firestore. Error: " + e.getMessage());
                                callback.onError(e);
                            }
                        });
                    } else {
                        Log.e(TAG, "currentBatch does not exist in Realtime Database. Trying to assign or fetch a valid batch...");

                        // If the currentBatch does not exist, get or assign one
                        getCurrentBatchForUser(new CurrentBatchCallback() {
                            @Override
                            public void onBatchRetrieved(int batchNumber) {
                                // Once a batch is retrieved, you can then attempt to fetch the questions again.
                                fetchShuffledBatchQuestions(callback);
                            }

                            @Override
                            public void onError(Exception e) {
                                // Handle the error
                                callback.onError(e);
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Failed fetching currentBatch from Realtime Database. Error: " + databaseError.getMessage());
                    callback.onError(new Exception(databaseError.getMessage()));
                }
            });
        }

        private void fetchQuestionsFromShuffledOrder(List<String> shuffledOrder, final QuestionsFetchCallback callback) {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            CollectionReference preguntasCollection = firestore.collection("PREGUNTAS");

            // Using a batch to fetch all questions at once might be more efficient, but for simplicity here, we're fetching them one by one
            List<QuestionModoCompeticion> fetchedQuestions = new ArrayList<>();
            for (String questionId : shuffledOrder) {
                preguntasCollection.document(questionId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        QuestionModoCompeticion question = documentSnapshot.toObject(QuestionModoCompeticion.class);
                        if (question != null) {
                            fetchedQuestions.add(question);
                        }

                        // Check if we've fetched all questions
                        if (fetchedQuestions.size() == shuffledOrder.size()) {
                            callback.onQuestionsFetched(fetchedQuestions);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e);
                    }
                });
            }
        }



        private void fetchQuestionsByOrder(List<String> shuffledOrder, final QuestionsFetchCallback callback) {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            CollectionReference preguntasCollection = firestore.collection("PREGUNTAS");

            // Assuming the shuffled order contains the document IDs of the questions
            List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
            for (String docId : shuffledOrder) {
                tasks.add(preguntasCollection.document(docId).get());
            }

            Task<List<DocumentSnapshot>> combinedTask = Tasks.whenAllSuccess(tasks);
            combinedTask.addOnSuccessListener(new OnSuccessListener<List<DocumentSnapshot>>() {
                @Override
                public void onSuccess(List<DocumentSnapshot> documentSnapshots) {
                    List<QuestionModoCompeticion> questions = new ArrayList<>();
                    for (DocumentSnapshot snapshot : documentSnapshots) {
                        // Assuming the document can be directly converted to QuestionModoCompeticion
                        questions.add(snapshot.toObject(QuestionModoCompeticion.class));
                    }
                    callback.onQuestionsFetched(questions);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Error fetching questions by order: " + e.getMessage());
                    callback.onQuestionsFetched(new ArrayList<>()); // return an empty list to indicate a failure
                }
            });
        }


//        private void createShuffledOrder(DocumentReference shuffledOrderRef, QuestionsFetchCallback callback) {
//                Log.d("QuestionFlow", "Entered <createShuffledOrder>");
//
//                // Step 1: Fetch all questions
//                fetchShuffledQuestions(new ArrayList<>(), new QuestionsFetchCallback() {
//                    @Override
//                    public void onQuestionsFetched(List<QuestionModoCompeticion> allQuestions) {
//
//                        // Step 2: Shuffle the fetched questions
//                        Collections.shuffle(allQuestions);
//                        List<String> initialShuffledOrder = new ArrayList<>();
//                        for (QuestionModoCompeticion q : allQuestions) {
//                            initialShuffledOrder.add(q.getNUMBER());
//                        }
//
//                        Log.d("QuestionFlow", "Initial shuffled order created with size: " + initialShuffledOrder.size());
//
//                        Map<String, Object> data = new HashMap<>();
//                        data.put("shuffledOrder", initialShuffledOrder);
//
//                        // Step 3: Save shuffled order to Firestore
//                        shuffledOrderRef.set(data).addOnSuccessListener(new OnSuccessListener<Void>() {
//                            @Override
//                            public void onSuccess(Void aVoid) {
//                                Log.d("QuestionFlow", "Shuffled order saved successfully.");
//                                fetchShuffledBatchQuestions(callback); // Starting from batch 1
//                            }
//                        }).addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Log.e("QuestionFlow", "Error saving shuffled order: ", e);
//                                callback.onError(e);
//                            }
//                        });
//                    }
//
//                    @Override
//                    public void onError(Exception e) {
//                        Log.e("QuestionFlow", "Error fetching all questions for shuffled order: ", e);
//                        callback.onError(e);
//                    }
//                });
//            }

        void setCurrentBatchForUser(String userId, int nextBatch) {
            if (currentUser != null) {
                DatabaseReference userRef = database.getReference("Users").child(currentUser.getUid());
                userRef.child("currentBatch").setValue(nextBatch);
            }
        }

       public interface CurrentBatchCallback {
            void onBatchRetrieved(int currentBatch);
            void onError(Exception e);
        }

       public void getCurrentBatchForUser(CurrentBatchCallback callback) {
                if (currentUser != null) {
                    DatabaseReference userRef = database.getReference("Users").child(currentUser.getUid());
                    userRef.child("currentBatch").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                Integer batchNumber = dataSnapshot.getValue(Integer.class);
                                if (batchNumber != null) {
                                    callback.onBatchRetrieved(batchNumber);
                                } else {
                                    callback.onError(new Exception("Batch number is not valid."));
                                }
                            } else {
                                // If no batch exists for the user, assign a random batch.
                                int randomBatch = new Random().nextInt(TOTAL_BATCHES) + 1;
                                setCurrentBatchForUser(currentUser.getUid(), randomBatch);
                                callback.onBatchRetrieved(randomBatch);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            callback.onError(databaseError.toException());
                        }
                    });
                } else {
                    callback.onError(new Exception("User is not authenticated."));
                }
            }

        public void moveToNextBatch() {
            if (currentUser != null) {
                String userId = currentUser.getUid();
                DatabaseReference userRef = database.getReference("Users").child(userId);

                userRef.child("currentBatch").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int currentBatch = dataSnapshot.getValue(Integer.class);
                        getTotalBatches(new BatchCountCallback() {
                            @Override
                            public void onBatchCountRetrieved(int totalCount) {
                                int nextBatch = (currentBatch >= totalCount) ? 1 : currentBatch + 1;
                                setCurrentBatchForUser(userId, nextBatch);
                            }

                            @Override
                            public void onError(Exception e) {
                                // Handle error
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle error
                    }
                });
            }
        }

        public void assignBatchForNewUser() {
                if (currentUser != null) {
                    String userId = currentUser.getUid();
                    DatabaseReference userRef = database.getReference("Users").child(userId);

                    userRef.child("currentBatch").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (!dataSnapshot.exists()) {
                                // The user does not have a batch assigned.
                                getTotalBatches(new BatchCountCallback() {
                                    @Override
                                    public void onBatchCountRetrieved(int totalCount) {
                                        int randomBatch = new Random().nextInt(totalCount) + 1;
                                        setCurrentBatchForUser(userId, randomBatch);
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        // Handle error
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // Handle error
                        }
                    });
                }
            }

        public interface BatchCountCallback {
            void onBatchCountRetrieved(int count);
            void onError(Exception e);
        }

        public void getTotalBatches(BatchCountCallback callback) {
            DatabaseReference metadataRef = database.getReference("Metadata");
            metadataRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    int count = (int) dataSnapshot.getChildrenCount();
                    callback.onBatchCountRetrieved(count);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    callback.onError(databaseError.toException());
                }
            });
        }

        private void fetchShuffledQuestions(List<String> questionIds, QuestionsFetchCallback callback) {
            List<QuestionModoCompeticion> questions = new ArrayList<>();
            List<String> questionsToFetch = new ArrayList<>(questionIds);

            fetchQuestionsInChunks(questionsToFetch, questions, callback);
        }

        private void fetchQuestionsInChunks(List<String> questionsToFetch, List<QuestionModoCompeticion> questions, QuestionsFetchCallback callback) {
            if (questionsToFetch.isEmpty()) {
                callback.onQuestionsFetched(questions);
                return;
            }

            int endIndex = Math.min(questionsToFetch.size(), MAX_EXCLUSION_CHUNK_SIZE);
            List<String> currentChunk = questionsToFetch.subList(0, endIndex);

            firestore.collection("PREGUNTAS")
                    .whereIn("NUMBER", currentChunk)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                QuestionModoCompeticion question = parseFirestoreDocument(document);
                                if (question != null) {
                                    questions.add(question);
                                }
                            }

                            questionsToFetch.removeAll(currentChunk);
                            fetchQuestionsInChunks(questionsToFetch, questions, callback);
                        } else {
                            callback.onError(new Exception("Failed to fetch questions from Firestore."));
                        }
                    });
        }

        public void updateLocalAndDatabaseWithNewQuestions(List<QuestionModoCompeticion> newQuestions, FirestoreQuestionManager.QuestionsFetchCallback callback)  {
        if (currentUser != null) {
                String userId = currentUser.getUid();
                DatabaseReference userQuestionsRef = database.getReference("UserQuestions").child(userId);
                DatabaseReference fetchedQuestionsByUserRef = userQuestionsRef.child("FetchedQuestionsByUser");

                Map<String, Object> updates = new HashMap<>();
                for (QuestionModoCompeticion question : newQuestions) {
                    if (question == null || question.getNUMBER() == null) continue;
                    updates.put(question.getNUMBER(), true);
                }

                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US);
                updates.put("timestamp", sdf.format(new Date()));

                fetchedQuestionsByUserRef.setValue(updates).addOnSuccessListener(aVoid -> {
                    dataSource.open();

                    // Retrieve the last 5 questions
                    List<QuestionModoCompeticion> lastFiveQuestions = dataSource.getLastFiveQuestions();

                    // Delete all used questions
                    dataSource.deleteUsedQuestions();

                    // Insert the last 5 questions back to the database
                    for (QuestionModoCompeticion question : lastFiveQuestions) {
                        dataSource.insertOrUpdateQuestion(question);
                    }

                    // Insert or update the new questions
                    for (QuestionModoCompeticion question : newQuestions) {
                        dataSource.insertOrUpdateQuestion(question);
                    }

                    dataSource.close();
                    callback.onQuestionsFetched(newQuestions);

                }).addOnFailureListener(callback::onError);
            } else {
                callback.onError(new Exception("User is not authenticated."));
            }
        }

        public List<QuestionModoCompeticion> getLastFiveUnusedQuestions() {
            List<QuestionModoCompeticion> lastFiveQuestions = new ArrayList<>();
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            String whereClause = QuestionDatabaseHelperII.COLUMN_USED + " = 0";
            Cursor cursor = db.query(QuestionDatabaseHelperII.TABLE_QUESTIONS, null, whereClause, null, null, null, QuestionDatabaseHelperII.COLUMN_ID + " DESC", "5");

            while (cursor.moveToNext()) {
                String question = cursor.getString(cursor.getColumnIndexOrThrow(QuestionDatabaseHelperII.COLUMN_QUESTION));
                String optionA = cursor.getString(cursor.getColumnIndexOrThrow(QuestionDatabaseHelperII.COLUMN_OPTION_A));
                String optionB = cursor.getString(cursor.getColumnIndexOrThrow(QuestionDatabaseHelperII.COLUMN_OPTION_B));
                String optionC = cursor.getString(cursor.getColumnIndexOrThrow(QuestionDatabaseHelperII.COLUMN_OPTION_C));
                String answer = cursor.getString(cursor.getColumnIndexOrThrow(QuestionDatabaseHelperII.COLUMN_ANSWER));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(QuestionDatabaseHelperII.COLUMN_CATEGORY));
                String image = cursor.getString(cursor.getColumnIndexOrThrow(QuestionDatabaseHelperII.COLUMN_IMAGE));
                String number = cursor.getString(cursor.getColumnIndexOrThrow(QuestionDatabaseHelperII.COLUMN_NUMBER));

                // Create the QuestionModoCompeticion object and add to the list
                QuestionModoCompeticion questionObj = new QuestionModoCompeticion(question, optionA, optionB, optionC, answer, category, image, number);
                lastFiveQuestions.add(questionObj);
            }
            cursor.close();
            db.close();

            return lastFiveQuestions;
        }

        public void checkIfShouldMoveToNextBatch(MoveToNextBatchCallback callback) {
            if (currentUser != null) {
                String userId = currentUser.getUid();
                DatabaseReference userRef = database.getReference("Users").child(userId);

                // Check if the user has a current batch assigned
                userRef.child("currentBatch").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            // The user does not have a batch assigned, so they should move
                            callback.onShouldMove();
                        } else {
                            // This is where you check if the current batch is exhausted.
                            // For this example, I assume if the currentBatch exists, then user doesn't need to move. Adjust as needed.
                            callback.onShouldNotMove();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle error - you might want to add an error method in the callback
                    }
                });
            } else {
                // Handle scenario when currentUser is null - maybe an error callback or default behavior
            }
        }

        public interface MoveToNextBatchCallback {
            void onShouldMove();

            void onShouldNotMove();
        }


        private QuestionModoCompeticion parseFirestoreDocument(QueryDocumentSnapshot document) {
            String question = document.getString("QUESTION");
            String optionA = document.getString("OPTION A");
            String optionB = document.getString("OPTION B");
            String optionC = document.getString("OPTION C");
            String answer = document.getString("ANSWER");
            String category = document.getString("CATEGORY");
            String imageUrl = document.getString("IMAGE");
            String number = document.getString("NUMBER");

            if (question == null || optionA == null || optionB == null || optionC == null || answer == null || category == null || imageUrl == null || number == null) {
                return null;
            }

            // Parsing and creating the QuestionModoCompeticion object
            return new QuestionModoCompeticion(question, optionA, optionB, optionC, answer, category, imageUrl, number);
        }
    }
