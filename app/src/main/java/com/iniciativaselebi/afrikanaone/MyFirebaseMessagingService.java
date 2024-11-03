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
            // User is logged in, log the message receipt
            logMessageReceipt(messageId, userId);

            // Check if the message contains a notification
            if (remoteMessage.getNotification() != null) {
                String notificationBody = remoteMessage.getNotification().getBody();
                String notificationTitle = remoteMessage.getNotification().getTitle();

                // Display notification with the additional messageId and userId
                showNotification(notificationTitle, notificationBody, messageId, userId);
            }
        } else {
            Log.d("FCM", "User is not logged in or message ID is null");
        }
    }

    private String getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // User is signed in
            Log.d("FCM", "getCurrentUserId: User ID: " + currentUser.getUid());
            return currentUser.getUid();
        } else {
            // User is not signed in
            Log.d("FCM", "getCurrentUserId: User not logged in");
            return null; // Or handle it as per your requirement
        }
    }
    private void showNotification(String title, String body, String messageId, String userId) {
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

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("messageId", messageId);
        intent.putExtra("userId", userId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(title)
                .setContentText(body)
                .setContentInfo("Info")
                .setContentIntent(pendingIntent);

        notificationManager.notify(1, notificationBuilder.build());
    }


    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d("FCM", "New token: " + token);
        sendTokenToServer(token);
        // Save the new token in SharedPreferences
        saveTokenInPreferences(token);
    }
    private void saveTokenInPreferences(String token) {
        SharedPreferences sharedPreferences = getSharedPreferences("appPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("fcmToken", token);
        editor.apply();
    }
    private void sendTokenToServer(String token) {
        // Replace with your server's URL
        String serverUrl = "https://yourserver.com/register_token";

        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(serverUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            // Prepare the JSON payload. You might need to customize this based on your server's requirements.
            JSONObject payload = new JSONObject();
            payload.put("token", token);

            // Enable writing output to the connection. This means we want to send a request body.
            urlConnection.setDoOutput(true);

            // Write the payload to the request body
            try (OutputStream os = urlConnection.getOutputStream()) {
                byte[] input = payload.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Check the response from the server
            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // The token was sent successfully
                Log.d("FCM", "Token sent successfully");
            } else {
                // Server returned an error
                Log.e("FCM", "Error sending token to server: " + responseCode);
            }
        } catch (Exception e) {
            Log.e("FCM", "Exception while sending token to server", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
    private void logMessageReceipt(String messageId, String userId) {
        Log.d("FCM", "Attempting to log message receipt for user: " + userId + " with message ID: " + messageId);
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(userId).child("ReceivedMsgs");

        // Create a unique key for each message receipt
        String key = userRef.push().getKey();

        // Format the receipt data
        Map<String, Object> receiptData = new HashMap<>();
        receiptData.put("messageId", messageId);
        receiptData.put("timestamp", System.currentTimeMillis());

        // Use the unique key to store each receipt
        if (key != null) {
            userRef.child(key).setValue(receiptData)
                    .addOnSuccessListener(aVoid -> Log.d("FCM", "Message receipt logged successfully for user: " + userId))
                    .addOnFailureListener(e -> Log.e("FCM", "Failed to log message receipt for user: " + userId, e));
        } else {
            Log.e("FCM", "Failed to generate a unique key for message receipt");
        }
    }



}
