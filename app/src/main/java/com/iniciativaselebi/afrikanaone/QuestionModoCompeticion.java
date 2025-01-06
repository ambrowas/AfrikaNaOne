package com.iniciativaselebi.afrikanaone;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.PropertyName;

public class QuestionModoCompeticion implements Parcelable {

    private String QUESTION;
    @PropertyName("OPTION A")
    private String OPTION_A;
    @PropertyName("OPTION B")
    private String OPTION_B;

    @PropertyName("OPTION C")
    private String OPTION_C;

    private String ANSWER;
    private String CATEGORY;
    private String IMAGE;
    private String NUMBER;
    private boolean used;
    private long lastAccessed;

    // Default constructor
    public QuestionModoCompeticion() {
    }




    // Parameterized constructor
    public QuestionModoCompeticion(String QUESTION, String OPTION_A, String OPTION_B, String OPTION_C, String ANSWER, String CATEGORY, String IMAGE, String NUMBER) {
        this.QUESTION = QUESTION;
        this.OPTION_A = OPTION_A;
        this.OPTION_B = OPTION_B;
        this.OPTION_C = OPTION_C;
        this.ANSWER = ANSWER;
        this.CATEGORY = CATEGORY;
        this.IMAGE = IMAGE;
        this.NUMBER = NUMBER;
        this.used = false;  // Default value
    }


    @Override
    public String toString() {
        return "QUESTION: " + QUESTION +
                ", OPTION A: " + OPTION_A +
                ", OPTION B: " + OPTION_B +
                ", OPTION C: " + OPTION_C +
                ", ANSWER: " + ANSWER +
                ", CATEGORY: " + CATEGORY +
                ", IMAGE: " + IMAGE +
                ", NUMBER: " + NUMBER;

    }

    protected QuestionModoCompeticion(Parcel in) {
        QUESTION = in.readString();
        OPTION_A = in.readString();
        OPTION_B = in.readString();
        OPTION_C = in.readString();
        ANSWER = in.readString();
        CATEGORY = in.readString();
        IMAGE = in.readString();
        NUMBER = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(QUESTION);
        dest.writeString(OPTION_A);
        dest.writeString(OPTION_B);
        dest.writeString(OPTION_C);
        dest.writeString(ANSWER);
        dest.writeString(CATEGORY);
        dest.writeString(IMAGE);
        dest.writeString(NUMBER);
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

    @PropertyName("QUESTION")
    public String getQUESTION() {
        return QUESTION;
    }

    @PropertyName("QUESTION")
    public void setQUESTION(String QUESTION) {
        this.QUESTION = QUESTION;
    }

    @PropertyName("OPTION A")
    public String getOPTION_A() {
        return OPTION_A;
    }

    @PropertyName("OPTION A")
    public void setOPTION_A(String OPTION_A) {
        this.OPTION_A = OPTION_A;
    }

    @PropertyName("OPTION B")
    public String getOPTION_B() {
        return OPTION_B;
    }

    @PropertyName("OPTION B")
    public void setOPTION_B(String OPTION_B) {
        this.OPTION_B = OPTION_B;
    }

    @PropertyName("OPTION C")
    public String getOPTION_C() {
        return OPTION_C;
    }

    @PropertyName("OPTION C")
    public void setOPTION_C(String OPTION_C) {
        this.OPTION_C = OPTION_C;
    }


    @PropertyName("ANSWER")
    public String getANSWER() {
        return ANSWER;
    }

    @PropertyName("ANSWER")
    public void setANSWER(String ANSWER) {
        this.ANSWER = ANSWER;
    }

    @PropertyName("CATEGORY")
    public String getCATEGORY() {
        return CATEGORY;
    }

    @PropertyName("CATEGORY")
    public void setCATEGORY(String CATEGORY) {
        this.CATEGORY = CATEGORY;
    }

    @PropertyName("IMAGE")
    public String getIMAGE() {
        return IMAGE;
    }

    @PropertyName("IMAGE")
    public void setIMAGE(String IMAGE) {
        this.IMAGE = IMAGE;
    }

    @PropertyName("NUMBER")
    public String getNUMBER() {
        return NUMBER;
    }

    @PropertyName("NUMBER")
    public void setNUMBER(String NUMBER) {
        this.NUMBER = NUMBER;
    }

    // Local property getters and setters
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