package iniciativaselebi.com.guinealogiaediciontrivial;

import android.provider.BaseColumns;

public class QuizContract {

    private QuizContract() {}

    public static class QuestionsTable implements BaseColumns {
        public static final String TABLE_NAME = "quiz_questions";
        public static final String COLUMN_QUESTION = "question";
        public static final String COLUMN_OPTION1 = "OPTION1";
        public static final String COLUMN_OPTION2 = "OPTION2";
        public static final String COLUMN_OPTION3 = "OPTION3";
        public static final String COLUMN_ANSWER_NR = "answer_nr";
    }
}
