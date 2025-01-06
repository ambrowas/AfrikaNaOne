package com.iniciativaselebi.afrikanaone;


import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

    private ImageView imageviewFlag;

    TextView textViewRecord, textViewEmail, nameTitle, textViewTelefono, textViewCiudad, textViewPais, textviewNoRanking,
    textViewPuntuacionAcumulada,textViewAciertosAcumulados, textViewFallosAcumulados, textViewPastaAcumulada ;
    private ValueAnimator animator;
    MediaPlayer swooshPlayer;

    private boolean isUpdatingPicture = false; // Initially set to false

    Drawable placeholderDrawable;

    private ValueAnimator growAndShrinkAnimator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize views
        profilepic = findViewById(R.id.profilepic);
        textviewNoRanking = findViewById(R.id.textviewNoRanking);
        textViewRecord = findViewById(R.id.textViewRecord);
        imageviewFlag = findViewById(R.id.imageviewFlag);
        buttonatras = findViewById(R.id.buttonatras);
        btn_upload = findViewById(R.id.btn_upload);
        btn_borrarusuario = findViewById(R.id.btn_borrarusuario);

        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);

        // Initialize placeholder drawable for profile picture
        placeholderDrawable = ContextCompat.getDrawable(this, R.drawable.baseline_account_circle_24);
        if (profilepic.getDrawable() == null) {
            profilepic.setImageDrawable(placeholderDrawable);
            profilepic.setTag("placeholder");
        }

        // Firebase user and database reference
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(user.getUid());

        // Set animated color cycling for textviewNoRanking
        final int[] colors = {Color.RED, Color.GREEN, Color.BLUE};
        animator = ValueAnimator.ofInt(colors);
        animator.setDuration(1000);
        animator.setEvaluator(new ArgbEvaluator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> {
            int color = (int) animation.getAnimatedValue();
            textviewNoRanking.setTextColor(color);
        });
        animator.start();

        // Fetch user data from Firebase
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        // Populate user data into UI
                        nameTitle = findViewById(R.id.nameTitle);
                        nameTitle.setText(user.getFullname());

                        textViewTelefono = findViewById(R.id.textViewTelefono);
                        textViewTelefono.setText("TELEPHONE: " + user.getTelefono());

                        textViewEmail= findViewById(R.id.textViewEmail);
                        textViewEmail.setText("EMAIL: " +  user.getEmail());

                        textViewCiudad = findViewById(R.id.textViewCiudad);
                        textViewCiudad.setText(("CITY: " + user.getCiudad()).toUpperCase());

                        textViewPais = findViewById(R.id.textViewPais);
                        textViewPais.setText(("COUNTRY: " + user.getPais()).toUpperCase());

                        textViewPuntuacionAcumulada = findViewById(R.id.textViewPuntuacionAcumulada);
                        textViewPuntuacionAcumulada.setText("TOTAL SCORE: " + user.getAccumulatedPuntuacion() + " POINTS");

                        textViewAciertosAcumulados = findViewById(R.id.textViewAciertosAcumulados);
                        textViewAciertosAcumulados.setText("TOTAL CORRECT ANSWERS: " + user.getAccumulatedAciertos());

                        textViewFallosAcumulados = findViewById(R.id.textViewFallosAcumulados);
                        textViewFallosAcumulados.setText("TOTAL INCORRECT ANSWERS: " + user.getAccumulatedFallos());

                        textViewPastaAcumulada = findViewById(R.id.textViewPastaAcumulada);
                        textViewPastaAcumulada.setText("TOTAL CASH: " + user.getAccumulatedPuntuacion() + " AFROS");

                        updateFcmTokenIfNeeded(userRef, snapshot);
                        updateInstallationIdIfNeeded(userRef, snapshot);

                        // Fetch profile picture
                        String profilePicUrl = snapshot.child("profilePicture").getValue(String.class);
                        if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                            Picasso.get().load(profilePicUrl).into(profilepic, new Callback() {
                                @Override
                                public void onSuccess() {
                                    isImageSet = true;
                                    profilepic.setTag("userImage");
                                    stopGrowAndShrinkEffect(); // Stop the animation if the image is successfully loaded
                                }

                                @Override
                                public void onError(Exception e) {
                                    isImageSet = false;
                                    profilepic.setImageDrawable(placeholderDrawable);
                                    profilepic.setTag("placeholder");
                                    startProfilePictureAnimationIfNotSet(); // Start animation for placeholder
                                }
                            });
                        } else {
                            isImageSet = false;
                            profilepic.setImageDrawable(placeholderDrawable);
                            profilepic.setTag("placeholder");
                            startProfilePictureAnimationIfNotSet(); // Start animation for placeholder
                        }
                        // Fetch and display flag
                        String flagUrl = snapshot.child("flagUrl").getValue(String.class);
                        if (flagUrl != null && !flagUrl.isEmpty()) {
                            Picasso.get()
                                    .load(flagUrl)
                                    .placeholder(R.drawable.other)
                                    .error(R.drawable.other)
                                    .into(imageviewFlag, new Callback() {
                                        @Override
                                        public void onSuccess() {
                                            addBlackBorderToImageView(imageviewFlag, dpToPx(2, getApplicationContext()));
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            imageviewFlag.setImageResource(R.drawable.other);
                                            addBlackBorderToImageView(imageviewFlag, dpToPx(2, getApplicationContext()));
                                        }
                                    });
                        } else {
                            imageviewFlag.setImageResource(R.drawable.other);
                            addBlackBorderToImageView(imageviewFlag, dpToPx(2, getApplicationContext()));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
            }
        });

        // Button click handlers
        buttonatras.setOnClickListener(v -> {
            if (!isImageSet) {
                showExitConfirmation();
            } else {
                navigateToModoCompeticion();
            }
        });

        btn_upload.setVisibility(View.INVISIBLE);
        btn_upload.setOnClickListener(v -> {
            uploadImage();
            btn_upload.setVisibility(View.INVISIBLE); // Hide button after upload
        });

        btn_borrarusuario.setOnClickListener(v -> borrarUsuario());

        profilepic.setOnClickListener(v -> {
            Intent photoIntent = new Intent(Intent.ACTION_PICK);
            photoIntent.setType("image/*");
            startActivityForResult(photoIntent, 1);
            btn_upload.setVisibility(View.VISIBLE);
        });

        // Load leaderboard position
        getPositionInLeaderboard();
        loadUserHighestScore();
    }


    private void startProfilePictureAnimationIfNotSet() {
        if (!isImageSet && isUsingPlaceholder(profilepic.getDrawable())) {
            if (growAndShrinkAnimator == null) {
                growAndShrinkAnimator = ValueAnimator.ofFloat(1.0f, 1.2f);
                growAndShrinkAnimator.setDuration(500);
                growAndShrinkAnimator.setRepeatCount(ValueAnimator.INFINITE);
                growAndShrinkAnimator.setRepeatMode(ValueAnimator.REVERSE);

                growAndShrinkAnimator.addUpdateListener(animation -> {
                    float scale = (float) animation.getAnimatedValue();
                    profilepic.setScaleX(scale);
                    profilepic.setScaleY(scale);
                });
            }
            growAndShrinkAnimator.start();
        } else {
            stopGrowAndShrinkEffect(); // Stop animation if conditions no longer apply
        }
    }

    private boolean isUsingPlaceholder(Drawable drawable) {
        if (profilepic.getDrawable() == null) {
            return true; // No image is set
        }

        // Compare the current drawable with the placeholder
        Drawable currentDrawable = profilepic.getDrawable();
        return currentDrawable.getConstantState() != null &&
                placeholderDrawable.getConstantState() != null &&
                currentDrawable.getConstantState().equals(placeholderDrawable.getConstantState());
    }

    private void stopGrowAndShrinkEffect() {
        if (growAndShrinkAnimator != null && growAndShrinkAnimator.isRunning()) {
            growAndShrinkAnimator.cancel();
            profilepic.setScaleX(1.0f);
            profilepic.setScaleY(1.0f);
        }
    }

    // Add a border to an ImageView
    private void addBlackBorderToImageView(ImageView imageView, int borderWidth) {
        GradientDrawable borderDrawable = new GradientDrawable();
        borderDrawable.setColor(Color.TRANSPARENT); // Transparent background
        borderDrawable.setStroke(borderWidth, Color.BLACK); // Black border with specified width
        imageView.setBackground(borderDrawable);
        imageView.setPadding(borderWidth, borderWidth, borderWidth, borderWidth); // Add padding for border visibility
    }

    public static int dpToPx(int dp, Context context) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    private void showExitConfirmation() {
        DialogInterface.OnClickListener positiveAction = (dialog, which) -> navigateToModoCompeticion();
        DialogInterface.OnClickListener negativeAction = (dialog, which) -> dialog.dismiss();
        Sounds.playWarningSound(getApplicationContext());
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
    private void borrarUsuario() {
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance(); // Ensure mAuth is initialized
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Delete Account Confirmation")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("DELETE", (dialog, which) -> {
                    playSwoosh(); // Play swoosh sound on confirmation
                    reauthenticateAndDelete(); // Proceed with deletion
                })
                .setNegativeButton("CANCEL", (dialog, which) -> {
                    playSwoosh(); // Play swoosh sound on cancellation
                    dialog.dismiss();
                })
                .setIcon(R.drawable.afrikanaonelogo); // Add your logo if desired

        AlertDialog dialog = builder.create();
        setDialogBackground(dialog); // Apply custom border styling
        dialog.show();

        Sounds.playWarningSound(getApplicationContext()); // Warning sound when dialog is shown
    }

    private void reauthenticateAndDelete() {
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance(); // Ensure mAuth is initialized
        }

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            // Prompt the user to re-enter their password
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Re-enter your password");

            final EditText passwordField = new EditText(this);

            // Set input type to password to hide password entries
            passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            builder.setView(passwordField);

            builder.setPositiveButton("CONTINUE", (dialog, which) -> {
                playSwoosh(); // Play swoosh on pressing CONTINUE
                String password = passwordField.getText().toString();

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(ProfileActivity.this, "You must provide your password.", Toast.LENGTH_SHORT).show();
                    return;
                }

                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
                user.reauthenticate(credential).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Reauthentication", "User reauthenticated successfully.");
                        deleteUserProcess();
                    } else {
                        Log.e("ReauthenticationError", "Error: " + task.getException().getMessage());
                        Toast.makeText(ProfileActivity.this, "Reauthentication failed: " + task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });

            builder.setNegativeButton("CANCEL", (dialog, which) -> {
                playSwoosh();
                dialog.dismiss();
            });

            AlertDialog passwordDialog = builder.create();
            setDialogBackground(passwordDialog);
            passwordDialog.show();
            playSwoosh();
        } else {
            Log.e("ReauthenticationError", "User not logged in.");
            showInformationalAlertDialog("Error", "User not logged in.");
        }
    }

    private void deleteUserProcess() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            String userID = user.getUid();
            String userFullName = user.getDisplayName() != null ? user.getDisplayName() : "Unknown";
            String email = user.getEmail() != null ? user.getEmail() : "Unknown";

            logDeletedUser(userFullName, email);

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user").child(userID);
            Log.d("DatabaseDeletion", "Attempting to remove user data for userID: " + userID);

            ref.removeValue((error, ref1) -> {
                if (error != null) {
                    Log.e("DatabaseDeletionError", "Error removing user data: " + error.getMessage());
                    showInformationalAlertDialog("Error", "Something went wrong: " + error.getMessage());
                    return;
                }

                Log.d("DatabaseDeletion", "User data removed successfully.");

                user.delete().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("AuthDeletion", "User account deleted successfully.");
                        Sounds.playMagicalSound(getApplicationContext());
                        showCustomAlertDialog("Success", "User and data successfully deleted. Bye Bye", this::navigateToMenuPrincipal);
                    } else {
                        Log.e("AuthDeletionError", "Failed to delete user account: " + task.getException().getMessage());
                        showInformationalAlertDialog("Error", "Failed to delete user account: " + task.getException().getMessage());
                    }
                });
            });
        } else {
            Log.e("DeletionProcess", "User not logged in.");
            showInformationalAlertDialog("Error", "User not logged in.");
        }
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
                Log.e("LogDeletedUserError", "Failed to save deleted user: " + error.getMessage());
                Toast.makeText(ProfileActivity.this, "Failed to log deleted user: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            } else {
                Log.d("LogDeletedUser", "Deleted user logged successfully.");
            }
        });
    }

    private void navigateToMenuPrincipal() {
        playSwoosh();
        Intent intent = new Intent(ProfileActivity.this, Menuprincipal.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
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
            showInformationalAlertDialog("Attention", "No image selected.");
            return;
        }

        Log.d("UploadImage", "Starting upload process...");
        isUpdatingPicture = true; // Indicate that the user is updating their profile picture

        // Show progress dialog
        AlertDialog uploadDialog = createUploadDialog();
        uploadDialog.show();

        // Generate a unique image filename using UUID
        String imageFilename = "images/" + UUID.randomUUID().toString() + ".jpg";
        Log.d("UploadImage", "Generated image filename: " + imageFilename);

        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReferenceFromUrl("gs://afrikanaone.firebasestorage.app");
        StorageReference fileRef = storageRef.child(imageFilename);

        fileRef.putFile(imagePath)
                .addOnSuccessListener(taskSnapshot -> {
                    taskSnapshot.getStorage().getDownloadUrl().addOnCompleteListener(urlTask -> {
                        if (urlTask.isSuccessful()) {
                            String profilePicUrl = urlTask.getResult().toString();
                            Log.d("UploadImage", "Download URL: " + profilePicUrl);

                            // Save the profile picture URL to Firebase and SharedPreferences
                            updateProfilePicture(profilePicUrl);
                            saveProfilePictureToPreferences(profilePicUrl);
                            hideUploadButton();

                            // Notify the user of success
                            Sounds.playMagicalSound(getApplicationContext());
                            showSuccessDialog("Your profile pic has been set!");
                            isImageSet = true; // Update the flag
                        } else {
                            Log.e("UploadImage", "Failed to get download URL", urlTask.getException());
                            showInformationalAlertDialog("Error", "Unable to retrieve the image URL.");
                            isImageSet = false;
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("UploadImage", "Upload failed: ", e);
                    showInformationalAlertDialog("Error", "Failed to upload image: " + e.getLocalizedMessage());
                    isImageSet = false;
                })
                .addOnCompleteListener(task -> {
                    uploadDialog.dismiss();
                    Log.d("UploadImage", "Upload task complete. Success: " + task.isSuccessful());
                    isUpdatingPicture = false; // Reset the flag
                })
                .addOnProgressListener(snapshot -> {
                    double progress = 100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount();
                    Log.d("UploadImage", "Upload progress: " + (int) progress + "%");
                    uploadDialog.setMessage("Uploaded " + (int) progress + "%");
                });
    }

    private void flashUploadButton() {
        if (btn_upload.getVisibility() == View.VISIBLE) {
            ObjectAnimator flashingAnimator = ObjectAnimator.ofFloat(btn_upload, "alpha", 1f, 0.5f, 1f);
            flashingAnimator.setDuration(500); // Duration for one flash cycle
            flashingAnimator.setRepeatCount(ValueAnimator.INFINITE); // Repeat indefinitely
            flashingAnimator.start();
        }
    }

    private void showUploadButton() {
        btn_upload.setVisibility(View.VISIBLE);
        flashUploadButton(); // Start flashing effect
    }

    private void hideUploadButton() {
        btn_upload.setVisibility(View.INVISIBLE);
        btn_upload.setAlpha(1f); // Reset alpha to avoid lingering effects
    }
    private void saveProfilePictureToPreferences(String profilePicUrl) {
        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("storedProfilePicUrl", profilePicUrl);
        editor.apply();
        Log.d("ProfilePicture", "Profile picture URL saved to SharedPreferences.");
    }

    private void showSuccessDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this)
                .setTitle("Djudju Black Magic")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    playSwoosh();
                    dialog.dismiss();
                })
                .setIcon(R.drawable.afrikanaonelogo);

        AlertDialog dialog = builder.create();
        setDialogBackground(dialog);
        dialog.show();
    }

    private void updateProfilePicture(String url) {
        FirebaseDatabase.getInstance().getReference("user/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "/profilePicture")
                .setValue(url)
                .addOnSuccessListener(aVoid -> Log.d("ProfilePicture", "Profile picture updated in database"))
                .addOnFailureListener(e -> Log.e("ProfilePicture", "Failed to update profile picture in database", e));
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
                .setPositiveButton("NO", positiveAction)
                .setNegativeButton("YES", negativeAction != null ? negativeAction : (dialogInterface, i) -> dialogInterface.dismiss())
                .setIcon(R.drawable.afrikanaonelogo);
        AlertDialog dialog = builder.create();
        setDialogBackground(dialog);
        dialog.show();
    }

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

        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            // Get the URI of the selected image
            imagePath = data.getData();

            try {
                // Convert the selected image to a Bitmap and display it in the profile picture view
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imagePath);
                profilepic.setImageBitmap(bitmap);

                // Make the upload button visible and start the flashing effect
                showUploadButton();
            } catch (IOException e) {
                // Handle any errors that occur while loading the image
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show();
            }
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
                       // Toast.makeText(getApplicationContext(), "User is still not up to this level. Keep trying", Toast.LENGTH_SHORT).show();
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

