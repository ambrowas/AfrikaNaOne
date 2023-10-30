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

    public List<QuestionModoCompeticion> fetchRandomUnusedQuestions(int count) {
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
            }
            cursor.close();
        } else {
            Log.e(TAG, "Cursor is null for fetchRandomUnusedQuestions.");
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
        ensureOpen();
        long result = -1;
        database.beginTransaction();
        try {
            if (isQuestionInDatabase(question.getNUMBER())) {
                result = updateQuestion(question);
            } else {
                result = insertQuestion(question);
            }
            database.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, "Error in insertOrUpdateQuestion.", e);
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
            Log.e(TAG, "Cursor is null for isQuestionInDatabase.");
        }
        return questionExists;
    }

    public void deleteUsedQuestions() {
        ensureOpen();
        database.delete(QuestionDatabaseHelperII.TABLE_QUESTIONS, QuestionDatabaseHelperII.COLUMN_USED + " = 1", null);
    }

    public List<QuestionModoCompeticion> getLastFiveQuestions() {
        List<QuestionModoCompeticion> questions = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + QuestionDatabaseHelperII.TABLE_QUESTIONS +
                " WHERE " + QuestionDatabaseHelperII.COLUMN_USED + " = 0" +
                " ORDER BY " + QuestionDatabaseHelperII.COLUMN_ID + " DESC" +
                " LIMIT 5";

        Cursor cursor = database.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                int questionIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_QUESTION);
                int optionAIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_OPTION_A);
                int optionBIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_OPTION_B);
                int optionCIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_OPTION_C);
                int answerIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_ANSWER);
                int categoryIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_CATEGORY);
                int imageIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_IMAGE);
                int numberIndex = cursor.getColumnIndex(QuestionDatabaseHelperII.COLUMN_NUMBER);

                if (questionIndex == -1 || optionAIndex == -1 || optionBIndex == -1 || optionCIndex == -1 ||
                        answerIndex == -1 || categoryIndex == -1 || imageIndex == -1 || numberIndex == -1) {
                    continue; // Skip this row if any column index is not found.
                }

                String question = cursor.getString(questionIndex);
                String optionA = cursor.getString(optionAIndex);
                String optionB = cursor.getString(optionBIndex);
                String optionC = cursor.getString(optionCIndex);
                String answer = cursor.getString(answerIndex);
                String category = cursor.getString(categoryIndex);
                String imageUrl = cursor.getString(imageIndex);
                String number = cursor.getString(numberIndex);

                // Now, create the QuestionModoCompeticion object and add it to the list
                QuestionModoCompeticion questionObject = new QuestionModoCompeticion(question, optionA, optionB, optionC, answer, category, imageUrl, number);
                questions.add(questionObject);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return questions;
    }

    public void markQuestionAsUsed(String questionNumber) {
        ensureOpen();
        ContentValues values = new ContentValues();
        values.put(QuestionDatabaseHelperII.COLUMN_USED, 1); // 1 for true
        database.update(QuestionDatabaseHelperII.TABLE_QUESTIONS, values, QuestionDatabaseHelperII.COLUMN_NUMBER + " = ?", new String[]{questionNumber});
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