package iniciativaselebi.com.guinealogiaediciontrivial;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

import iniciativaselebi.com.guinealogiaediciontrivial.QuizContract.QuestionsTable;

public class    QuizDBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Guinealogia.db";
    private static final int DATABASE_VERSION = 1;

    private SQLiteDatabase db;

    public QuizDBHelper(Context context) {
        super(context, DATABASE_NAME,null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        this.db= db;

        final String SQL_CREATE_QUESTIONS_TABLE = "CREATE TABLE " +
                QuestionsTable.TABLE_NAME + " ( " +
                QuestionsTable._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                QuestionsTable.COLUMN_QUESTION + " TEXT, " +
                QuestionsTable.COLUMN_OPTION1  + " TEXT, " +
                QuestionsTable.COLUMN_OPTION2  + " TEXT, " +
                QuestionsTable.COLUMN_OPTION3  + " TEXT, " +
                QuestionsTable.COLUMN_ANSWER_NR + " INTEGER " +
                " ) ";

        db.execSQL(SQL_CREATE_QUESTIONS_TABLE);
        fillQuestionsTable();
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + QuestionsTable.TABLE_NAME);
        onCreate(db);


    }

    private void fillQuestionsTable(){
        Question q1 = new Question("GEOGRAFIA: ¿Cual de estas localidades no es uno de los tres distritos que conforman la provincia de Litoral?", "Bata", "Machinda", "Kogo", 2);
        addQuestion(q1);
        Question q2 = new Question("GEOGRAFIA: ¿Qué nombre le dieron los colonos españoles a la localidad de Añisok?", "Puerto Irradier", "Sevilla de Rio Muni", "Valladolid de los Bimbiles", 3);
        addQuestion(q2);
        Question q3 = new Question("GEOGRAFIA: ¿Qué pueblo costero era conocido anteriormente como Rio Benito?", "Mbini", "Kogo", "Ekuku", 1);
        addQuestion(q3);
        Question q4 = new Question("GEOGRAFIA: ¿Dónde se encuentra el paso fronterizo oficial entre Camerún y Guinea Ecuatorial?", "Kie-Ossi ", "Acombang", "Rio Campo", 1);
        addQuestion(q4);
        Question q5 = new Question("GEOGRAFIA: ¿En qué distrito se encuentra la Reserva Natural del Estuario del Muni?", "Mikomeseng", "Akonibe", "Kogo", 3);
        addQuestion(q5);


    }

    private void addQuestion(Question question) {
        ContentValues cv = new ContentValues();
        cv.put(QuestionsTable.COLUMN_QUESTION, question.getQuestion());
        cv.put(QuestionsTable.COLUMN_OPTION1, question.getOption1());
        cv.put(QuestionsTable.COLUMN_OPTION2, question.getOption2());
        cv.put(QuestionsTable.COLUMN_OPTION3, question.getOption3());
        cv.put(QuestionsTable.COLUMN_ANSWER_NR, question.getAnswerNr());
        db.insert(QuestionsTable.TABLE_NAME, null, cv);
    }

    @SuppressLint("Range")
    public ArrayList<Question> getRandomQuestions(int count) {
        ArrayList<Question> questionList = new ArrayList<>();
        db = getReadableDatabase();
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
    }}
