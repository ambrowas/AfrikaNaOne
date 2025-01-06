package com.iniciativaselebi.afrikanaone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BatchManager {
    private final SharedPreferences preferences;
    private static final String PREFS_NAME = "quiz_prefs";
    private static final String COMPLETED_BATCHES_KEY = "completed_batches";
    private static final String TOTAL_BATCHES_KEY = "total_batches";

    private final List<Integer> shuffledBatchOrder;
    private int currentBatchIndex = 0;

    private static final String SHUFFLED_BATCH_ORDER_KEY = "shuffled_batch_order_key";


    public BatchManager(Context context) {
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.shuffledBatchOrder = new ArrayList<>();
    }

    public void initializeShuffledBatchOrder(int totalBatches) {
        if (shuffledBatchOrder.isEmpty()) {
            shuffledBatchOrder.clear();
            for (int i = 0; i < totalBatches; i++) {
                shuffledBatchOrder.add(i);
            }
            Collections.shuffle(shuffledBatchOrder);
            saveShuffledBatchOrderToPreferences();
            Log.d("batchFlow", "Initialized shuffled batch order: " + shuffledBatchOrder);
        } else {
            loadShuffledBatchOrderFromPreferences();
        }
    }

    private void saveShuffledBatchOrderToPreferences() {
        SharedPreferences.Editor editor = preferences.edit();
        Set<String> batchOrderSet = new HashSet<>();
        for (Integer batchIndex : shuffledBatchOrder) {
            batchOrderSet.add(String.valueOf(batchIndex));
        }
        editor.putStringSet(SHUFFLED_BATCH_ORDER_KEY, batchOrderSet).apply();
    }

    private void loadShuffledBatchOrderFromPreferences() {
        Set<String> batchOrderSet = preferences.getStringSet(SHUFFLED_BATCH_ORDER_KEY, null);
        if (batchOrderSet != null) {
            shuffledBatchOrder.clear();
            for (String batchIndex : batchOrderSet) {
                shuffledBatchOrder.add(Integer.parseInt(batchIndex));
            }
            Log.d("batchFlow", "Loaded shuffled batch order from preferences: " + shuffledBatchOrder);
        }
    }

    public void saveTotalBatches(int totalBatches, Map<Integer, List<JsonQuestion>> questionBatches) {
        preferences.edit().putInt(TOTAL_BATCHES_KEY, totalBatches).apply();
        shuffledBatchOrder.clear();
        for (int i = 0; i < totalBatches; i++) {
            shuffledBatchOrder.add(i);
        }
        Collections.shuffle(shuffledBatchOrder);
        Log.d("batchFlow", "Initialized shuffled batch order: " + shuffledBatchOrder);


    }

    public void resetQuizProgress() {
        preferences.edit()
                .remove(COMPLETED_BATCHES_KEY)
                .remove(TOTAL_BATCHES_KEY)
                .apply();
        currentBatchIndex = 0;
        Log.d("batchFlow", "Quiz progress reset: cleared completed batches and total batches.");
    }

    public int getTotalBatches() {
        return preferences.getInt(TOTAL_BATCHES_KEY, 0);
    }

    public void markBatchAsCompleted(int batchIndex) {
        Set<String> completedBatches = getCompletedBatchesStringSet();
        completedBatches.add(String.valueOf(batchIndex));
        preferences.edit().putStringSet(COMPLETED_BATCHES_KEY, completedBatches).apply();
        Log.d("batchFlow", "Marked batch " + (batchIndex + 1) + " as completed. Total completed: " + completedBatches.size());
    }

    public boolean allBatchesCompleted() {
        int completedCount = getCompletedBatches().size();
        int totalCount = getTotalBatches();
        Log.d("batchFlow", "Checking if all batches completed: " + completedCount + "/" + totalCount);
        return completedCount >= totalCount;
    }

    public int getNextAvailableBatchIndex() {
        Set<Integer> completedBatches = getCompletedBatches();
        while (currentBatchIndex < shuffledBatchOrder.size()) {
            int batchIndex = shuffledBatchOrder.get(currentBatchIndex);
            if (!completedBatches.contains(batchIndex)) {
                Log.d("batchFlow", "Next available batch (shuffled): " + (batchIndex + 1));
                return batchIndex;
            }
            currentBatchIndex++;
        }
        return -1;
    }

   Set<Integer> getCompletedBatches() {
        Set<String> batchSet = preferences.getStringSet(COMPLETED_BATCHES_KEY, new HashSet<>());
        Set<Integer> completedBatches = new HashSet<>();
        for (String batch : batchSet) {
            completedBatches.add(Integer.parseInt(batch));
        }
        return completedBatches;
    }

    private Set<String> getCompletedBatchesStringSet() {
        return preferences.getStringSet(COMPLETED_BATCHES_KEY, new HashSet<>());
    }
}
