package com.iniciativaselebi.afrikanaone;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Question implements Parcelable {

    private String question;
    private String option1;
    private String option2;
    private String option3;
    private int answerNr;
    private String answer;

    // Constructor
    public Question(String question, String option1, String option2, String option3, int answerNr) {
        this.question = question;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
        this.answerNr = answerNr;

        // Set the answer based on the answerNr
        switch (answerNr) {
            case 1:
                this.answer = option1;
                break;
            case 2:
                this.answer = option2;
                break;
            case 3:
                this.answer = option3;
                break;
            default:
                throw new IllegalArgumentException("Invalid answer number: " + answerNr);
        }
    }

    protected Question(Parcel in) {
        question = in.readString();
        option1 = in.readString();
        option2 = in.readString();
        option3 = in.readString();
        answerNr = in.readInt();
        answer = in.readString();
    }

    public static final Creator<Question> CREATOR = new Creator<Question>() {
        @Override
        public Question createFromParcel(Parcel in) {
            return new Question(in);
        }

        @Override
        public Question[] newArray(int size) {
            return new Question[size];
        }
    };

    // Getters
    public String getQuestion() {
        return question;
    }

    public String getOption1() {
        return option1;
    }

    public String getOption2() {
        return option2;
    }

    public String getOption3() {
        return option3;
    }

    public int getAnswerNr() {
        return answerNr;
    }

    public String getAnswer() {
        return answer;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(question);
        dest.writeString(option1);
        dest.writeString(option2);
        dest.writeString(option3);
        dest.writeInt(answerNr);
        dest.writeString(answer);
    }

    // Parcelable methods
    // ...
}
