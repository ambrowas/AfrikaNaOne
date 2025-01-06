package com.iniciativaselebi.afrikanaone;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class QuestionDatabaseHelperII extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "questions.db";
    private static final int DATABASE_VERSION = 1;

    // Define table name and column names
    public static final String TABLE_QUESTIONS = "questions";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_QUESTION = "question";
    public static final String COLUMN_OPTION_A = "optionA";
    public static final String COLUMN_OPTION_B = "optionB";
    public static final String COLUMN_OPTION_C = "optionC";
    public static final String COLUMN_ANSWER = "answer";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_IMAGE = "image";
    public static final String COLUMN_NUMBER = "number";
    public static final String COLUMN_USED = "used";

    // Singleton instance of the helper
    private static QuestionDatabaseHelperII sInstance;

    // Private constructor to prevent direct instantiation
    public QuestionDatabaseHelperII(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Public method to get the Singleton instance
    public static synchronized QuestionDatabaseHelperII getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new QuestionDatabaseHelperII(context.getApplicationContext());
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Define the table schema and create the table
        String createTableSQL = "CREATE TABLE " + TABLE_QUESTIONS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_QUESTION + " TEXT, " +
                COLUMN_OPTION_A + " TEXT, " +
                COLUMN_OPTION_B + " TEXT, " +
                COLUMN_OPTION_C + " TEXT, " +
                COLUMN_ANSWER + " TEXT, " +
                COLUMN_CATEGORY + " TEXT, " +
                COLUMN_IMAGE + " TEXT, " +
                COLUMN_NUMBER + " TEXT, " +
                COLUMN_USED + " INTEGER DEFAULT 0" +
                ");";
        db.execSQL(createTableSQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database schema upgrades if needed
    }
}
