package com.iniciativaselebi.afrikanaone;

import java.util.Date;
import java.util.Map;

public class JsonQuestion {
    private int number;  // Corresponds to the 'number' field in Firestore
    private String image;  // If you plan to use image URLs
    private String category;  // Corresponds to the 'category' field in Firestore
    private String question;  // Corresponds to the 'question' field in Firestore
    private String answer;  // Corresponds to the 'answer' field in Firestore
    private Map<String, String> options;  // To store the options 'a', 'b', 'c'
    private Date createdAt;  // To store timestamp from Firestore
    private String explanation;  // New field to store explanations

    // No-argument constructor (required for Firebase)
    public JsonQuestion() {
    }

    // Getters and Setters
    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    // Helper methods to get options A, B, C
    public String getOPTION_A() {
        return options != null ? options.get("a") : null;
    }

    public String getOPTION_B() {
        return options != null ? options.get("b") : null;
    }

    public String getOPTION_C() {
        return options != null ? options.get("c") : null;
    }
    public int getAnswerNr() {
        if (answer.equals(getOPTION_A())) {
            return 1;
        } else if (answer.equals(getOPTION_B())) {
            return 2;
        } else if (answer.equals(getOPTION_C())) {
            return 3;
        }
        return -1;  // If none match, return -1 or handle as an error
    }

}
