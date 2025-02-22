package com.iniciativaselebi.afrikanaone;

import java.util.Random;
public class LevelMessages {

    // Messages for Failure (Score <= 50%)
    private static final String[] FAILURE_MESSAGES = {
            "Even the lion fails to catch the antelope sometimes. Try again!",
            "A river doesn’t stop flowing just because of a rock. Keep going!",
            "Wisdom is like a baobab tree; no one can embrace it alone. Keep learning!",
            "A fool at forty is a fool forever… but you still have time!",
            "Even the tortoise eventually reaches its destination. Slow progress is still progress!",
            "Do not look where you fell, but where you slipped.",
            "A bird does not change its feathers because the weather is bad.",
            "If you want to go fast, go alone. If you want to go far, play again!",
            "An old monkey knows how to climb trees… but even young monkeys learn!",
            "No matter how hot your anger is, it cannot cook yams."
    };

    // Messages for Average Performance (51% - 89%)
    private static final String[] AVERAGE_MESSAGES = {
            "Not bad, but the ancestors expect more from you!",
            "A little effort and you'll be a legend in no time!",
            "You’re walking, but Africa needs runners. Try again!",
            "Even an elephant was once small. Keep growing!",
            "An eagle that flies with chickens will never soar high. Keep pushing!",
            "You're getting there, but the road is still long.",
            "Even the best hunters miss their prey sometimes. Aim better next time!",
            "A traveler who stops halfway never sees the whole journey.",
            "A tree starts as a seed—water your knowledge and keep growing!",
            "Good job, but can you go higher? Let’s see!"
    };

    // Messages for Success (Score >= 90%)
    private static final String[] SUCCESS_MESSAGES = {
            "Fantastic! We need more Africans like you!",
            "A true warrior of knowledge! Africa salutes you!",
            "Your ancestors are clapping for you right now!",
            "You are what African dreams are made of!",
            "You just made history. Keep going!",
            "If wisdom had a face, it would look like yours!",
            "Even the elders would seek your advice now!",
            "Africa’s future looks brighter with minds like yours!",
            "You’re a walking library of wisdom!",
            "Perfection is rare, and you’re getting there!"
    };

    // Method to get a random failure message
    public static String getRandomFailureMessage() {
        return FAILURE_MESSAGES[new Random().nextInt(FAILURE_MESSAGES.length)];
    }

    // Method to get a random average message
    public static String getRandomAverageMessage() {
        return AVERAGE_MESSAGES[new Random().nextInt(AVERAGE_MESSAGES.length)];
    }

    // Method to get a random success message
    public static String getRandomSuccessMessage() {
        return SUCCESS_MESSAGES[new Random().nextInt(SUCCESS_MESSAGES.length)];
    }
}