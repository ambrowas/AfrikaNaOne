package com.iniciativaselebi.afrikanaone;


import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import Model.User;
import de.hdodenhof.circleimageview.CircleImageView;


public class ProfileActivity extends AppCompatActivity {

    
    private Uri imagePath;
    private CircleImageView profilepic;


    Button btn_jugar, buttonatras, btn_upload, btn_borrarusuario;
    FirebaseAuth mAuth;

    private FirebaseUser user;
    private DatabaseReference reference;
    private String userID;
    FirebaseAuth fAtuh;
    FirebaseFirestore fStore;
    String userId;
    SharedPreferences preferences;
    Long highestScore;
    private int newScore;
    Long positionInLeaderboard;
    private boolean isImageSet = false;



    TextView textViewRecord, emailAddressTitle, nameTitle, textViewTelefono, textViewBarrio, textViewCiudad, textViewPais, textviewNoRanking,
    textViewPuntuacionAcumulada,textViewAciertosAcumulados, textViewFallosAcumulados, textViewPastaAcumulada ;
    private ValueAnimator animator;
    MediaPlayer swooshPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(user.getUid());
        getPositionInLeaderboard();

        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);



        textviewNoRanking = (TextView) findViewById(R.id.textviewNoRanking);
        final int[] colors = {Color.RED, Color.GREEN, Color.BLUE}; // array of colors to cycle through
        animator = ValueAnimator.ofInt(colors); // create value animator with the array of colors
        animator.setDuration(1000); // set the duration for each color change
        animator.setEvaluator(new ArgbEvaluator()); // set the evaluator to interpolate between the colors
        animator.setRepeatCount(ValueAnimator.INFINITE); // set the repeat count to infinite
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int color = (int) animator.getAnimatedValue(); // get the current color from the animator
                textviewNoRanking.setTextColor(color); // set the text color to the current color
            }
        });
        animator.start(); // start the animator

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        // set the user's name and email to the appropriate UI elements
                        nameTitle = findViewById(R.id.nameTitle);
                        nameTitle.setText(user.getFullname());



                        textViewTelefono = findViewById(R.id.textViewTelefono);
                        textViewTelefono.setText("TELEPHONE: " + user.getTelefono());

                        textViewBarrio = findViewById(R.id.textViewBarrio);
                        textViewBarrio.setText("NEIGHBORHOOD: " + user.getBarrio());

                        textViewCiudad = findViewById(R.id.textViewCiudad);
                        textViewCiudad.setText("CITY: " + user.getCiudad());

                        textViewPais = findViewById(R.id.textViewPais);
                        textViewPais.setText("COUNTRY: " + user.getPais());

                        textViewPuntuacionAcumulada = findViewById(R.id.textViewPuntuacionAcumulada);
                        textViewPuntuacionAcumulada.setText("TOTAL POINTS: " + String.valueOf(user.getAccumulatedPuntuacion() + " POINTS"));

                        textViewAciertosAcumulados = findViewById(R.id.textViewAciertosAcumulados);
                        textViewAciertosAcumulados.setText("TOTAL CORRECT ANSWERS: " + String.valueOf(user.getAccumulatedAciertos()));

                        textViewFallosAcumulados = findViewById(R.id.textViewFallosAcumulados);
                        textViewFallosAcumulados.setText("TOTAL ERRORS: " + String.valueOf(user.getAccumulatedFallos()));

                        textViewPastaAcumulada = findViewById(R.id.textViewPastaAcumulada);
                        textViewPastaAcumulada.setText("TOTAL EARNINGS: " + String.valueOf(user.getAccumulatedPuntuacion() + " AFROS"));

                        profilepic = findViewById(R.id.profilepic);
                        String profilePicUrl = snapshot.child("profilePicture").getValue(String.class);
                        if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                            Picasso.get().load(profilePicUrl).into(profilepic, new Callback() {
                                @Override
                                public void onSuccess() {
                                    isImageSet = true; // Image successfully loaded
                                }

                                @Override
                                public void onError(Exception e) {
                                    isImageSet = false; // Error loading image
                                }
                            });
                        } else {
                            isImageSet = false; // No image URL, hence no image set
                        }

                        // Update FCM token and Installation ID if necessary
                        updateFcmTokenIfNeeded(userRef, snapshot);
                        updateInstallationIdIfNeeded(userRef, snapshot);
                    }
                }
                loadUserHighestScore();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Something went wrong. Try again", Toast.LENGTH_SHORT).show();

            }


        });



        textViewRecord = (TextView)findViewById(R.id.textViewRecord);
        profilepic = (CircleImageView) findViewById(R.id.profilepic);
