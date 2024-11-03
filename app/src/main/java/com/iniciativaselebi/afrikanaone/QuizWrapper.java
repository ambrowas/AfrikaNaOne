package com.iniciativaselebi.afrikanaone;

import java.util.List;

public class QuizWrapper {
    private int count;
    private int uniqueQuestions;
    private List<JsonQuestion> questions;

    // Getters
    public List<JsonQuestion> getQuestions() {
        return questions;
    }
}
