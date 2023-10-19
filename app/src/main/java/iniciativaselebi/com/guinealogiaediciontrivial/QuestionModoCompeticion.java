package iniciativaselebi.com.guinealogiaediciontrivial;
import android.os.Parcel;
import android.os.Parcelable;

public class QuestionModoCompeticion implements Parcelable {

    private String question;
    private String optionA;
    private String optionB;
    private String optionC;
    private String answer;
    private String category;
    private String imageUrl;
    private String number;
    private boolean used;

    private long lastAccessed;

    // Constructor
    public QuestionModoCompeticion(String question, String optionA, String optionB, String optionC, String answer, String category, String image, String number, boolean used) {
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.answer = answer;
        this.category = category;
        this.imageUrl = image;
        this.number = number;
        this.used = false; // Initialize used to false
        this.lastAccessed = 0L; // Initialize lastAccessed to 0
    }

    // Other getters and setters

    protected QuestionModoCompeticion(Parcel in) {
        question = in.readString();
        optionA = in.readString();
        optionB = in.readString();
        optionC = in.readString();
        answer = in.readString();
        category = in.readString();
        imageUrl = in.readString();
        number = in.readString();
        used = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(question);
        dest.writeString(optionA);
        dest.writeString(optionB);
        dest.writeString(optionC);
        dest.writeString(answer);
        dest.writeString(category);
        dest.writeString(imageUrl);
        dest.writeString(number);
        dest.writeByte((byte) (used ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<QuestionModoCompeticion> CREATOR = new Creator<QuestionModoCompeticion>() {
        @Override
        public QuestionModoCompeticion createFromParcel(Parcel in) {
            return new QuestionModoCompeticion(in);
        }

        @Override
        public QuestionModoCompeticion[] newArray(int size) {
            return new QuestionModoCompeticion[size];
        }
    };

    public String getQuestion() {
        return question;
    }

    public String getOptionA() {
        return optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public String getAnswer() {
        return answer;
    }

    public String getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getNumber() {
        return number;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public long getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(long lastAccessed) {
        this.lastAccessed = lastAccessed;
    }
}
