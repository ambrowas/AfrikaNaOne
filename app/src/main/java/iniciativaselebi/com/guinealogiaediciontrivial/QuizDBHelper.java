package iniciativaselebi.com.guinealogiaediciontrivial;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import iniciativaselebi.com.guinealogiaediciontrivial.QuizContract.QuestionsTable;

public class QuizDBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Guinealogia.db";
    private static final int DATABASE_VERSION = 3;

    private final Context context;
    private static String DB_PATH;

    public QuizDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        DB_PATH = context.getDatabasePath(DATABASE_NAME).getPath();

        if (!checkDatabase()) {
            try {
                copyDatabase(context);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    private boolean checkDatabase() {
        SQLiteDatabase checkDB = null;
        try {
            checkDB = SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READONLY);
        } catch (SQLiteException e) {
            // Database doesn't exist yet
            Log.e("QuizDBHelper", "Database not found");
        }

        if (checkDB != null) {
            checkDB.close();
        }

        return checkDB != null;
    }


    private void copyDatabase(Context context) throws IOException {
        InputStream myInput = context.getAssets().open(DATABASE_NAME);
        OutputStream myOutput = new FileOutputStream(DB_PATH);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }

        myOutput.flush();
        myOutput.close();
        myInput.close();

        Log.d("QuizDBHelper", "Database successfully copied");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion) {
            context.deleteDatabase(DATABASE_NAME);
            try {
                copyDatabase(context);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SuppressLint("Range")
    public ArrayList<Question> getRandomQuestions(int count) {
        ArrayList<Question> questionList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + QuestionsTable.TABLE_NAME + " ORDER BY RANDOM() LIMIT " + count, null);

        if (c.moveToFirst()) {
            do {
                int id = c.getInt(c.getColumnIndex(QuestionsTable._ID));
                String questionText = c.getString(c.getColumnIndex(QuestionsTable.COLUMN_QUESTION));
                String option1 = c.getString(c.getColumnIndex(QuestionsTable.COLUMN_OPTION1));
                String option2 = c.getString(c.getColumnIndex(QuestionsTable.COLUMN_OPTION2));
                String option3 = c.getString(c.getColumnIndex(QuestionsTable.COLUMN_OPTION3));
                int answerNr = c.getInt(c.getColumnIndex(QuestionsTable.COLUMN_ANSWER_NR));

                Question question = new Question(questionText, option1, option2, option3, answerNr);
                questionList.add(question);
            } while (c.moveToNext());
        }

        c.close();
        return questionList;
    }
}
