package com.iniciativaselebi.afrikanaone;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CheckCodigoActivity extends AppCompatActivity {
    private Button button_validar, button_regresar;

    MediaPlayer swooshPlayer;

        @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_check_codigo);

            final DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("gamecodes");

            swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);
            button_regresar = (Button)findViewById(R.id.button_regresar);
            button_regresar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playSwoosh();
                    Intent intent = new Intent(CheckCodigoActivity.this, Modocompeticion.class);
                    startActivity(intent);
                }
            });
            button_validar = (Button) findViewById(R.id.button_validar);

            button_validar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Collect the digits entered by the user
                    String enteredCode = "";
                    for (int i = 1; i <= 4; i++) {
                        int id = getResources().getIdentifier("editTextCode" + i, "id", getPackageName());
                        EditText editText = findViewById(id);
                        enteredCode += editText.getText().toString();
                    }

                    final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    final DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(userId);
                    String finalEnteredCode = enteredCode;
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String fullName = dataSnapshot.child("fullname").getValue(String.class);
                            validateCode(userId, fullName, finalEnteredCode);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            // Handle database error
                        }
                    });
                }
            });
        }
    private void showCustomAlertDialog(String title, String message, DialogInterface.OnClickListener onPositiveClickListener) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title) // Set the title here
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, onPositiveClickListener)
                .setIcon(R.drawable.afrikanaonelogo) // Set the icon here
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background); // Set the background here
        }

        dialog.show();
    }

    private void validateCode(String userId, String fullName, String enteredCode) {
        DatabaseReference gameCodesRef = FirebaseDatabase.getInstance().getReference("gamecodes");
        gameCodesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean codeFound = false;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String code = String.valueOf(snapshot.child("code").getValue(Long.class));

                    if (enteredCode.equals(code)) {
                        codeFound = true;
                        handleCodeValidation(snapshot, userId, fullName);
                        break;
                    }
                }

                if (!codeFound) {
                    Sounds.playWarningSound(getApplicationContext());
                    showCustomAlertDialog("Attention", "THIS GAME CODE DOESN'T EXIST", null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Sounds.playWarningSound(getApplicationContext());
                showCustomAlertDialog("Error", "Error: " + databaseError.getMessage(), null);
            }
        });
    }
    private void handleCodeValidation(DataSnapshot snapshot, String userId, String fullName) {
        if (snapshot.hasChild("StaticCode")) {
            resetGameData(userId);
            Sounds.playMagicalSound(getApplicationContext());
            showCustomAlertDialog("Attention", "YOUR PROMO CODE HAS BEEN VALIDATED", (dialogInterface, i) -> navigateToModocompeticion());
            return;
        }

        Boolean isUsed = snapshot.child("used").getValue(Boolean.class);
        if (isUsed != null && isUsed) {
            Sounds.playWarningSound(getApplicationContext());
            showCustomAlertDialog("Attention", "THIS CODE HAS ALREADY BEEN USED. ENTER A NEW ONE", (dialogInterface, i) -> {});
            navigateToModocompeticionWithDelay();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(userId);
        userRef.child("currentGameFallos").setValue(0);
        userRef.child("currentGameAciertos").setValue(0);
        userRef.child("currentGamePuntuacion").setValue(0);
        snapshot.getRef().child("usedByUserID").setValue(userId);

        if (fullName != null) {
            snapshot.getRef().child("usedByfullname").setValue(fullName);
        } else {
            snapshot.getRef().child("usedByfullname").setValue("Unknown User");
        }

        // Set code as used and the timestamp here
        snapshot.getRef().child("used").setValue(true);
        DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
        String usedTimestamp = df.format(new Date());
        snapshot.getRef().child("usedTimestamp").setValue(usedTimestamp);
        Sounds.playMagicalSound(getApplicationContext());
        showCustomAlertDialog("Attention", "GAME CODE HAS BEEN VALIDATED. GOOD LUCK", (dialogInterface, i) -> navigateToModocompeticion());
    }

    private void navigateToModocompeticion() {
        playSwoosh();
        Intent intent = new Intent(getApplicationContext(), Modocompeticion.class);
        startActivity(intent);
        finish();
    }

    private void resetGameData(String userId) {
        DatabaseReference userGameRef = FirebaseDatabase.getInstance().getReference("user").child(userId);
        Map<String, Object> gameData = new HashMap<>();
        gameData.put("currentGameAciertos", 0);
        gameData.put("currentGameFallos", 0);
        gameData.put("currentGamePuntuacion", 0);

        userGameRef.updateChildren(gameData, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError != null) {
                    Log.e("CheckCodigoActivity", "Failed to reset game data. Error: " + databaseError.getMessage());
                } else {
                    Log.i("CheckCodigoActivity", "Game data has been successfully reset.");
                }
            }
        });
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

    private void navigateToModocompeticionWithDelay() {
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(getApplicationContext(), Modocompeticion.class);
            startActivity(intent);
            finish();
        }, 2000); // delay for 2 seconds
    }

}




