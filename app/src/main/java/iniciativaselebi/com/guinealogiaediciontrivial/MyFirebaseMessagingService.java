package iniciativaselebi.com.guinealogiaediciontrivial;

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

        // Log the receipt of a message
        Log.d("FCM", "onMessageReceived: Received a message");

        // Retrieve the current user's ID
        String userId = getCurrentUserId();
        if (userId != null) {
            // User is logged in, log the message receipt
            String messageId = remoteMessage.getMessageId();
            logMessageReceipt(messageId, userId);
            Log.d("FCM", "onMessageReceived: User is logged in, logging message receipt");
        } else {
            // User is not logged in
            Log.d("FCM", "onMessageReceived: User is not logged in, cannot log message receipt");
        }

        // Check if the message contains a notification
        if (remoteMessage.getNotification() != null) {
            String notificationBody = remoteMessage.getNotification().getBody();
            String notificationTitle = remoteMessage.getNotification().getTitle();
            Log.d("FCM", "onMessageReceived: Notification received - Title: " + notificationTitle + ", Body: " + notificationBody);
            // Display notification
            showNotification(notificationTitle, notificationBody);
        } else {
            // No notification payload, might be a data message
            Log.d("FCM", "onMessageReceived: No Notification payload, might be a data message");
            // Additional handling for data messages if needed
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
    private void showNotification(String title, String body) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";

        // Since Android Oreo, notification channels are required.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Notification Channel Title",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Channel Description");
            notificationManager.createNotificationChannel(channel);
        }

        // Create an intent that does nothing
        Intent intent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_stat_name) // Use the resource ID of your icon
                .setContentTitle(title)
                .setContentText(body)
                .setContentInfo("Info")
                .setContentIntent(pendingIntent); // Set the do-nothing intent

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
        // Reference to the user's node in the Firebase Realtime Database
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(userId);

        // Concatenate messageId and the current timestamp
        String receiptData = messageId + ", " + System.currentTimeMillis();

        // Update the user's data with the concatenated receipt information
        userRef.child("ReceivedMsgs").setValue(receiptData)
                .addOnSuccessListener(aVoid -> Log.d("FCM", "Message receipt logged successfully in user node"))
                .addOnFailureListener(e -> Log.e("FCM", "Failed to log message receipt in user node", e));
    }

}
