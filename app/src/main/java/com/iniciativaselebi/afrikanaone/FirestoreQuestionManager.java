package com.iniciativaselebi.afrikanaone;

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

import java.util.ArrayList;
import java.util.List;
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
            CollectionReference preguntasCollection = firestore.collection("QUESTIONS");

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
                                    if (totalCount > 0) { // Ensure totalCount is positive
                                        int randomBatch = new Random().nextInt(totalCount) + 1;
                                        Log.d("[MyApp]FetchAndSaveFlow", "New batch assigned for user: " + userId + " - Batch: " + randomBatch);
                                        setCurrentBatchForUser(userId, randomBatch);
                                        // Notify the callback that the batch has been assigned
                                        callback.onBatchAssigned(randomBatch);
                                    } else {
                                        Log.e("[MyApp]FetchAndSaveFlow", "No available batches to assign.");
                                        callback.onError(new Exception("No available batches to assign."));
                                    }
                                }

                                @Override
                                public void onError(Exception e) {
                                    Log.e("[MyApp]FetchAndSaveFlow", "Error retrieving total batch count.", e);
                                    // Notify the callback about the error
                                    callback.onError(e);
                                }
                            });
                        } else {
                            // Batch already exists for the user, use existing batch number
                            int existingBatch = dataSnapshot.getValue(Integer.class);
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
                // No current user found, handle this case with an error callback
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

        public void prepareNextBatchIfRequired(String userId, BatchPreparationCallback callback) {
            DatabaseReference userRef = database.getReference("user").child(userId).child("currentBatch");

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot currentBatchSnapshot) {
                    if (currentBatchSnapshot.exists()) {
                        int currentBatchId = currentBatchSnapshot.getValue(Integer.class);

                        // Log the current batch as completed
                        logCompletedBatch(userId, currentBatchId);

                        // Retrieve total batch count to determine the next batch ID
                        getTotalBatches(new BatchCountCallback() {
                            @Override
                            public void onBatchCountRetrieved(int totalCount) {
                                int newBatchId = currentBatchId + 1;
                                if (newBatchId > totalCount) {
                                    newBatchId = 1; // Wrap around to the first batch if the last one is reached
                                }

                                // Declare newBatchIdFinal as final so that it can be used within the lambda expression
                                final int newBatchIdFinal = newBatchId;

                                // Update the user's current batch ID in the database and proceed with preparation
                                userRef.setValue(newBatchIdFinal).addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Log.d("[MyApp]FetchAndSaveFlow", "Set new batch for user: " + userId + " to batch number: " + newBatchIdFinal);
                                        // Since newBatchIdFinal is effectively final, it can be used here.
                                        // Other operations or notifications can go here
                                    } else {
                                        Log.e("[MyApp]FetchAndSaveFlow", "Failed to set new batch for user: " + userId, task.getException());
                                        callback.onBatchPreparationFailed(task.getException());
                                    }
                                });
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e("[MyApp]FetchAndSaveFlow", "Error retrieving total batch count.", e);
                                callback.onBatchPreparationFailed(e);
                            }
                        });
                    } else {
                        Log.e("[MyApp]FetchAndSaveFlow", "No current batch found for user: " + userId);
                        // The process for assigning a new user's first batch could be initiated here as well.
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("[MyApp]FetchAndSaveFlow", "Database error occurred while checking current batch.", databaseError.toException());
                    callback.onBatchPreparationFailed(databaseError.toException());
                }
            });
        }

        interface BatchPreparationCallback {
                void onBatchPreparationComplete();
                void onBatchPreparationFailed(Exception e);
            }

        public void updateLocalDatabaseWithNewQuestions(List<QuestionModoCompeticion> newQuestions, int completedBatchId, QuestionsFetchCallback callback) {
            Log.d("[MyApp]FetchAndSaveFlow", "Starting update of local database with new questions.");

            if (currentUser != null) {
                Log.d("[MyApp]FetchAndSaveFlow", "Current user's UID: " + currentUser.getUid());
            } else {
                Log.d("[MyApp]FetchAndSaveFlow", "Current user is null.");
            }
            Log.d("[MyApp]FetchAndSaveFlow", "Batch ID to be marked as completed: " + completedBatchId);

            dataSource.open(); // Open the data source to update the database

            try {
                Log.d("[MyApp]FetchAndSaveFlow", "Local data source opened successfully.");

                List<QuestionModoCompeticion> lastFiveQuestions = getLastFiveUnusedQuestions();
                Log.d("[MyApp]FetchAndSaveFlow", "Retrieved last five unused questions. Count: " + lastFiveQuestions.size());

                dataSource.deleteUsedQuestions();
                Log.d("[MyApp]FetchAndSaveFlow", "Deleted used questions from local database.");

                // Insert or update last five unused questions after checking for duplicates
                for (QuestionModoCompeticion question : lastFiveQuestions) {
                    if (!dataSource.isQuestionInDatabase(question.getNUMBER())) {
                        dataSource.insertOrUpdateQuestion(question);
                    }
                }
                Log.d("[MyApp]FetchAndSaveFlow", "Processed last five unused questions in local database.");

                // Insert or update new questions after checking for duplicates
                for (QuestionModoCompeticion question : newQuestions) {
                    if (!dataSource.isQuestionInDatabase(question.getNUMBER())) {
                        dataSource.insertOrUpdateQuestion(question);
                    }
                }
                Log.d("[MyApp]FetchAndSaveFlow", "Inserted new questions into local database.");

                callback.onQuestionsUpdated(newQuestions); // Notify callback that questions have been updated
                Log.d("[MyApp]FetchAndSaveFlow", "Callback notified that questions have been updated.");

            } catch (Exception e) {
                Log.e("[MyApp]FetchAndSaveFlow", "Error when updating local database with new questions.", e);
                callback.onError(e); // Notify callback about the error
            } finally {
                dataSource.close(); // Close the data source regardless of previous success or failure
                Log.d("[MyApp]FetchAndSaveFlow", "Local data source has been closed.");
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


        public void logCompletedBatch(String userId, int completedBatch) {
            DatabaseReference userRef = database.getReference("user").child(userId).child("CompletedBatch");
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String existingBatches = dataSnapshot.exists() ? dataSnapshot.getValue(String.class) : "";
                    String updatedBatches = existingBatches.isEmpty() ? String.valueOf(completedBatch) : existingBatches + ", " + completedBatch;

                    userRef.setValue(updatedBatches).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("[MyApp]CompletedBatch", "Batch " + completedBatch + " added to completed list for user: " + userId);
                        } else {
                            Log.e("[MyApp]CompletedBatch", "Failed to update completed batch list for user: " + userId, task.getException());
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("[MyApp]CompletedBatch", "Error updating completed batch list for user: " + userId, databaseError.toException());
                }
            });
        }

    }
