package com.iniciativaselebi.afrikanaone;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d("FCM", "Message Received from: " + remoteMessage.getFrom());

        String userId = getCurrentUserId();
        String messageId = remoteMessage.getMessageId();

        if (userId != null && messageId != null) {
            // Log the message receipt directly
            logMessageReceipt(messageId, userId);

            // Display notification
            if (remoteMessage.getNotification() != null) {
                String notificationBody = remoteMessage.getNotification().getBody();
                String notificationTitle = remoteMessage.getNotification().getTitle();
                showNotification(notificationTitle, notificationBody);
            }
        } else {
            Log.d("FCM", "User is not logged in or message ID is null");
        }
    }

    private void logMessageReceipt(String messageId, String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(userId).child("ReceivedMsgs");

        Map<String, Object> receiptData = new HashMap<>();
        receiptData.put("messageId", messageId);
        receiptData.put("timestamp", System.currentTimeMillis());

        String key = userRef.push().getKey();
        if (key != null) {
            userRef.child(key).setValue(receiptData)
                    .addOnSuccessListener(aVoid -> Log.d("FCM", "Message receipt logged successfully"))
                    .addOnFailureListener(e -> Log.e("FCM", "Failed to log message receipt", e));
        } else {
            Log.e("FCM", "Failed to generate unique key for receipt");
        }
    }

    private void showNotification(String title, String body) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Notification Channel Title",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Channel Description");
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(title)
                .setContentText(body)
                .setContentInfo("Info");

        notificationManager.notify(1, notificationBuilder.build());
    }

    private String getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d("FCM", "New token: " + token);
        saveTokenInPreferences(token);
    }

    private void saveTokenInPreferences(String token) {
        SharedPreferences sharedPreferences = getSharedPreferences("appPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("fcmToken", token);
        editor.apply();
    }
}