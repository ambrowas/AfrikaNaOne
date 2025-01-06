package com.iniciativaselebi.afrikanaone;

import static com.google.firebase.crashlytics.internal.Logger.TAG;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class QRcodeActivity extends AppCompatActivity {
        private ImageView qrCodeImageView;
        Button buttonguardar, buttonvolver;
        Bitmap qrCodeBitmap;
        TextView textviewiuu;
        String  code,qrCodeKey;
        MediaPlayer swooshPlayer;
        private long lastSavedTimestamp = 0;



        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_qrcode);

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);
            code = getIntent().getStringExtra("code");
            textviewiuu = (TextView) findViewById(R.id.textviewiuu);

            qrCodeKey = getIntent().getStringExtra("code");
            qrCodeBitmap = generateQRCode(qrCodeKey);
            textviewiuu.setText(qrCodeKey);

            qrCodeImageView = findViewById(R.id.qrCodeImageView);
            final String intentQrCodeKey = getIntent().getStringExtra("code");
            if (intentQrCodeKey != null) {
                Bitmap qrCodeBitmap = generateQRCode(intentQrCodeKey);
                qrCodeImageView.setImageBitmap(qrCodeBitmap);
            }

            if (currentUser != null) {

                String userId = currentUser.getUid();
                String userName = getIntent().getStringExtra("name");
                String userEmail = currentUser.getEmail();
                int lastGameScore = getIntent().getIntExtra("aciertos", 0);
                int lastGamePuntuacion = getIntent().getIntExtra("puntuacion", 0);
                String timestamp = getCurrentDateTime();

                String base64QRCode = generateQRcode(code);
                saveQRCodeToDatabase(userId, base64QRCode, userName, userEmail, lastGameScore, lastGamePuntuacion, timestamp)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Database operation was successful");

                                // Make sure this line is uncommented to get the correct qrCodeKey from the task result
                                String qrCodeKey = task.getResult();

                                Log.d(TAG, "QR Code Key: " + qrCodeKey);

                                // Set the text for textViewiuu here
                                textviewiuu.setText(qrCodeKey);
                            } else {
                                Exception e = task.getException();
                                Log.e(TAG, "Error saving QR Code to database", e);
                            }
                        });


            } else {
                showCustomAlertDialog("Attention", "You have to log in", null);
            }

            buttonvolver = findViewById(R.id.buttonvolver);
            buttonvolver.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playSwoosh();
                    Intent intent = new Intent(QRcodeActivity.this, ClassficationActivity.class);
                    startActivity(intent);
                }
            });
            buttonguardar = findViewById(R.id.buttonguardar);
            buttonguardar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isWithinCoolDownPeriod()) {
                        // If the user has saved a QR code within the last three minutes, show a toast message
                        Sounds.playWarningSound(getApplicationContext());
                        showCustomAlertDialog("Atention", "This QR Code has already been saved. U may have to wait a bit", null);
                        return; // Exit the method early
                    }

                    // Check for storage permissions to save the QR code to the gallery
                    if (ContextCompat.checkSelfPermission(QRcodeActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        // Save the image to gallery
                        saveImageToGallery(QRcodeActivity.this, qrCodeBitmap);
                    } else {
                        // Permission is not granted, show an explanation to the user, etc.
                        if (ActivityCompat.shouldShowRequestPermissionRationale(QRcodeActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            showPermissionRationaleDialog();
                        } else {
                            ActivityCompat.requestPermissions(QRcodeActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        }
                    }
                }
            });

        }

        private void showCustomAlertDialog(String title, String message, DialogInterface.OnClickListener positiveClickListener) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, positiveClickListener != null ? positiveClickListener : (dialogInterface, i) -> dialogInterface.dismiss())
                    .setIcon(R.drawable.afrikanaonelogo)
                    .create();

            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(R.drawable.dialog_background);
            }

            dialog.show();
        }

        private void showPermissionRationaleDialog() {
            AlertDialog dialog = new AlertDialog.Builder(QRcodeActivity.this)
                    .setTitle("Permission Needed")
                    .setMessage("Permission needed to save the image")
                    .setPositiveButton("OK", (dialogInterface, i) -> ActivityCompat.requestPermissions(QRcodeActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1))
                    .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                    .setIcon(R.drawable.afrikanaonelogo)
                    .create();

            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(R.drawable.dialog_background);
            }

            dialog.show();
        }

        private Bitmap generateQRCode(String key) {
            try {
                BitMatrix bitMatrix = new MultiFormatWriter().encode(key, BarcodeFormat.QR_CODE, 400, 400);
                int width = bitMatrix.getWidth();
                int height = bitMatrix.getHeight();
                Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        bmp.setPixel(x, y, bitMatrix.get(x, y) ? android.graphics.Color.BLACK : android.graphics.Color.WHITE);
                    }
                }
                return bmp;

            } catch (WriterException e) {
                e.printStackTrace();
            }
            return null;
        }

        private void saveImageToGallery(Context context, Bitmap bitmap) {
            OutputStream outputStream;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "QRCode_" + System.currentTimeMillis() + ".jpg");
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                try {
                    outputStream = resolver.openOutputStream(Objects.requireNonNull(imageUri));
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    Objects.requireNonNull(outputStream).close();
                        playSwoosh();
                    showCustomAlertDialog("Success", "QR Code saved in your gallery", null);
                    } catch (IOException e) {
                        e.printStackTrace();
                    showCustomAlertDialog("Error", "Something went wrong", null);
                    }
                } else {
                    File imagesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "YourAppName");

                    if (!imagesDir.exists()) {
                        if (!imagesDir.mkdirs()) {
                            showCustomAlertDialog("Error", "Something went wrong", null);
                            return;
                        }
                    }

                    String fileName = "QRCode_" + System.currentTimeMillis() + ".jpg";
                    File imageFile = new File(imagesDir, fileName);
                    try {
                        outputStream = new FileOutputStream(imageFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        outputStream.close();
                        showCustomAlertDialog("Success", "QR Code saved in your gallery", null);
                    } catch (IOException e) {
                        e.printStackTrace();
                        showCustomAlertDialog("Error", "Something went wrong", null);
                    }
                }
            }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == 1) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveImageToGallery(this, qrCodeBitmap);
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(QRcodeActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        // Show an explanation to the user and request the permission again
                        showCustomAlertDialog(
                                "Permission needed",
                                "You need permission to save image in the gallery.",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ActivityCompat.requestPermissions(QRcodeActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                                    }
                                });
                    } else {
                        // User has denied permission and selected "Don't ask again"
                        showCustomAlertDialog(
                                "Permission Denied",
                                "You need to change your settings",
                                null);
                    }
                }
            }
        }

        private String getCurrentDateTime() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
            return dateFormat.format(new Date());
        }

        private String generateQRcode(String code) {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            try {
                BitMatrix bitMatrix = qrCodeWriter.encode(code, BarcodeFormat.QR_CODE, 200, 200);
                qrCodeBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.RGB_565);

                for (int x = 0; x < 200; x++) {
                    for (int y = 0; y < 200; y++) {
                        qrCodeBitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                    }
                }



            } catch (Exception e) {
                e.printStackTrace();
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            // Convert byte array to 24-character base64-encoded string
            String base64QRCode = Base64.encodeToString(byteArray, Base64.DEFAULT);
            return base64QRCode.substring(0, 24);
        }

        private Task<String> saveQRCodeToDatabase(String userId, String base64QRCode, String fullname, String email, int lastGameScore, int lastGamePuntuacion, String timestamp) {
            TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

            if (isWithinCoolDownPeriod()) {
                showCustomAlertDialog("Attention", "You've just generates a code.Wait a few minutes.");
                tcs.setException(new RuntimeException("Cooldown period active"));
                return tcs.getTask();
            }

            DatabaseReference qrCodesRef = FirebaseDatabase.getInstance().getReference("qrCodes");

            Query query = qrCodesRef.orderByChild("userId").equalTo(userId);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot qrCodeSnapshot : dataSnapshot.getChildren()) {
                            QRCodeData qrCodeData = new QRCodeData(qrCodeSnapshot.getKey(), userId, base64QRCode, fullname, email, lastGameScore, lastGamePuntuacion, timestamp);

                            qrCodesRef.child(qrCodeSnapshot.getKey()).setValue(qrCodeData).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Sounds.playMagicalSound(getApplicationContext());
                                    showCustomAlertDialog("Success", "QR Code updated. Go get your money");
                                    tcs.setResult(qrCodeSnapshot.getKey());
                                    lastSavedTimestamp = System.currentTimeMillis();
                                } else {
                                    showCustomAlertDialog("Error", "Something went wrong");
                                    tcs.setException(task.getException());
                                }
                            });
                        }
                    } else {
                        qrCodeKey = qrCodesRef.push().getKey();
                        if (qrCodeKey != null) {
                            QRCodeData qrCodeData = new QRCodeData(qrCodeKey, userId, base64QRCode, fullname, email, lastGameScore, lastGamePuntuacion, timestamp);

                            qrCodesRef.child(qrCodeKey).setValue(qrCodeData).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Sounds.playMagicalSound(getApplicationContext());
                                    showCustomAlertDialog("Success", "QR Code generated. Go get your money");
                                    tcs.setResult(qrCodeKey);
                                    lastSavedTimestamp = System.currentTimeMillis();
                                } else {
                                    showCustomAlertDialog("Error", "Something went wrong");
                                    tcs.setException(task.getException());
                                }
                            });
                        } else {
                            showCustomAlertDialog("Error", "Something went wrong");
                            tcs.setException(new RuntimeException("Error generating QR code key"));
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    showCustomAlertDialog("Error", error.getMessage());
                    tcs.setException(error.toException());
                }
            });
            return tcs.getTask();
        }

        private void showCustomAlertDialog(String title, String message) {
            AlertDialog dialog = new AlertDialog.Builder(QRcodeActivity.this)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss())
                    .setIcon(R.drawable.afrikanaonelogo)
                    .create();

            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(R.drawable.dialog_background);
            }

            dialog.show();
        }

        private boolean isWithinCoolDownPeriod() {
            long currentTimeMillis = System.currentTimeMillis();
            long threeMinutesInMillis = 3 * 60 * 1000;

            return (currentTimeMillis - lastSavedTimestamp) < threeMinutesInMillis;
        }

        private void playSwoosh() {
            if (swooshPlayer != null) {
                swooshPlayer.seekTo(0);
                swooshPlayer.start();
            }
        }

        @Override
        protected void onDestroy() {
            if (swooshPlayer != null) {
                swooshPlayer.release();
                swooshPlayer = null;
            }
            super.onDestroy();
        }



    }


