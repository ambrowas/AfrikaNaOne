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


        public interface QuestionsFetchCallback {
            void onSuccess(List<QuestionModoCompeticion> fetchedQuestions);

            void onQuestionsFetched(List<QuestionModoCompeticion> fetchedQuestions); // Call this when questions are initially fetched
            void onQuestionsUpdated(List<QuestionModoCompeticion> updatedQuestions); // Call this after saving fetched questions in the database
            void onError(Exception e);
        }

        public FirestoreQuestionManager(Context context) {
                this.context = context;
                this.dbHelper = new QuestionDatabaseHelperII(this.context);
                dataSource = new QuestionDataSource(this.context);
                firestore = FirebaseFirestore.getInstance();
                mAuth = FirebaseAuth.getInstance();
                currentUser = mAuth.getCurrentUser();
            }

        public void fetchShuffledBatchQuestions(final QuestionsFetchCallback callback) {
            Log.d(TAG, "[MyApp] Starting the process to fetch questions...");

            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser mUser = mAuth.getCurrentUser();

            if (mUser == null) {
                Log.e(TAG, "[MyApp] User is not authenticated. Cannot fetch questions.");
                callback.onError(new Exception("User is not authenticated."));
                return;
            }

            String userId = mUser.getUid();
            FirebaseDatabase rtdb = FirebaseDatabase.getInstance();
            DatabaseReference batchRef = rtdb.getReference("user").child(userId).child("currentBatch");

            batchRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Long currentBatch = dataSnapshot.getValue(Long.class);
                        Log.d(TAG, "[MyApp] Successfully fetched currentBatch: " + currentBatch + " from Realtime Database.");

                        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                        DocumentReference batchReference = firestore.collection("Metadata").document("Batch_" + currentBatch);

                        batchReference.get().addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists() && documentSnapshot.contains("shuffledOrder")) {
                                List<String> shuffledOrder = (List<String>) documentSnapshot.get("shuffledOrder");
                                Log.d(TAG, "[MyApp] Fetched shuffledOrder: " + shuffledOrder);
                                fetchQuestionsFromShuffledOrder(shuffledOrder, callback);
                            } else {
                                Log.e(TAG, "[MyApp] Metadata document does not exist or shuffledOrder is missing.");
                                callback.onError(new Exception("Metadata document does not exist or shuffledOrder is missing."));
                            }
                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "[MyApp] Failed fetching shuffledOrder for currentBatch from Firestore. Error: " + e.getMessage());
                            callback.onError(e);
                        });
                    } else {
                        assignBatchForNewUser(new BatchAssignmentCallback() {
                            @Override
                            public void onBatchAssigned(int batchNumber) {
                                fetchShuffledBatchQuestions(callback);
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "[MyApp] Failed to assign a new batch for the user. Error: " + e.getMessage());
                                callback.onError(e);
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "[MyApp] Failed fetching currentBatch from Realtime Database. Error: " + databaseError.getMessage());
                    callback.onError(new Exception(databaseError.getMessage()));
                }
            });
        }

        private void fetchQuestionsFromShuffledOrder(List<String> shuffledOrder, final QuestionsFetchCallback callback) {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            CollectionReference preguntasCollection = firestore.collection("PREGUNTAS");

            Log.d(TAG, "[MyApp] FetchAndSaveFlow: Starting to fetch questions for shuffledOrder: " + shuffledOrder);

            List<QuestionModoCompeticion> fetchedQuestions = new ArrayList<>();
            AtomicInteger counter = new AtomicInteger();

            for (String questionId : shuffledOrder) {
                preguntasCollection.document(questionId).get().addOnSuccessListener(documentSnapshot -> {
                    QuestionModoCompeticion question = documentSnapshot.toObject(QuestionModoCompeticion.class);
                    if (question != null) {
                        fetchedQuestions.add(question);
                        Log.d(TAG, "[MyApp] FetchAndSaveFlow: Question fetched: " + questionId);
                    }

                    if (counter.incrementAndGet() == shuffledOrder.size()) {
                        Log.d(TAG, "[MyApp] FetchAndSaveFlow: All questions fetched. Total: " + fetchedQuestions.size());
                        callback.onQuestionsFetched(fetchedQuestions);
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "[MyApp] FetchAndSaveFlow: Failed to fetch question: " + questionId, e);
                    callback.onError(e);
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
                    Log.e(TAG, "[MyApp]Error fetching questions by order: " + e.getMessage());
                    callback.onQuestionsFetched(new ArrayList<>()); // return an empty list to indicate a failure
                }
            });
        }


        void setCurrentBatchForUser(String userId, int nextBatch) {
            if (userId != null) {
                DatabaseReference userRef = database.getReference("user").child(userId);
                Log.d("[MyApp]FetchAndSaveFlow", "Attempting to set current batch for user: " + userId + " to batch number: " + nextBatch);

                // Set the value and add a completion listener to handle success or failure
                userRef.child("currentBatch").setValue(nextBatch, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        if (databaseError != null) {
                            // Handle the error scenario, log the error
                            Log.e("[MyApp]FetchAndSaveFlow", "Error setting current batch for user: " + userId, databaseError.toException());
                        } else {
                            // The batch was set successfully, log this outcome
                            Log.d("[MyApp]FetchAndSaveFlow", "Successfully set current batch for user: " + userId + " to batch number: " + nextBatch);
                        }
                    }
                });
            } else {
                Log.e("[MyApp]FetchAndSaveFlow", "Cannot set current batch. UserId is null.");
            }
        }

        public interface CurrentBatchCallback {
            void onBatchRetrieved(int currentBatch);
            void onError(Exception e);
        }

        public void getCurrentBatchForUser(CurrentBatchCallback callback) {
                if (currentUser != null) {
                    DatabaseReference userRef = database.getReference("user").child(currentUser.getUid());
                    Log.d("FetchAndSaveFlow", "Fetching current batch for user: " + currentUser.getUid());
                    userRef.child("currentBatch").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                Integer batchNumber = dataSnapshot.getValue(Integer.class);
                                if (batchNumber != null) {
                                    Log.d("FetchAndSaveFlow", "Current batch retrieved: " + batchNumber);
                                    callback.onBatchRetrieved(batchNumber);
                                } else {
                                    Log.e("FetchAndSaveFlow", "Batch number is not valid.");
                                    callback.onError(new Exception("Batch number is not valid."));
                                }
                            } else {
                                // Batch not found for the user, handle accordingly (but do not assign a new batch here)
                                Log.e("FetchAndSaveFlow", "No current batch found for user.");
                                callback.onError(new Exception("No current batch found for user."));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e("FetchAndSaveFlow", "Failed to fetch current batch for user.", databaseError.toException());
                            callback.onError(databaseError.toException());
                        }
                    });
                } else {
                    Log.e("FetchAndSaveFlow", "User is not authenticated.");
                    callback.onError(new Exception("User is not authenticated."));
                }
            }


        public void assignBatchForNewUser(BatchAssignmentCallback callback) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();
                Log.d("[MyApp]FetchAndSaveFlow", "Assigning batch for new user: " + userId);
                DatabaseReference userRef = database.getReference("user").child(userId);

                userRef.child("currentBatch").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            getTotalBatches(new BatchCountCallback() {
                                @Override
                                public void onBatchCountRetrieved(int totalCount) {
                                    int randomBatch = new Random().nextInt(totalCount) + 1;
                                    Log.d("[MyApp]FetchAndSaveFlow", "New batch assigned for user: " + userId + " - Batch: " + randomBatch);
                                    setCurrentBatchForUser(userId, randomBatch);
                                    // Notify the callback that the batch has been assigned
                                    callback.onBatchAssigned(randomBatch);
                                }

                                @Override
                                public void onError(Exception e) {
                                    Log.e("[MyApp]FetchAndSaveFlow", "Error retrieving total batch count.", e);
                                    // Notify the callback about the error
                                    callback.onError(e);
                                }
                            });
                        } else {
                            // Batch already exists for the user, perhaps call onBatchAssigned with the existing batch number
                            // You will need to fetch the existing batch number from dataSnapshot if needed.
                            int existingBatch = dataSnapshot.getValue(Integer.class); // assuming it's stored as an Integer
                            callback.onBatchAssigned(existingBatch);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("[MyApp]FetchAndSaveFlow", "Failed to assign batch for new user.", databaseError.toException());
                        // Notify the callback about the cancellation/error
                        callback.onError(databaseError.toException());
                    }
                });
            } else {
                // No current user found, handle this case, perhaps with an error callback
                callback.onError(new Exception("No current user found."));
            }
        }

        public interface BatchAssignmentCallback {
            void onBatchAssigned(int batchNumber);
            void onError(Exception e);
        }

        public interface BatchCountCallback {
            void onBatchCountRetrieved(int count);
            void onError(Exception e);
        }

        public void getTotalBatches(BatchCountCallback callback) {
            // Get a reference to the Firestore Metadata collection
            CollectionReference metadataRef = FirebaseFirestore.getInstance().collection("Metadata");
            Log.d("[MyApp]FetchAndSaveFlow", "Getting total batches from Metadata collection.");

            metadataRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        int count = task.getResult().size();
                        Log.d("[MyApp]FetchAndSaveFlow", "Total batches retrieved: " + count);
                        callback.onBatchCountRetrieved(count);
                    } else {
                        Log.e("[MyApp]FetchAndSaveFlow", "Error getting total batches.", task.getException());
                        callback.onError(task.getException());
                    }
                }
            });
        }

        public void prepareNextBatchIfRequired(String userId, BatchPreparationCallback callback)  {
                DatabaseReference userRef = database.getReference("user").child(userId).child("currentBatch");

                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot currentBatchSnapshot) {
                        if (currentBatchSnapshot.exists()) {
                            int currentBatchId = currentBatchSnapshot.getValue(Integer.class);

                            // Retrieve total batch count to determine the next batch ID
                            getTotalBatches(new BatchCountCallback() {
                                @Override
                                public void onBatchCountRetrieved(int totalCount) {
                                    int nextBatchId = currentBatchId + 1;
                                    // Wrap around if the nextBatchId exceeds total batches
                                    if (nextBatchId > totalCount) {
                                        nextBatchId = 1;
                                    }

                                    // final variable for use in lambda
                                    final int newBatchId = nextBatchId;

                                    // Update the user's current batch ID in the database
                                    userRef.setValue(newBatchId).addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Log.d("[MyApp]FetchAndSaveFlow", "Set new batch for user: " + userId + " to batch number: " + newBatchId);
                                        } else {
                                            Log.e("[MyApp]FetchAndSaveFlow", "Failed to set new batch for user: " + userId, task.getException());
                                        }
                                    });
                                }

                                @Override
                                public void onError(Exception e) {
                                    Log.e("[MyApp]FetchAndSaveFlow", "Error retrieving total batch count.", e);
                                }
                            });

                        } else {
                            Log.e("[MyApp]FetchAndSaveFlow", "No current batch found for user: " + userId);
                            // Possibly initialize the user's batch here
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("[MyApp]FetchAndSaveFlow", "Database error occurred while checking current batch.", databaseError.toException());
                    }
                });
            }

        interface BatchPreparationCallback {
                void onBatchPreparationComplete();
                void onBatchPreparationFailed(Exception e);
            }

        private void fetchShuffledQuestions(List<String> questionIds, QuestionsFetchCallback callback) {
            Log.d("[MyApp]FetchAndSaveFlow", "Fetching shuffled questions.");
            List<QuestionModoCompeticion> questions = new ArrayList<>();
            List<String> questionsToFetch = new ArrayList<>(questionIds);

            fetchQuestionsInChunks(questionsToFetch, questions, callback);
        }

        private void fetchQuestionsInChunks(List<String> questionsToFetch, List<QuestionModoCompeticion> questions, QuestionsFetchCallback callback) {
            Log.d("[MyApp]FetchAndSaveFlow", "Fetching questions in chunks.");

            if (questionsToFetch.isEmpty()) {
                Log.d("[MyApp]FetchAndSaveFlow", "All chunks have been fetched.");
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

                            Log.d("[MyApp]FetchAndSaveFlow", "Fetched chunk of questions. Remaining: " + questionsToFetch.size());

                            questionsToFetch.removeAll(currentChunk);
                            fetchQuestionsInChunks(questionsToFetch, questions, callback);
                        } else {
                            Log.e("[MyApp]FetchAndSaveFlow", "Failed to fetch questions chunk.", task.getException());
                            callback.onError(new Exception("Failed to fetch questions from Firestore."));
                        }
                    });
        }

        public void updateLocalDatabaseWithNewQuestions(List<QuestionModoCompeticion> newQuestions, QuestionsFetchCallback callback) {
            Log.d("[MyApp]FetchAndSaveFlow", "[MyApp]Updating local database with new questions.");

            dataSource.open();

            try {
                Log.d("[MyApp]FetchAndSaveFlow", "[MyApp]Local data source opened.");

                List<QuestionModoCompeticion> lastFiveQuestions = getLastFiveUnusedQuestions();
                dataSource.deleteUsedQuestions();

                // Ensure that only unused questions are preserved.
                if (lastFiveQuestions.size() > 0) {
                    for (QuestionModoCompeticion question : lastFiveQuestions) {
                        dataSource.insertOrUpdateQuestion(question);
                    }
                }

                // Insert the new questions into the database.
                for (QuestionModoCompeticion question : newQuestions) {
                    dataSource.insertOrUpdateQuestion(question);
                }

                Log.d("[MyApp]FetchAndSaveFlow", "[MyApp]Local database updated with new questions.");
                callback.onQuestionsUpdated(newQuestions); // Notify that questions have been updated

            } catch (Exception e) {
                Log.e("[MyApp]FetchAndSaveFlow", "[MyApp]Error updating local database with new questions.", e);
                callback.onError(e); // Notify about the error
            } finally {
                dataSource.close();
                Log.d("[MyApp]FetchAndSaveFlow", "[MyApp]Local database closed.");
            }
        }


        public List<QuestionModoCompeticion> getLastFiveUnusedQuestions() {
            Log.d("[MyApp]FetchAndSaveFlow", "[MyApp]Fetching last five unused questions from local database.");
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
