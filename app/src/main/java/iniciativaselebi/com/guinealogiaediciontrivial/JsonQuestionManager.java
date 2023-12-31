package iniciativaselebi.com.guinealogiaediciontrivial;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonQuestionManager {
    private static final String PREFS_NAME = "QuestionPrefs";
    private static final String SHOWN_QUESTIONS_KEY = "ShownQuestions";
    private SharedPreferences sharedPreferences;
    private List<JsonQuestion> allQuestions;
    private Set<Integer> shownQuestionIds;

    private int currentQuestionIndex = -1;
    private boolean allQuestionsShown = false;

    public JsonQuestionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Load all questions from JSON
        allQuestions = loadQuestionsFromJson(context);
        // Shuffle the questions to randomize order
        Collections.shuffle(allQuestions);
        loadShownQuestionIds();
    }

    private List<JsonQuestion> loadQuestionsFromJson(Context context) {
        String json = loadJSONFromAsset(context, "quiz_questions.json");
        Type questionListType = new TypeToken<ArrayList<JsonQuestion>>(){}.getType();
        return new Gson().fromJson(json, questionListType);
    }

    private String loadJSONFromAsset(Context context, String fileName) {
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public JsonQuestion getNextQuestion(PreguntasModoLibre activity) {
        if (allQuestionsShown) {
            // If all questions have been shown, don't show the dialog again
            return null;
        }
        // Get questions that haven't been shown yet
        List<JsonQuestion> availableQuestions = allQuestions.stream()
                .filter(q -> !shownQuestionIds.contains(q.get_id()))
                .collect(Collectors.toList());

        // If no available questions are left, show the dialog and return null
        if (availableQuestions.isEmpty()) {
           activity.showAllQuestionsPlayedDialog();
            allQuestionsShown = true;
            return null;
        }

        // Select a random question from the available ones
        JsonQuestion nextQuestion = availableQuestions.get(new Random().nextInt(availableQuestions.size()));
        shownQuestionIds.add(nextQuestion.get_id());

        Log.d("JsonQuestionManager", "Selected Question ID: " + nextQuestion.get_id());

        saveShownQuestionIds();
        return nextQuestion;
    }

    // Call this method after the dialog is dismissed to reset the game

    private void saveShownQuestionIds() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String shownIdsJson = new Gson().toJson(shownQuestionIds);
        editor.putString(SHOWN_QUESTIONS_KEY, shownIdsJson);
        editor.apply();

        // Log the set of shown question IDs
        Log.d("JsonQuestionManager", "Shown Question IDs: " + shownIdsJson);
    }

    public int getQuestionCount() {
        return allQuestions != null ? allQuestions.size() : 0;
    }

    public List<JsonQuestion> getRandomQuestions(int count) {
        List<JsonQuestion> randomQuestions = new ArrayList<>();
        Collections.shuffle(allQuestions);
        for (int i = 0; i < Math.min(count, allQuestions.size()); i++) {
            randomQuestions.add(allQuestions.get(i));
        }
        return randomQuestions;
    }
    private void loadShownQuestionIds() {
        String shownIdsJson = sharedPreferences.getString(SHOWN_QUESTIONS_KEY, "");
        if (!shownIdsJson.isEmpty()) {
            shownQuestionIds = new Gson().fromJson(shownIdsJson, new TypeToken<Set<Integer>>(){}.getType());
        } else {
            shownQuestionIds = new HashSet<>();
        }
    }

    public void resetShownQuestions() {
        shownQuestionIds.clear();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SHOWN_QUESTIONS_KEY, "");
        editor.apply();
    }


}