//        btn_borrarusuario = (Button)findViewById(R.id.btn_borrarusuario);

    buttonatras = (Button)findViewById(R.id.buttonatras);
    buttonatras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isImageSet) {
                    showExitConfirmation();
                } else {
                    navigateToModoCompeticion();
                }
            }
        });




    btn_upload = (Button)findViewById(R.id.btn_upload);
    btn_upload.setVisibility(View.INVISIBLE);
    btn_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });

    mAuth = FirebaseAuth.getInstance();
     
   btn_borrarusuario = (Button)findViewById(R.id.btn_borrarusuario);
   btn_borrarusuario.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
              borraUsuario();
           }
       });

    profilepic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoIntent = new Intent(Intent.ACTION_PICK);
                photoIntent.setType("image/*");
                startActivityForResult(photoIntent, 1);
                btn_upload.setVisibility(View.VISIBLE);

            }
        });
    }

    private void showExitConfirmation() {
        DialogInterface.OnClickListener positiveAction = (dialog, which) -> navigateToModoCompeticion();
        DialogInterface.OnClickListener negativeAction = (dialog, which) -> dialog.dismiss();

        showAlertDialogWithActions("Confirm exit", "Don't u want to set a profile pic?", positiveAction, negativeAction);
    }
    private void navigateToModoCompeticion() {
        playSwoosh();
        Intent intent = new Intent(getApplicationContext(), Modocompeticion.class);
        startActivity(intent);
        finish();
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
    private void borraUsuario() {
        // Positive button listener for confirming user deletion
        DialogInterface.OnClickListener positiveClickListener = (dialog, which) -> reauthenticateAndDelete();

        // Negative button listener for dismissing the dialog
        DialogInterface.OnClickListener negativeClickListener = (dialog, which) -> dialog.dismiss();

        // Show the dialog with the listeners
        showAlertDialogWithActions("Confirm action",
                "U sure you want to delete this user? This action cannot be undone.",
                positiveClickListener,
                negativeClickListener);
    }
    private void deleteUserProcess() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userID = user.getUid();
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Delete user data from Realtime Database
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user").child(userID);
                    ref.removeValue((error, ref1) -> {
                        if (error != null) {
                            showInformationalAlertDialog("Error", "Something went wrong: " + error.getMessage());
                            return;
                        }
                        // Log the deleted user
                        logDeletedUser(user.getDisplayName(), user.getEmail());
                        // Show a success dialog with navigation upon dismissal
                        showCustomAlertDialog("Success", "User and data succesfully deleted. Bye Bye", this::navigateToMenuPrincipal);
                    });
                } else {
                    showInformationalAlertDialog("Error", "Something went wrong: " + task.getException().getMessage());
                }
            });
        } else {
            showInformationalAlertDialog("Error", "Usuario not logged in.");
        }
    }
    private void reauthenticateAndDelete() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            // Prompt the user to re-enter their password
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Re-enter your password");

            final EditText passwordField = new EditText(this);

            // Set input type to password to hide password entries
            passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            builder.setView(passwordField);

            builder.setPositiveButton("Send", (dialog, which) -> {
                String password = passwordField.getText().toString();

                // Check if password field is not empty
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(ProfileActivity.this, "Please enter your password.", Toast.LENGTH_SHORT).show();
                    return; // Don't proceed with re-authentication
                }

                // Re-authenticate using email and password
                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
                user.reauthenticate(credential).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        deleteUserProcess();
                    } else {
                        // Show the error message in the dialog to keep context for the user
                        Toast.makeText(ProfileActivity.this, "Login Error: " + task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

            // Create the AlertDialog and show it
            AlertDialog passwordDialog = builder.create();
            passwordDialog.show();
        }
    }
    private void navigateToMenuPrincipal() {
        playSwoosh();
        Intent intent = new Intent(ProfileActivity.this, Menuprincipal.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    private void logDeletedUser(String userFullName, String email) {
        DatabaseReference deletedUsersRef = FirebaseDatabase.getInstance().getReference().child("deleted_users");
        DatabaseReference userRef = deletedUsersRef.push();

        SimpleDateFormat dateFormatter = new SimpleDateFormat("ddMMyy HHmmss", Locale.getDefault());
        String currentTimestamp = dateFormatter.format(new Date());

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 48);
        String finalDeletionTimestamp = dateFormatter.format(calendar.getTime());

        Map<String, String> userData = new HashMap<>();
        userData.put("fullName", userFullName);
        userData.put("email", email);
        userData.put("currentTimestamp", currentTimestamp);
        userData.put("Final Deletion", finalDeletionTimestamp);

        userRef.setValue(userData, (error, ref) -> {
            if (error != null) {
                Toast.makeText(ProfileActivity.this, "Failed to save user to database: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
           // Toast.makeText(ProfileActivity.this, "Successfully saved user to the database.", Toast.LENGTH_SHORT).show();
        });
    }

    private void getImageInImageView() {
        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imagePath);
            profilepic.setImageBitmap(bitmap);
            isImageSet = true;  // Set flag to true as image is now set
        } catch (IOException e) {
            isImageSet = false; // Reset flag as there was an error loading the image
            throw new RuntimeException(e);
        }
    }

    private void uploadImage() {
        // Check if an image has been selected
        if (imagePath == null) {
            Log.e("UploadImage", "Image path is null, cannot upload");
            showInformationalAlertDialog("Attention", "No image selected");
            return;
        }

        Log.d("UploadImage", "Starting upload process...");

        AlertDialog uploadDialog = createUploadDialog();
        uploadDialog.show();

        // Generate a unique image filename path using UUID
        String imageFilename = "images/" + UUID.randomUUID().toString() + ".jpg";
        Log.d("UploadImage", "Generated image filename: " + imageFilename);

        // Use the correct storage bucket URL
        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReferenceFromUrl("gs://afrikanaone.firebasestorage.app");  // Make sure this matches exactly

        StorageReference fileRef = storageRef.child(imageFilename);

        fileRef.putFile(imagePath)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d("UploadImage", "Upload completed successfully");
                    // Get the download URL for the uploaded image
                    taskSnapshot.getStorage().getDownloadUrl().addOnCompleteListener(urlTask -> {
                        if (urlTask.isSuccessful()) {
                            String imageUrl = urlTask.getResult().toString();
                            Log.d("UploadImage", "Download URL: " + imageUrl);
                            updateProfilePicture(imageUrl);
                            playSwoosh(); // Play a sound effect
                            // Show a success dialog and navigate back to MenuModoCompeticion on dismiss
                            showCustomAlertDialogWithDismissListener("Success", "Profile picture set. Looking kinda cool", dialogInterface -> {
                                navigateToModoCompeticion();
                            });
                            // Hide the upload button after a successful upload
                            btn_upload.setVisibility(View.INVISIBLE);
                        } else {
                            Log.e("UploadImage", "Failed to get download URL", urlTask.getException());
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("UploadImage", "Upload failed: ", e);
                    showInformationalAlertDialog("Error", e.getLocalizedMessage());
                })
                .addOnCompleteListener(task -> {
                    uploadDialog.dismiss();
                    Log.d("UploadImage", "Upload task complete. Success: " + task.isSuccessful());
                })
                .addOnProgressListener(snapshot -> {
                    double progress = 100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount();
                    Log.d("UploadImage", "Upload progress: " + (int) progress + "%");
                    uploadDialog.setMessage("Uploaded " + (int) progress + "%");
                });
    }

    private void updateProfilePicture(String url) {
        FirebaseDatabase.getInstance().getReference("user/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "/profilePicture").setValue(url);
        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("profilePictureUri", url);
        editor.apply();

    }

    private AlertDialog createUploadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Set up the input
        final ProgressBar progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        progressBar.setLayoutParams(lp);
        builder.setView(progressBar);

        // Set up the buttons
        builder.setTitle("Uploading ...")
                .setIcon(R.drawable.afrikanaonelogo);

        AlertDialog dialog = builder.create();

        // Get the Window of the AlertDialog and set the custom background
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        return dialog;
    }


    // Simple method to show an informational alert dialog
    private void showInformationalAlertDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialogInterface, i) -> dialogInterface.dismiss())
                .setIcon(R.drawable.afrikanaonelogo); // Set the custom icon

        // Create the AlertDialog from the builder
        AlertDialog dialog = builder.create();

        // Check if the Window for the AlertDialog is available and set the custom background
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        // Show the dialog to the user
        dialog.show();
    }


    private void showCustomAlertDialog(String title, String message, final Runnable onDismiss) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    if (onDismiss != null) {
                        onDismiss.run();
                    }
                })
                .setIcon(R.drawable.afrikanaonelogo);

        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(dialogInterface -> {
            // The Runnable runs after the dialog is dismissed, including
            // when the positive button is pressed or the dialog is canceled.
            if (onDismiss != null) {
                onDismiss.run();
            }
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        dialog.show();
    }

    private void showAlertDialogWithActions(String title, String message,
                                            DialogInterface.OnClickListener positiveAction,
                                            DialogInterface.OnClickListener negativeAction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("YEP", positiveAction)
                .setNegativeButton("NOPE", negativeAction != null ? negativeAction : (dialogInterface, i) -> dialogInterface.dismiss())
                .setIcon(R.drawable.afrikanaonelogo);
        AlertDialog dialog = builder.create();
        setDialogBackground(dialog);
        dialog.show();
    }

    private void showCustomAlertDialogWithDismissListener(String title, String message, DialogInterface.OnDismissListener dismissListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null) // OK button to dismiss the dialog
                .setIcon(R.drawable.afrikanaonelogo); // Replace with your drawable resource

        AlertDialog dialog = builder.create();

        if (dismissListener != null) {
            dialog.setOnDismissListener(dismissListener);
        }

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        dialog.show();
    }

    // Common method to set dialog background (if required)
    private void setDialogBackground(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);

        }
    }


    private void loadUserHighestScore() {

            final AtomicInteger updatedScore = new AtomicInteger(0);
        DatabaseReference leaderboardRef = FirebaseDatabase.getInstance().getReference("user");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String userId = user.getUid();
            leaderboardRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Integer highestScore = dataSnapshot.child("highestScore").getValue(Integer.class);
                    if (highestScore == null || newScore > highestScore) {
                        dataSnapshot.getRef().child("highestScore").setValue(newScore);
                        Toast.makeText(ProfileActivity.this, "New record established!", Toast.LENGTH_SHORT).show();
                        textViewRecord.setText("RECORD: " + newScore + " POINTS"); // Update the textview with the new record value
                    } else {
                        textViewRecord.setText("RECORD: " + (highestScore == null ? 0 : highestScore + " POINTS"));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==1 && resultCode==RESULT_OK && data !=null){
            imagePath = data.getData();
            getImageInImageView();
        }
    }

    public void getPositionInLeaderboard() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            database.child("user").child(userId).child("positionInLeaderboard").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Long positionInLeaderboard = dataSnapshot.getValue(Long.class);
                        if(positionInLeaderboard == 0){
                            textviewNoRanking.setText("?");
                        } else {
                            textviewNoRanking.setText(String.valueOf(positionInLeaderboard));
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "User is still not up to this level. Keep trying", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // User is not logged in. Handle this case.
        }
    }
    private String getInstallationIdFromPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("appPreferences", MODE_PRIVATE);
        return sharedPreferences.getString("firebaseInstallationId", null);
    }

    private void updateFcmTokenIfNeeded(DatabaseReference userRef, DataSnapshot snapshot) {
        String newFcmToken = getFcmTokenFromPreferences();
        String currentFcmTokenInDb = snapshot.child("fcmToken").getValue(String.class);
        if (newFcmToken != null && !newFcmToken.equals(currentFcmTokenInDb)) {
            userRef.child("fcmToken").setValue(newFcmToken)
                    .addOnSuccessListener(aVoid -> Log.d("ModoCompeticion", "FCM Token updated successfully"))
                    .addOnFailureListener(e -> Log.e("ModoCompeticion", "Failed to update FCM Token", e));
        }
    }

    private void updateInstallationIdIfNeeded(DatabaseReference userRef, DataSnapshot snapshot) {
        String newInstallationId = getInstallationIdFromPreferences();
        String currentInstallationIdInDb = snapshot.child("installationID").getValue(String.class);
        if (newInstallationId != null && !newInstallationId.equals(currentInstallationIdInDb)) {
            userRef.child("installationID").setValue(newInstallationId)
                    .addOnSuccessListener(aVoid -> Log.d("ModoCompeticion", "Installation ID updated successfully"))
                    .addOnFailureListener(e -> Log.e("ModoCompeticion", "Failed to update Installation ID", e));
        }

    }

    private String getFcmTokenFromPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("appPreferences", MODE_PRIVATE);
        return sharedPreferences.getString("fcmToken", null); // Returns null if "fcmToken" doesn't exist
    }


}

