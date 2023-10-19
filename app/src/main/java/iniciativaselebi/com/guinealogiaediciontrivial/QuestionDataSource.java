package iniciativaselebi.com.guinealogiaediciontrivial;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class QuestionDataSource {
    private SQLiteDatabase database;
    private QuestionDatabaseHelperII dbHelper;

    public QuestionDataSource(Context context) {
        dbHelper = new QuestionDatabaseHelperII(context);
    }

    public List<QuestionModoCompeticion> fetchRandomUnusedQuestions(int count) {
        List<QuestionModoCompeticion> questions = new ArrayList<>();
        SQLiteDatabase database = dbHelper.getReadableDatabase();

        // Define the query to fetch random questions up to the specified count
        String query = "SELECT * FROM " + QuestionDatabaseHelperII.TABLE_QUESTIONS +
                " ORDER BY RANDOM()" + // Randomize the order
                " LIMIT " + count; // Limit the number of rows

        Cursor cursor = database.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                QuestionModoCompeticion question = cursorToQuestion(cursor);
                questions.add(question);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return questions;
    }


    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public long insertQuestion(QuestionModoCompeticion question)    {
        ContentValues values = new ContentValues();
        values.put(QuestionDatabaseHelperII.COLUMN_QUESTION, question.getQuestion());
        values.put(QuestionDatabaseHelperII.COLUMN_OPTION_A, question.getOptionA());
        values.put(QuestionDatabaseHelperII.COLUMN_OPTION_B, question.getOptionB());
        values.put(QuestionDatabaseHelperII.COLUMN_OPTION_C, question.getOptionC());
        values.put(QuestionDatabaseHelperII.COLUMN_ANSWER, question.getAnswer());
        values.put(QuestionDatabaseHelperII.COLUMN_CATEGORY, question.getCategory());
        values.put(QuestionDatabaseHelperII.COLUMN_IMAGE, question.getImageUrl());
        values.put(QuestionDatabaseHelperII.COLUMN_NUMBER, question.getNumber());

        return database.insert(QuestionDatabaseHelperII.TABLE_QUESTIONS, null, values);
    }

    public boolean isQuestionInDatabase(String questionText) {
        SQLiteDatabase database = dbHelper.getReadableDatabase();

        // Define the query to check if the question with the given text exists
        String query = "SELECT COUNT(*) FROM " + QuestionDatabaseHelperII.TABLE_QUESTIONS +
                " WHERE " + QuestionDatabaseHelperII.COLUMN_QUESTION + " = ?";

        // Use a raw query to execute the count query
        Cursor cursor = database.rawQuery(query, new String[]{questionText});

        boolean questionExists = false;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                questionExists = (count > 0);
            }
            cursor.close();
        }

        // Close the database
        database.close();

        return questionExists;
    }
    private QuestionModoCompeticion cursorToQuestion(Cursor cursor) {
        int questionIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_QUESTION);
        int optionAIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_OPTION_A);
        int optionBIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_OPTION_B);
        int optionCIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_OPTION_C);
        int answerIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_ANSWER);
        int categoryIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_CATEGORY);
        int imageIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_IMAGE);
        int numberIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_NUMBER);
        int usedIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_USED);

        // Check if columns exist in the cursor
        if (questionIndex == -1 || optionAIndex == -1 || optionBIndex == -1 || optionCIndex == -1
                || answerIndex == -1 || categoryIndex == -1 || imageIndex == -1 || numberIndex == -1 || usedIndex == -1) {
            // Handle the case where columns are missing
            return null; // Return null or handle the error as appropriate
        }

        String question = cursor.getString(questionIndex);
        String optionA = cursor.getString(optionAIndex);
        String optionB = cursor.getString(optionBIndex);
        String optionC = cursor.getString(optionCIndex);
        String answer = cursor.getString(answerIndex);
        String category = cursor.getString(categoryIndex);
        String image = cursor.getString(imageIndex);
        String number = cursor.getString(numberIndex);
        boolean used = cursor.getInt(usedIndex) == 1;

        return new QuestionModoCompeticion(question, optionA, optionB, optionC, answer, category, image, number, used);
    }
    public void clearAllQuestions() {
        database.execSQL("DELETE FROM " + QuestionDatabaseHelperII.TABLE_QUESTIONS);
    }

}

