package iniciativaselebi.com.guinealogiaediciontrivial;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
public class QuestionDataSource {
    private SQLiteDatabase database;
    private QuestionDatabaseHelperII dbHelper;
    private static final String TAG = "QuestionDataSource";

    public QuestionDataSource(Context context) {
        dbHelper = QuestionDatabaseHelperII.getInstance(context);
    }

    private void ensureOpen() {
        if (database == null || !database.isOpen()) {
            open();
        }
    }

    public void markRandomQuestionsAsUsed(int count, boolean closeAfterOperation) {
        Log.d(TAG, "Attempting to mark " + count + " random questions as used.");
        ensureOpen();
        List<QuestionModoCompeticion> randomQuestions = fetchRandomUnusedQuestions(count);
        database.beginTransaction();
        try {
            for (QuestionModoCompeticion question : randomQuestions) {
                ContentValues values = new ContentValues();
                values.put(QuestionDatabaseHelperII.COLUMN_USED, 1); // Mark as used
                database.update(QuestionDatabaseHelperII.TABLE_QUESTIONS, values,
                        QuestionDatabaseHelperII.COLUMN_NUMBER + " = ?",
                        new String[]{question.getNUMBER()});
            }
            database.setTransactionSuccessful();
            Log.d(TAG, "Successfully marked " + count + " questions as used.");
        } catch (SQLException e) {
            Log.e(TAG, "Error marking questions as used.", e);
        } finally {
            database.endTransaction();
            if (closeAfterOperation) {
                close();
                Log.d(TAG, "Database closed after marking questions as used.");
            }
        }
    }

