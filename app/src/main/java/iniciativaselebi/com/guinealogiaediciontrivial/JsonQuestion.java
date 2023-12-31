package iniciativaselebi.com.guinealogiaediciontrivial;

import java.util.List;

    // Constructor
    public class JsonQuestion {
        private String OPTION1;
        private String OPTION2;
        private String OPTION3;
        private int _id;
        private int answer_nr;
        private String question;

        // Constructor
        public JsonQuestion(String OPTION1, String OPTION2, String OPTION3, int _id, int answer_nr, String question) {
            this.OPTION1 = OPTION1;
            this.OPTION2 = OPTION2;
            this.OPTION3 = OPTION3;
            this._id = _id;
            this.answer_nr = answer_nr;
            this.question = question;
        }

        // Getters
        public String getOPTION1() {
            return OPTION1;
        }

        public String getOPTION2() {
            return OPTION2;
        }

        public String getOPTION3() {
            return OPTION3;
        }

        public int get_id() {
            return _id;
        }

        public int getAnswer_nr() {
            return answer_nr;
        }

        public String getQuestion() {
            return question;
        }

        // Note: Setters removed for immutability
    }

