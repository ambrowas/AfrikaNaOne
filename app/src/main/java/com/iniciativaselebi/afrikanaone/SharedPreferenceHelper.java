package com.iniciativaselebi.afrikanaone;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class SharedPreferenceHelper {
    private final SharedPreferences preferences;
    public static final String PREFS_NAME = "quiz_prefs";
    public static final String QUESTIONS_KEY = "questions";
    public static final String USED_QUESTIONS_KEY = "used_questions";
    public static final String BATCH_INDEX_KEY = "batch_index";

    public SharedPreferenceHelper(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<JsonQuestion> getQuestions() {
        String questionsJson = preferences.getString(QUESTIONS_KEY, null);
        if (questionsJson != null) {
            Type questionListType = new TypeToken<List<JsonQuestion>>() {}.getType();
            return new Gson().fromJson(questionsJson, questionListType);
        }
        return new ArrayList<>();
    }

    public void saveQuestions(List<JsonQuestion> questionList) {
        String questionsJson = new Gson().toJson(questionList);
        preferences.edit().putString(QUESTIONS_KEY, questionsJson).apply();
        Log.d("batchFlow", "Saved Questions to SharedPreferences, Size: " + questionList.size());
    }

    public static int getBatchIndex(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int batchIndex = preferences.getInt(BATCH_INDEX_KEY, 0);
        return batchIndex;
    }

    public static void saveBatchIndex(Context context, int batchIndex) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().putInt(BATCH_INDEX_KEY, batchIndex).apply();
    }

    public static Set<String> getUsedQuestions(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> usedQuestions = new HashSet<>(preferences.getStringSet(USED_QUESTIONS_KEY, new HashSet<>()));
        Log.d("batchFlow", "Getting Used Questions from SharedPreferences, Size: " + usedQuestions.size());
        return usedQuestions;
    }

    public static void saveUsedQuestions(Context context, Set<String> usedQuestions) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().putStringSet(USED_QUESTIONS_KEY, usedQuestions).apply();
        Log.d("batchFlow", "Saved Used Questions to SharedPreferences, Count: " + usedQuestions.size());
    }

    public static void clearUsedQuestions(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().remove(USED_QUESTIONS_KEY).apply();
        Log.d("batchFlow", "Cleared Used Questions from SharedPreferences.");
    }

    public void clearQuestions() {
        preferences.edit().remove(QUESTIONS_KEY).apply();
        Log.d("batchFlow", "Cleared Questions from SharedPreferences.");
    }
}
