package iniciativaselebi.com.guinealogiaediciontrivial;

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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

import Model.User;
import iniciativaselebi.com.guinealogiaediciontrivial.R.layout;

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
                Toast.makeText(getApplicationContext(), "Debes iniciar sesion", Toast.LENGTH_SHORT).show();
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
                    // Check for storage permissions to save the QR code to the gallery
                    if (ContextCompat.checkSelfPermission(QRcodeActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        saveImageToGallery(QRcodeActivity.this, qrCodeBitmap);
                    } else {
                        // Show an explanation to the user and request the permission again
                        if (ActivityCompat.shouldShowRequestPermissionRationale(QRcodeActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            new AlertDialog.Builder(QRcodeActivity.this)
                                    .setTitle("Se necesita permiso")
                                    .setMessage("Se necesita permiso para guardar la imagen")
                                    .setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(QRcodeActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1))
                                    .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                                    .create().show();
                        } else {
                            ActivityCompat.requestPermissions(QRcodeActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        }
                    }
                }
            });

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
                    Toast.makeText(getApplicationContext(), "Código QR guardado en la galería", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Error al guardar el código", Toast.LENGTH_LONG).show();
                }
            } else {
                File imagesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "YourAppName");

                if (!imagesDir.exists()) {
                    if (!imagesDir.mkdirs()) {
                        Toast.makeText(getApplicationContext(), "Error al crear el directorio", Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                String fileName = "QRCode_" + System.currentTimeMillis() + ".jpg";
                File imageFile = new File(imagesDir, fileName);
                try {
                    outputStream = new FileOutputStream(imageFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.close();
                    Toast.makeText(getApplicationContext(), "Código QR guardado en la galería", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Error al guardar el código", Toast.LENGTH_LONG).show();
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
                        new AlertDialog.Builder(this)
                                .setTitle("Se necesita permiso")
                                .setMessage("Se necesita permiso para guardar la imagen en la galería.")
                                .setPositiveButton("OK",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                ActivityCompat.requestPermissions(QRcodeActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                                            }
                                        })
                                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .create().show();
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

//                qr_code_image = findViewById(R.id.qr_code_image);
//                qr_code_image.setImageBitmap(qrCodeBitmap);

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
                Toast.makeText(QRcodeActivity.this, "Debes esperar 3 minutos antes de poder guardar otro código QR.", Toast.LENGTH_SHORT).show();
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

                            qrCodesRef.child(qrCodeSnapshot.getKey()).setValue(qrCodeData).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        playSwoosh();
                                        Toast.makeText(QRcodeActivity.this, "Código QR actualizado", Toast.LENGTH_SHORT).show();
                                        tcs.setResult(qrCodeSnapshot.getKey());
                                        lastSavedTimestamp = System.currentTimeMillis();  // Update the timestamp here.
                                    } else {
                                        Toast.makeText(QRcodeActivity.this, "Error al actualizar Código QR", Toast.LENGTH_SHORT).show();
                                        tcs.setException(task.getException());
                                    }

                                }
                            });
                        }
                    } else {
                        qrCodeKey = qrCodesRef.push().getKey();
                        if (qrCodeKey != null) {
                            QRCodeData qrCodeData = new QRCodeData(qrCodeKey, userId, base64QRCode, fullname, email, lastGameScore, lastGamePuntuacion, timestamp);

                            qrCodesRef.child(qrCodeKey).setValue(qrCodeData).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        playSwoosh();
                                        Toast.makeText(QRcodeActivity.this, "Código QR creado", Toast.LENGTH_SHORT).show();
                                        tcs.setResult(qrCodeKey);
                                        lastSavedTimestamp = System.currentTimeMillis();  // Update the timestamp here.
                                    } else {
                                        Toast.makeText(QRcodeActivity.this, "Error al crear Código QR", Toast.LENGTH_SHORT).show();
                                        tcs.setException(task.getException());
                                    }

                                }
                            });
                        } else {
                            Toast.makeText(QRcodeActivity.this, "Error al generar código QR", Toast.LENGTH_SHORT).show();
                            tcs.setException(new RuntimeException("Error generating QR code key"));
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    tcs.setException(error.toException());
                }
            });
            return tcs.getTask();

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


