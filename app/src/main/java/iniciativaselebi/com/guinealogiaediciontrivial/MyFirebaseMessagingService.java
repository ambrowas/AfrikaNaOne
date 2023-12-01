package iniciativaselebi.com.guinealogiaediciontrivial;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if the message contains data
        if (remoteMessage.getData().size() > 0) {
            // Process the data here
            String message = remoteMessage.getData().get("key"); // Replace 'key' with actual data key
            // Display notification based on the data
        }

        // Check if the message contains a notification
        if (remoteMessage.getNotification() != null) {
            String notificationBody = remoteMessage.getNotification().getBody();
            String notificationTitle = remoteMessage.getNotification().getTitle();
            // Display notification
            showNotification(notificationTitle, notificationBody);
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

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher) // Replace with your own icon
                .setContentTitle(title)
                .setContentText(body)
                .setContentInfo("Info");

        notificationManager.notify(1, notificationBuilder.build());

    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        sendTokenToServer(token); // Call your method here
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

}
