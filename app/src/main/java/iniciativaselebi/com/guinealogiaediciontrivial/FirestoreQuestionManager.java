package iniciativaselebi.com.guinealogiaediciontrivial;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;


public class FirestoreQuestionManager {
    public static final int BATCH_SIZE = 10; // Number of questions to fetch in each batch
    private Context context;
    private QuestionDataSource dataSource;
    private FirebaseFirestore firestore;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference gameStatsRef;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();

    boolean used = false;
    String userId;

    QuestionDataSource questionDataSource = new QuestionDataSource(context);



    public interface QuestionsFetchCallback {
        void onQuestionsFetched(List<QuestionModoCompeticion> newQuestions);
        void onError(Exception e);
    }

    private FirebaseUser currentUser;

    public FirestoreQuestionManager(Context context) {
        this.context = context;
        dataSource = new QuestionDataSource(context);
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser(); // Initialize currentUser here

        if (currentUser != null) {
            userId = currentUser.getUid();  // <-- Here's the initialization
        }
    }

    public void fetchQuestionsBatch(QuestionsFetchCallback callback) {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userQuestionsRef = database.getReference("UserQuestions").child(userId);
            DatabaseReference fetchedQuestionsByUserRef = userQuestionsRef.child("FetchedQuestionsByUser");

            fetchedQuestionsByUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<String> fetchedQuestionIds = new ArrayList<>();
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        String questionId = childSnapshot.getKey();
                        if (!"timestamp".equals(questionId)) {  // ensure the timestamp isn't added to the exclusion list
                            fetchedQuestionIds.add(questionId);
                        }
                    }
                    fetchNewQuestionsExcluding(fetchedQuestionIds, callback);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    callback.onError(databaseError.toException());
                }
            });
        } else {
            callback.onError(new Exception("User is not authenticated."));
        }
    }

    private void fetchNewQuestionsExcluding(List<String> exclusionList, QuestionsFetchCallback callback) {
        CollectionReference questionsCollection = firestore.collection("PREGUNTAS");

        // Only take the latest 10 fetched questions for exclusion
        List<String> limitedExclusionList = exclusionList.size() > 10 ?
                exclusionList.subList(exclusionList.size() - 10, exclusionList.size()) :
                exclusionList;

        Query query;
        if (!limitedExclusionList.isEmpty()) {
            query = questionsCollection
                    .orderBy("NUMBER")   // <-- Add this before ordering by "random"
                    .orderBy("random")
                    .whereNotIn("NUMBER", limitedExclusionList)
                    .limit(BATCH_SIZE);
        } else {
            query = questionsCollection
                    .orderBy("NUMBER")   // <-- Add this before ordering by "random"
                    .orderBy("random")
                    .limit(BATCH_SIZE);
        }

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<QuestionModoCompeticion> newQuestions = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    QuestionModoCompeticion question = parseFirestoreDocument(document);
                    newQuestions.add(question);
                }
                updateLocalAndDatabaseWithNewQuestions(newQuestions, callback);
            } else {
                callback.onError(task.getException());
            }
        });
    }


    private void updateLocalAndDatabaseWithNewQuestions(List<QuestionModoCompeticion> newQuestions, QuestionsFetchCallback callback) {

        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userQuestionsRef = database.getReference("UserQuestions").child(userId);
            DatabaseReference fetchedQuestionsByUserRef = userQuestionsRef.child("FetchedQuestionsByUser");

            Map<String, Object> updates = new HashMap<>();

            for (QuestionModoCompeticion question : newQuestions) {
                updates.put(question.getNumber(), true);
            }

            // Add readable timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US);
            String formattedDate = sdf.format(new Date()); // getting current date and time
            updates.put("timestamp", formattedDate);

            fetchedQuestionsByUserRef.setValue(updates).addOnSuccessListener(aVoid -> {
                dataSource.open();

                // Clear out old questions from local SQL database
               dataSource.clearAllQuestions();


                // Store the new batch of questions in local SQL database
                for (QuestionModoCompeticion question : newQuestions) {
                   dataSource.insertQuestion(question);
// Make sure you've initialized and opened questionDataSource
                }
                callback.onQuestionsFetched(newQuestions);
                dataSource.close();
            }).addOnFailureListener(callback::onError);

        }

    }

    private QuestionModoCompeticion parseFirestoreDocument(QueryDocumentSnapshot document) {
        // Parse the document fields and create a QuestionModoCompeticion object
        String question = document.getString("QUESTION");
        String optionA = document.getString("OPTION A");
        String optionB = document.getString("OPTION B");
        String optionC = document.getString("OPTION C");
        String answer = document.getString("ANSWER");
        String category = document.getString("CATEGORY");
        String imageUrl = document.getString("IMAGE");
        String number = document.getString("NUMBER");

        return new QuestionModoCompeticion(question, optionA, optionB, optionC, answer, category, imageUrl, number, used);
    }


}
