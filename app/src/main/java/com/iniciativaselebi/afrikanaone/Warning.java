package com.iniciativaselebi.afrikanaone;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;

public class Warning implements Comparable<Warning> {
    private final Context context;
    private final int priority;
    private final String title;
    private final String message;
    private Runnable onDismiss; // Add callback
    private AlertDialog dialog;

    public Warning(Context context, int priority, String title, String message, Runnable onDismiss) {
        this.context = context;
        this.priority = priority;
        this.title = title;
        this.message = message;
        this.onDismiss = onDismiss; // Assign the callback
    }

    public Warning(Context context, int priority, String title, String message) {
        this(context, priority, title, message, null); // Default constructor without callback
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    if (onDismiss != null) {
                        onDismiss.run(); // Invoke the callback when dismissed
                    }
                })
                .setCancelable(false)
                .setIcon(R.drawable.afrikanaonelogo);

        dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        dialog.show();
    }

    public void setOnDismissListener(Runnable onDismiss) {
        this.onDismiss = onDismiss; // Allow setting the callback dynamically
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(Warning other) {
        return Integer.compare(this.priority, other.priority);
    }
}