    public int getUnusedQuestionsCount() {
        Log.d(TAG, "Fetching the count of unused questions.");
        ensureOpen();
        String query = "SELECT COUNT(*) FROM " + QuestionDatabaseHelperII.TABLE_QUESTIONS +
                " WHERE " + QuestionDatabaseHelperII.COLUMN_USED + " = 0";
        Cursor cursor = database.rawQuery(query, null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
                Log.d(TAG, "Count of unused questions retrieved: " + count);
            }
            cursor.close();
        } else {
            Log.e(TAG, "Failed to fetch the count of unused questions - cursor is null.");
        }
        return count;
    }


    public QuestionModoCompeticion getUnusedQuestion() {
        Log.d(TAG, "Fetching a random unused question.");
        ensureOpen();
        String query = "SELECT * FROM " + QuestionDatabaseHelperII.TABLE_QUESTIONS +
                " WHERE " + QuestionDatabaseHelperII.COLUMN_USED + " = 0" +
                " ORDER BY RANDOM()" +
                " LIMIT 1";
        Cursor cursor = database.rawQuery(query, null);
        QuestionModoCompeticion question = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                question = cursorToQuestion(cursor);
                Log.d(TAG, "A random unused question was successfully fetched.");
            }
            cursor.close();
        } else {
            Log.e(TAG, "Failed to fetch a random unused question - cursor is null.");
        }
        return question;
    }


    public List<QuestionModoCompeticion> fetchRandomUnusedQuestions(int count) {
        Log.d(TAG, "Fetching " + count + " random unused questions.");
        ensureOpen();
        List<QuestionModoCompeticion> questions = new ArrayList<>();
        String query = "SELECT * FROM " + QuestionDatabaseHelperII.TABLE_QUESTIONS +
                " WHERE " + QuestionDatabaseHelperII.COLUMN_USED + " = 0" +
                " ORDER BY RANDOM()" +
                " LIMIT " + count;
        Cursor cursor = database.rawQuery(query, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    questions.add(cursorToQuestion(cursor));
                } while (cursor.moveToNext());
                Log.d(TAG, "Successfully fetched " + questions.size() + " random unused questions.");
            }
            cursor.close();
        } else {
            Log.e(TAG, "Failed to fetch random unused questions - cursor is null.");
        }
        return questions;
    }


    private QuestionModoCompeticion cursorToQuestion(Cursor cursor) {
        QuestionModoCompeticion question = new QuestionModoCompeticion();

        int questionIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_QUESTION);
        if (questionIndex != -1) {
            question.setQUESTION(cursor.getString(questionIndex));
        }

        int optionAIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_OPTION_A);
        if (optionAIndex != -1) {
            question.setOPTION_A(cursor.getString(optionAIndex));
        }

        int optionBIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_OPTION_B);
        if (optionBIndex != -1) {
            question.setOPTION_B(cursor.getString(optionBIndex));
        }

        int optionCIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_OPTION_C);
        if (optionCIndex != -1) {
            question.setOPTION_C(cursor.getString(optionCIndex));
        }

        int answerIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_ANSWER);
        if (answerIndex != -1) {
            question.setANSWER(cursor.getString(answerIndex));
        }

        int categoryIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_CATEGORY);
        if (categoryIndex != -1) {
            question.setCATEGORY(cursor.getString(categoryIndex));
        }

        int imageIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_IMAGE);
        if (imageIndex != -1) {
            question.setIMAGE(cursor.getString(imageIndex));
        }

        int numberIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_NUMBER);
        if (numberIndex != -1) {
            question.setNUMBER(cursor.getString(numberIndex));
        }

        int usedIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_USED);
        if (usedIndex != -1) {
            question.setUsed(cursor.getInt(usedIndex) == 1);
        }

        return question;
    }


    private ContentValues questionToContentValues(QuestionModoCompeticion question) {
        ContentValues values = new ContentValues();

        values.put(QuestionDatabaseHelperII.COLUMN_QUESTION, question.getQUESTION());
        values.put(QuestionDatabaseHelperII.COLUMN_OPTION_A, question.getOPTION_A());
        values.put(QuestionDatabaseHelperII.COLUMN_OPTION_B, question.getOPTION_B());
        values.put(QuestionDatabaseHelperII.COLUMN_OPTION_C, question.getOPTION_C());
        values.put(QuestionDatabaseHelperII.COLUMN_ANSWER, question.getANSWER());
        values.put(QuestionDatabaseHelperII.COLUMN_CATEGORY, question.getCATEGORY());
        values.put(QuestionDatabaseHelperII.COLUMN_IMAGE, question.getIMAGE());
        values.put(QuestionDatabaseHelperII.COLUMN_NUMBER, question.getNUMBER());
        values.put(QuestionDatabaseHelperII.COLUMN_USED, question.isUsed() ? 1 : 0);
        return values;
    }

    public void deleteQuestion(String questionNumber) {
        ensureOpen();
        database.delete(QuestionDatabaseHelperII.TABLE_QUESTIONS, QuestionDatabaseHelperII.COLUMN_NUMBER + " = ?", new String[]{questionNumber});
    }

    public long insertOrUpdateQuestion(QuestionModoCompeticion question) {
        Log.d(TAG, "Inserting new question with number: " + question.getNUMBER());
        ensureOpen();
        long result = -1;
        database.beginTransaction();
        try {
            // Directly insert the question using your insert method
            result = insertQuestion(question);

            database.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, "Error inserting new question with number: " + question.getNUMBER(), e);
        } finally {
            database.endTransaction();
        }
        return result;
    }

    private long updateQuestion(QuestionModoCompeticion question) {
        ContentValues values = questionToContentValues(question);
        return database.update(QuestionDatabaseHelperII.TABLE_QUESTIONS, values, QuestionDatabaseHelperII.COLUMN_NUMBER + " = ?", new String[]{question.getNUMBER()});
    }

    private long insertQuestion(QuestionModoCompeticion question) {
        ContentValues values = questionToContentValues(question);
        return database.insert(QuestionDatabaseHelperII.TABLE_QUESTIONS, null, values);
    }

    public boolean isQuestionInDatabase(String questionNumber) {
        Log.d(TAG, "Checking if question exists in database with number: " + questionNumber);
        ensureOpen();
        String query = "SELECT COUNT(*) FROM " + QuestionDatabaseHelperII.TABLE_QUESTIONS +
                " WHERE " + QuestionDatabaseHelperII.COLUMN_NUMBER + " = ?";

        Cursor cursor = database.rawQuery(query, new String[]{questionNumber});
        boolean questionExists = false;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                questionExists = (count > 0);
            }
            cursor.close();
        } else {
            Log.e(TAG, "Cursor is null for isQuestionInDatabase for question number: " + questionNumber);
        }
        Log.d(TAG, "Question with number: " + questionNumber + " exists: " + questionExists);
        return questionExists;
    }

    public void deleteUsedQuestions() {
        Log.d(TAG, "Deleting all used questions from database.");
        ensureOpen();
        int deletedRows = database.delete(QuestionDatabaseHelperII.TABLE_QUESTIONS, QuestionDatabaseHelperII.COLUMN_USED + " = 1", null);
        Log.d(TAG, "Deleted " + deletedRows + " used questions from database.");
    }

    public List<QuestionModoCompeticion> getLastFiveQuestions() {
        Log.d(TAG, "Fetching the last five questions from database.");
        List<QuestionModoCompeticion> questions = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + QuestionDatabaseHelperII.TABLE_QUESTIONS +
                " WHERE " + QuestionDatabaseHelperII.COLUMN_USED + " = 0" +
                " ORDER BY " + QuestionDatabaseHelperII.COLUMN_ID + " DESC" +
                " LIMIT 5";

        Cursor cursor = database.rawQuery(selectQuery, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                questions.add(cursorToQuestion(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            Log.e(TAG, "Cursor is null or empty for getLastFiveQuestions.");
        }
        Log.d(TAG, "Fetched " + questions.size() + " questions from database as the last five questions.");
        return questions;
    }

    public void markQuestionAsUsed(String questionNumber) {
        Log.d(TAG, "Marking question as used with number: " + questionNumber);
        ensureOpen();
        ContentValues values = new ContentValues();
        values.put(QuestionDatabaseHelperII.COLUMN_USED, 1); // 1 for true
        int updatedRows = database.update(QuestionDatabaseHelperII.TABLE_QUESTIONS, values, QuestionDatabaseHelperII.COLUMN_NUMBER + " = ?", new String[]{questionNumber});
        Log.d(TAG, "Marked " + updatedRows + " question(s) as used with number: " + questionNumber);
    }

    public void open() {
        if (database == null || !database.isOpen()) {
            database = dbHelper.getWritableDatabase();
        }
    }

    public void close() {
        if (database != null && database.isOpen()) {
            database.close();
        }
    }
}