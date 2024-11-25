package com.iniciativaselebi.afrikanaone;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class JsonQuestionManager {
    private FirebaseFirestore db;
    private List<JsonQuestion> allQuestions;
    private Set<Integer> shownQuestionIds;
    private Context context;  // Class member to hold the Context

    public JsonQuestionManager(Context context) {
        this.context = context;  // Assign the context passed to the constructor
        db = FirebaseFirestore.getInstance();
        allQuestions = new ArrayList<>();
        shownQuestionIds = new HashSet<>();
        loadQuestionsFromFirestore();
        loadShownQuestionIds();
    }

    private void loadQuestionsFromFirestore() {
        db.collection("SINGLEMODEQUESTIONS")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            JsonQuestion question = document.toObject(JsonQuestion.class);
                            allQuestions.add(question);
                        }
                        Collections.shuffle(allQuestions); // Shuffle the questions to randomize order
                    } else {
                        Log.w("JsonQuestionManager", "Error getting documents.", task.getException());
                    }
                });
    }


    public int getQuestionCount() {
        return allQuestions.size();
    }

    public List<JsonQuestion> getRandomQuestions(int count) {
        Collections.shuffle(allQuestions);
        List<JsonQuestion> limitedList = new ArrayList<>();
        for (int i = 0; i < Math.min(count, allQuestions.size()); i++) {
            limitedList.add(allQuestions.get(i));
        }
        return limitedList;
    }

    private void loadShownQuestionIds() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("QuestionPrefs", Context.MODE_PRIVATE);
        String shownIdsJson = sharedPreferences.getString("ShownQuestions", "");
        if (!shownIdsJson.isEmpty()) {
            shownQuestionIds = new Gson().fromJson(shownIdsJson, new TypeToken<Set<Integer>>(){}.getType());
        } else {
            shownQuestionIds = new HashSet<>();
        }
    }

    public void resetShownQuestions() {
        shownQuestionIds.clear();
        SharedPreferences sharedPreferences = context.getSharedPreferences("QuestionPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("ShownQuestions", "");
        editor.apply();
    }
}
