package com.iniciativaselebi.afrikanaone;


import java.util.Comparator;
import java.util.PriorityQueue;

import com.iniciativaselebi.afrikanaone.Warning;
import java.util.PriorityQueue;
public class WarningManager {
    private final PriorityQueue<Warning> warningQueue = new PriorityQueue<>();
    private boolean isDialogVisible = false;

    public void addWarning(Warning warning) {
        warningQueue.offer(warning);
        showNextWarning();
    }

    private void showNextWarning() {
        if (isDialogVisible || warningQueue.isEmpty()) return;

        Warning nextWarning = warningQueue.poll();
        if (nextWarning != null) {
            isDialogVisible = true;
            nextWarning.show(); // Call show() without parameters
            nextWarning.setOnDismissListener(() -> {
                isDialogVisible = false;
                showNextWarning(); // Show the next warning after the current one is dismissed
            });
        }
    }

    public void clearWarnings() {
        warningQueue.clear();
    }
}