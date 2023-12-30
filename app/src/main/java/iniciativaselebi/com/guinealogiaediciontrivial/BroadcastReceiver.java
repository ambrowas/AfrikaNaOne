package iniciativaselebi.com.guinealogiaediciontrivial;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Extract messageId and userId from intent
        String messageId = intent.getStringExtra("messageId");
        String userId = intent.getStringExtra("userId");

        if (messageId != null && userId != null) {
            // Log the message receipt
            logMessageReceipt(context, messageId, userId);
        }
    }

    private void logMessageReceipt(Context context, String messageId, String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(userId);
        String receiptData = messageId + ", " + System.currentTimeMillis();
        userRef.child("ReceivedMsgs").setValue(receiptData)
                .addOnSuccessListener(aVoid -> Log.d("FCM", "Message receipt logged successfully in user node"))
                .addOnFailureListener(e -> Log.e("FCM", "Failed to log message receipt in user node", e));
    }
}
