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
import android.graphics.drawable.LayerDrawable;
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

    private ImageView imageviewFlag, profilepic;

    TextView textViewRecord, textViewEmail, nameTitle, textViewTelefono, textViewCiudad, textViewPais, textviewNoRanking,
    textViewPuntuacionAcumulada,textViewAciertosAcumulados, textViewFallosAcumulados, textViewPastaAcumulada ;
    private ValueAnimator animator;
    MediaPlayer swooshPlayer;

    private boolean isUpdatingPicture = false; // Initially set to false

    Drawable placeholderDrawable;

    private ValueAnimator growAndShrinkAnimator;

    private ValueAnimator flashingAnimator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // ✅ Step 1: Initialize UI elements
        initializeViews();


        // ✅ Step 2: Ensure profile picture always exists
        ensureProfilePictureExists();
        placeholderDrawable = ContextCompat.getDrawable(this, R.drawable.baseline_account_circle_24);

        // ✅ Step 3: Firebase references
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(user.getUid());

        // ✅ Step 4: Fetch user data & update UI
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        updateUserUI(user); // ✅ Populate UI with user data

                        // ✅ Fetch profile picture separately
                        fetchProfilePicture(snapshot.child("profilePicture").getValue(String.class));

                        // ✅ Fetch country flag separately
                        fetchCountryFlag(snapshot.child("flagUrl").getValue(String.class));


                        // ✅ Ensure FCM Token & Installation ID are updated
                        updateFcmTokenIfNeeded(userRef, snapshot);
                        updateInstallationIdIfNeeded(userRef, snapshot);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ Step 5: Load leaderboard position & highest score
        getPositionInLeaderboard();
        loadUserHighestScore();

        // ✅ Step 6: Setup buttons
        setupButtonListeners();
        checkProfilePictureStatus();
    }

    private void checkProfilePictureStatus() {
        if (!isImageSet || isUsingPlaceholder(profilepic.getDrawable())) {
            startFlashingEffect(); // ✅ Start flashing if no profile picture
        } else {
            stopFlashingEffect(); // ✅ Stop flashing when the picture is set
        }
    }

    private void startFlashingEffect() {
        if (profilepic.getTag() == null || !(boolean) profilepic.getTag()) {
            flashingAnimator = ValueAnimator.ofFloat(1f, 0.5f, 1f);
            flashingAnimator.setDuration(1000); // ✅ Slow down flashing
            flashingAnimator.setRepeatCount(ValueAnimator.INFINITE);
            flashingAnimator.setRepeatMode(ValueAnimator.REVERSE);

            flashingAnimator.addUpdateListener(animation -> {
                float alpha = (float) animation.getAnimatedValue();
                profilepic.setAlpha(alpha);
            });

            flashingAnimator.start();

            // ✅ Change background to "SelectedOptionColor"
            profilepic.setBackgroundColor(ContextCompat.getColor(this, R.color.SelectedOptionColor));
            profilepic.setTag(true); // Mark flashing as active
        }
    }

    private void stopFlashingEffect() {
        if (flashingAnimator != null && flashingAnimator.isRunning()) {
            flashingAnimator.cancel();
            profilepic.setAlpha(1f); // ✅ Restore full visibility

            // ✅ Reset background color to "black"
            profilepic.setBackgroundColor(ContextCompat.getColor(this, R.color.black));

            profilepic.setTag(false); // Mark flashing as inactive
        }
    }
    private void initializeViews() {
        profilepic = findViewById(R.id.profilepic);
        textviewNoRanking = findViewById(R.id.textviewNoRanking);
        textViewRecord = findViewById(R.id.textViewRecord);
        imageviewFlag = findViewById(R.id.imageviewFlag);
        buttonatras = findViewById(R.id.buttonatras);
        btn_upload = findViewById(R.id.btn_upload);
        btn_borrarusuario = findViewById(R.id.btn_borrarusuario);
        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);

        // Add border to the flag image
        addBlackBorderToImageView(imageviewFlag, dpToPx(3, getApplicationContext()));

        // Ensure profilePic settings
        if (profilepic != null) {
            profilepic.setScaleType(ImageView.ScaleType.FIT_XY);
            profilepic.setClipToOutline(true);
            profilepic.setAdjustViewBounds(false);
        }
    }
    private void ensureProfilePictureExists() {
        if (profilepic == null) {
            Log.e("ProfileActivity", "profilepic ImageView is null!");
            return;
        }

        if (profilepic.getDrawable() == null) {
            profilepic.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.baseline_account_circle_24));
        }
    }
    private void updateUserUI(User user) {
        ((TextView) findViewById(R.id.nameTitle)).setText(user.getFullname());
        ((TextView) findViewById(R.id.textViewTelefono)).setText("TELEPHONE: " + user.getTelefono());
        ((TextView) findViewById(R.id.textViewEmail)).setText("EMAIL: " + user.getEmail());
        ((TextView) findViewById(R.id.textViewCiudad)).setText(("CITY: " + user.getCiudad()).toUpperCase());
        ((TextView) findViewById(R.id.textViewPais)).setText(("COUNTRY: " + user.getPais()).toUpperCase());
        ((TextView) findViewById(R.id.textViewPuntuacionAcumulada)).setText("TOTAL SCORE: " + user.getAccumulatedPuntuacion() + " POINTS");
        ((TextView) findViewById(R.id.textViewAciertosAcumulados)).setText("TOTAL CORRECT ANSWERS: " + user.getAccumulatedAciertos());
        ((TextView) findViewById(R.id.textViewFallosAcumulados)).setText("TOTAL INCORRECT ANSWERS: " + user.getAccumulatedFallos());
        ((TextView) findViewById(R.id.textViewPastaAcumulada)).setText("TOTAL CASH: " + user.getAccumulatedPuntuacion() + " AFROS");
    }
    private void fetchProfilePicture(String profilePicUrl) {
        if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
            Picasso.get().load(profilePicUrl).into(profilepic, new Callback() {
                @Override
                public void onSuccess() {
                    isImageSet = true;
                    profilepic.setTag("userImage");
                    stopFlashingEffect(); // ✅ Stop flashing when the picture is set
                    checkProfilePictureStatus(); // ✅ Ensure status is updated
                }

                @Override
                public void onError(Exception e) {
                    setProfilePicturePlaceholder();
                }
            });
        } else {
            setProfilePicturePlaceholder();
        }
    }
    private void setProfilePicturePlaceholder() {
        isImageSet = false;
        profilepic.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.baseline_account_circle_24));
        profilepic.setTag("placeholder");
        startProfilePictureAnimationIfNotSet();
    }
    private void fetchCountryFlag(String flagUrl) {
        if (flagUrl != null && !flagUrl.isEmpty()) {
            Picasso.get()
                    .load(flagUrl)
                    .placeholder(R.drawable.other)
                    .error(R.drawable.other)
                    .into(imageviewFlag, new Callback() {
                        @Override
                        public void onSuccess() {
                            addBlackBorderToImageView(imageviewFlag, dpToPx(3, getApplicationContext()));
                        }

                        @Override
                        public void onError(Exception e) {
                            imageviewFlag.setImageResource(R.drawable.other);
                            addBlackBorderToImageView(imageviewFlag, dpToPx(3, getApplicationContext()));
                        }
                    });
        } else {
            imageviewFlag.setImageResource(R.drawable.other);
            addBlackBorderToImageView(imageviewFlag, dpToPx(3, getApplicationContext()));
        }
    }
    private void setupButtonListeners() {
        buttonatras.setOnClickListener(v -> {
            if (!isImageSet) showExitConfirmation();
            else navigateToModoCompeticion();
        });

        btn_upload.setVisibility(View.INVISIBLE);
        btn_upload.setOnClickListener(v -> {
            uploadImage();
            btn_upload.setVisibility(View.INVISIBLE);
        });

        btn_borrarusuario.setOnClickListener(v -> borrarUsuario());

        profilepic.setOnClickListener(v -> {
            Intent photoIntent = new Intent(Intent.ACTION_PICK);
            photoIntent.setType("image/*");
            startActivityForResult(photoIntent, 1);
            btn_upload.setVisibility(View.VISIBLE);
        });
    }

    private void startProfilePictureAnimationIfNotSet() {
        ensureProfilePictureExists(); // ✅ Ensure the image is set before using it

        Drawable currentDrawable = profilepic.getDrawable();

        if (!isImageSet && isUsingPlaceholder(currentDrawable)) {
            if (growAndShrinkAnimator == null) {
                growAndShrinkAnimator = ValueAnimator.ofFloat(1.0f, 1.2f);
                growAndShrinkAnimator.setDuration(500);
                growAndShrinkAnimator.setRepeatCount(ValueAnimator.INFINITE);
                growAndShrinkAnimator.setRepeatMode(ValueAnimator.REVERSE);

                growAndShrinkAnimator.addUpdateListener(animation -> {
                    if (profilepic != null) {
                        float scale = (float) animation.getAnimatedValue();
                        profilepic.setScaleX(scale);
                        profilepic.setScaleY(scale);
                    }
                });
            }
            growAndShrinkAnimator.start();
        } else {
            stopGrowAndShrinkEffect(); // Stop animation if conditions no longer apply
        }
    }

    private boolean isUsingPlaceholder(Drawable drawable) {
        ensureProfilePictureExists(); // ✅ Ensure the image is set before checking

        // ✅ Ensure placeholderDrawable is initialized
        if (placeholderDrawable == null) {
            placeholderDrawable = ContextCompat.getDrawable(this, R.drawable.baseline_account_circle_24);
        }

        // ✅ Ensure profilepic has a drawable before checking
        Drawable currentDrawable = profilepic.getDrawable();
        if (currentDrawable == null || placeholderDrawable == null) {
            return true; // No image is set, return true
        }

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

    private void addBlackBorderToImageView(ImageView imageView, int borderWidth) {
        // Create a black border drawable
        GradientDrawable borderDrawable = new GradientDrawable();
        borderDrawable.setColor(Color.TRANSPARENT); // Keep background transparent
        borderDrawable.setStroke(borderWidth, Color.BLACK); // Black border

        // Get the current image as a drawable
        Drawable originalDrawable = imageView.getDrawable();
        if (originalDrawable == null) {
            originalDrawable = ContextCompat.getDrawable(this, R.drawable.baseline_account_circle_24);
        }

        // Combine image and border into a LayerDrawable
        Drawable[] layers = new Drawable[]{borderDrawable, originalDrawable};
        LayerDrawable layerDrawable = new LayerDrawable(layers);

        // Ensure the border surrounds the image
        layerDrawable.setLayerInset(1, borderWidth, borderWidth, borderWidth, borderWidth);

        // Apply the border
        imageView.setImageDrawable(layerDrawable);
    }
    public static int dpToPx(int dp, Context context) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    private void showExitConfirmation() {
        DialogInterface.OnClickListener positiveAction = (dialog, which) -> navigateToModoCompeticion();
        DialogInterface.OnClickListener negativeAction = (dialog, which) -> dialog.dismiss();

        // ✅ Pass 'false' to indicate this is a warning message
        showAlertDialogWithActions("Confirm Exit", "Don't you want to set a profile pic?", positiveAction, negativeAction, false);
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

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme) // Apply custom theme
                .setTitle("Delete Account Confirmation")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("DELETE", (dialogInterface, which) -> {
                    playSwoosh(); // Play swoosh sound on confirmation
                    reauthenticateAndDelete(); // Proceed with deletion
                })
                .setNegativeButton("CANCEL", (dialogInterface, which) -> {
                    playSwoosh(); // Play swoosh sound on cancellation
                    dialogInterface.dismiss();
                })
                .setIcon(R.drawable.afrikanaonelogo) // Add logo if needed
                .create();

        // Apply custom background
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        Sounds.playWarningSound(getApplicationContext()); // Warning sound when dialog is shown
        dialog.show();

        // Ensure both buttons have white text
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(Color.WHITE);
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(Color.WHITE);
        }
    }

    private void reauthenticateAndDelete() {
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance(); // Ensure Firebase Auth is initialized
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e("ReauthenticationError", "User not logged in.");
            showInformationalAlertDialog("Error", "User not logged in.", false); // ✅ Fixed call
            return;
        }

        // 🔊 Play warning sound
        Sounds.playWarningSound(getApplicationContext());

        // Prompt user for password
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
                .setTitle("Re-enter your password");

        final EditText passwordField = new EditText(this);
        passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(passwordField);

        builder.setPositiveButton("CONTINUE", (dialog, which) -> {
            Sounds.playSwooshSound(getApplicationContext()); // 🔊 Play swoosh sound
            String password = passwordField.getText().toString().trim();

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(ProfileActivity.this, "You must provide your password.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Authenticate user before proceeding with deletion
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("Reauthentication", "User reauthenticated successfully.");
                    deleteUserProcess();
                } else {
                    Log.e("ReauthenticationError", "Error: " + task.getException().getMessage());
                    showInformationalAlertDialog("Error", "Reauthentication failed: " + task.getException().getLocalizedMessage(), false); // ✅ Fixed call
                }
            });
        });

        builder.setNegativeButton("CANCEL", (dialog, which) -> {
            Sounds.playSwooshSound(getApplicationContext()); // 🔊 Play swoosh sound
            dialog.dismiss();
        });

        AlertDialog passwordDialog = builder.create();
        setDialogBackground(passwordDialog);
        passwordDialog.show();

        // ✅ Ensure button text is white
        setDialogButtonColors(passwordDialog);
    }

    private void deleteUserProcess() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Log.e("DeletionProcess", "User not logged in.");
            showInformationalAlertDialog("Error", "User not logged in.", false); // ❌ Error case
            return;
        }

        String userID = user.getUid();
        String userFullName = user.getDisplayName() != null ? user.getDisplayName() : "Unknown";
        String email = user.getEmail() != null ? user.getEmail() : "Unknown";

        logDeletedUser(userFullName, email);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user").child(userID);
        Log.d("DatabaseDeletion", "Attempting to remove user data for userID: " + userID);

        ref.removeValue((error, ref1) -> {
            if (error != null) {
                Log.e("DatabaseDeletionError", "Error removing user data: " + error.getMessage());
                showInformationalAlertDialog("Error", "Something went wrong: " + error.getMessage(), false); // ❌ Error case
                return;
            }

            Log.d("DatabaseDeletion", "User data removed successfully.");

            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("AuthDeletion", "User account deleted successfully.");

                    // 🔊 Play success sound
                    Sounds.playMagicalSound(getApplicationContext());

                    // ✅ Show success message & navigate to main menu
                    showInformationalAlertDialog("Success", "User and data successfully deleted. Bye Bye", true); // ✅ Success case
                    navigateToMenuPrincipal();
                } else {
                    Log.e("AuthDeletionError", "Failed to delete user account: " + task.getException().getMessage());
                    showInformationalAlertDialog("Error", "Failed to delete user account: " + task.getException().getMessage(), false); // ❌ Error case
                }
            });
        });
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
        if (imagePath == null) {
            Log.e("getImageInImageView", "imagePath is null. Cannot set image.");
            return;
        }

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imagePath);
            profilepic.setImageBitmap(bitmap);
            isImageSet = true;  // ✅ Set flag to true since image is now set
        } catch (IOException e) {
            Log.e("getImageInImageView", "Error loading image: " + e.getMessage());
            isImageSet = false;  // ✅ Reset flag due to error
            setProfilePicturePlaceholder(); // ✅ Set placeholder if loading fails
        }
    }

    private void uploadImage() {
        // Check if an image has been selected
        if (imagePath == null) {
            Sounds.playWarningSound(getApplicationContext()); // 🔊 Play warning sound for missing image
            Log.e("UploadImage", "Image path is null, cannot upload");
            showInformationalAlertDialog("Attention", "No image selected.", false); // ❌ Pass 'false' to indicate failure
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

                            // 🔊 Play success sound
                            Sounds.playMagicalSound(getApplicationContext());

                            // ✅ Show success message
                            showInformationalAlertDialog("Success", "Your profile pic has been set!", true); // ✅ Pass 'true' for success
                            isImageSet = true;
                        } else {
                            Log.e("UploadImage", "Failed to get download URL", urlTask.getException());
                            showInformationalAlertDialog("Error", "Unable to retrieve the image URL.", false); // ❌ Pass 'false' for failure
                            isImageSet = false;
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("UploadImage", "Upload failed: ", e);
                    showInformationalAlertDialog("Error", "Failed to upload image: " + e.getLocalizedMessage(), false); // ❌ Failure case
                    isImageSet = false;
                })
                .addOnCompleteListener(task -> {
                    uploadDialog.dismiss();
                    Log.d("UploadImage", "Upload task complete. Success: " + task.isSuccessful());
                    isUpdatingPicture = false;
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
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this, R.style.CustomAlertDialogTheme)
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

        setDialogButtonColors(dialog);
    }

    private void updateProfilePicture(String url) {
        FirebaseDatabase.getInstance().getReference("user/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "/profilePicture")
                .setValue(url)
                .addOnSuccessListener(aVoid -> Log.d("ProfilePicture", "Profile picture updated in database"))
                .addOnFailureListener(e -> Log.e("ProfilePicture", "Failed to update profile picture in database", e));
    }

    private AlertDialog createUploadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        final ProgressBar progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        progressBar.setLayoutParams(lp);
        builder.setView(progressBar);
        builder.setTitle("Uploading ...")
                .setIcon(R.drawable.afrikanaonelogo);

        AlertDialog dialog = builder.create();
        setDialogBackground(dialog);
        return dialog;
    }

    private void showInformationalAlertDialog(String title, String message, boolean isSuccess) {
        // ✅ Play success or warning sound based on the flag
        if (isSuccess) {
            Sounds.playMagicalSound(getApplicationContext()); // ✅ Play success sound
        } else {
            Sounds.playWarningSound(getApplicationContext()); // ✅ Play warning sound
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    // ✅ Play swoosh sound when dialog is dismissed
                    Sounds.playSwooshSound(getApplicationContext());
                    dialogInterface.dismiss();
                })
                .setIcon(R.drawable.afrikanaonelogo);

        AlertDialog dialog = builder.create();
        setDialogBackground(dialog);
        dialog.show();
        setDialogButtonColors(dialog); // Ensure white text
    }
    private void showCustomAlertDialog(String title, String message, Runnable onDismiss, boolean isSuccess) {
        // ✅ Play the appropriate sound based on success or warning
        if (isSuccess) {
            Sounds.playMagicalSound(getApplicationContext()); // ✅ Success Sound
        } else {
            Sounds.playWarningSound(getApplicationContext()); // 🚨 Warning Sound
        }

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    // ✅ Play swoosh sound when dialog is dismissed
                    Sounds.playSwooshSound(getApplicationContext());
                    if (onDismiss != null) {
                        onDismiss.run();
                    }
                })
                .setIcon(R.drawable.afrikanaonelogo)
                .create();

        // Ensure action is performed when dismissed
        dialog.setOnDismissListener(dialogInterface -> {
            if (onDismiss != null) {
                onDismiss.run();
            }
        });

        setDialogBackground(dialog);
        dialog.show();
        setDialogButtonColors(dialog); // Ensure white text
    }
    private void showAlertDialogWithActions(String title, String message,
                                            DialogInterface.OnClickListener positiveAction,
                                            DialogInterface.OnClickListener negativeAction,
                                            boolean isSuccess) {
        // ✅ Play success or warning sound before showing the dialog
        if (isSuccess) {
            Sounds.playMagicalSound(getApplicationContext()); // ✅ Success sound
        } else {
            Sounds.playWarningSound(getApplicationContext()); // 🚨 Warning sound
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("NO", positiveAction)
                .setNegativeButton("YES", negativeAction != null ? negativeAction : (dialogInterface, i) -> dialogInterface.dismiss())
                .setIcon(R.drawable.afrikanaonelogo);

        AlertDialog dialog = builder.create();
        setDialogBackground(dialog);
        dialog.show();
        setDialogButtonColors(dialog); // Ensure white text
    }
    private void setDialogBackground(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }
    }

    private void setDialogButtonColors(AlertDialog dialog) {
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(Color.WHITE);
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(Color.WHITE);
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
            // ✅ Get the selected image URI
            imagePath = data.getData();

            try {
                // ✅ Convert the image URI into a Bitmap and display it
                getImageInImageView(); // ✅ Call the method here

                // ✅ Show the upload button & start animation
                showUploadButton();
            } catch (Exception e) {
                // ❌ Handle any errors that occur while loading the image
                e.printStackTrace();
                Toast.makeText(this, "⚠️ Failed to load image.", Toast.LENGTH_SHORT).show();
                Sounds.playWarningSound(getApplicationContext()); // 🔊 Play warning sound on error
            }
        } else {
            // ❌ Handle the case where no image was selected
            Log.w("onActivityResult", "⚠️ No image selected.");
            Toast.makeText(this, "⚠️ No image selected.", Toast.LENGTH_SHORT).show();
            Sounds.playWarningSound(getApplicationContext()); // 🔊 Play warning sound on error
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

