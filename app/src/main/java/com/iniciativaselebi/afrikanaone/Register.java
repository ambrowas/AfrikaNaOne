package com.iniciativaselebi.afrikanaone;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import utils.CountryFlagUtils;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import Model.User;

public class Register extends AppCompatActivity {
    Button btn_register, btn_volver;
    private FirebaseAuth mAuth;
    String password, email, nombre, ciudad, telefono, pais, deviceType;
    MediaPlayer swooshPlayer;
    private Map<String, String> countryFlagMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        FirestoreQuestionManager questionManager = new FirestoreQuestionManager(this);

        // Initialize dropdowns for device type and country
        // Set up device type dropdown
        AutoCompleteTextView dropdownTipoDispositivo = findViewById(R.id.deviceTypeDropdown);
        String[] devices = getResources().getStringArray(R.array.tipo_dispositivo_options);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item, devices);
        dropdownTipoDispositivo.setAdapter(adapter);

// Make the full field clickable to open the dropdown
        dropdownTipoDispositivo.setOnClickListener(v -> {
            Sounds.playSwooshSound(this); // Play swoosh sound
            dropdownTipoDispositivo.showDropDown(); // Programmatically show the dropdown
        });

// Play swoosh sound when a device is selected
        dropdownTipoDispositivo.setOnItemClickListener((parent, view, position, id) -> {
            Sounds.playSwooshSound(this); // Play swoosh sound
        });

// Set up country dropdown
        AutoCompleteTextView paisDropdown = findViewById(R.id.paisDropdown);
        String[] countries = getResources().getStringArray(R.array.countries_array);
        ArrayAdapter<String> countriesAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, countries);
        paisDropdown.setAdapter(countriesAdapter);

// Make the full field clickable to open the dropdown
        paisDropdown.setOnClickListener(v -> {
            Sounds.playSwooshSound(this); // Play swoosh sound
            paisDropdown.showDropDown(); // Programmatically show the dropdown
        });

// Play swoosh sound when the dropdown closes
        paisDropdown.setOnDismissListener(() -> Sounds.playSwooshSound(this));
        // Initialize buttons and Firebase Auth
        btn_volver = findViewById(R.id.btn_volver);
        mAuth = FirebaseAuth.getInstance();
        btn_register = findViewById(R.id.btn_register);
        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);

        // Set up button listeners
        btn_volver.setOnClickListener(v -> {
            playSwoosh();
            startActivity(new Intent(getApplicationContext(), Login.class));
            finish();
        });

        btn_register.setOnClickListener(v -> {
            // Retrieve and sanitize input data
            EditText editTextNombre = findViewById(R.id.nombre);
            EditText editTextEmail = findViewById(R.id.email);
            EditText editTextPassword = findViewById(R.id.password);
            EditText editTextTelefono = findViewById(R.id.telefono);
            EditText editTextCiudad = findViewById(R.id.ciudad);

            nombre = sanitizeInput(editTextNombre.getText().toString().trim());
            email = editTextEmail.getText().toString().trim(); // Email should not be sanitized
            password = editTextPassword.getText().toString().trim();
            telefono = sanitizeInput(editTextTelefono.getText().toString().trim());
            ciudad = sanitizeInput(editTextCiudad.getText().toString().trim());
            pais = paisDropdown.getText().toString();
            deviceType = dropdownTipoDispositivo.getText().toString();

            // Centralized validation
            if (!areAllFieldsValid()) {
                return;
            }

            // Register user in Firebase Authentication
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                String userId = firebaseUser.getUid();
                                fetchFlagUrlAndSaveUser(userId);
                            }
                        } else {
                            handleAuthError(task.getException());
                        }
                    });
        });
    }

    // Fetch the flag URL for the selected country and save the user to the database
    private void fetchFlagUrlAndSaveUser(String userId) {
        Log.d("StoragePath", "Attempting to access: flags/" + pais + ".png");

        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("flags/" + pais + ".png");

        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            String flagUrl = uri.toString();
            Log.d("FlagURL", "Generated public flag URL: " + flagUrl);
            saveUserToDatabase(userId, flagUrl);
        }).addOnFailureListener(e -> {
            Log.e("FirebaseStorage", "Error fetching flag download URL: ", e);
            String defaultFlagUrl = "https://example.com/default_flag.png";
            saveUserToDatabase(userId, defaultFlagUrl);
        });
    }

    // Save the user data to Firebase Realtime Database
    private void saveUserToDatabase(String userId, String flagUrl) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        User newUser = new User(nombre, email, telefono, ciudad, pais, deviceType, currentDate);
        newUser.setCountryAbbreviation(CountryFlagUtils.getCountryAbbreviation(pais));
        newUser.setFlagUrl(flagUrl);

        FirebaseDatabase.getInstance().getReference("user/" + userId)
                .setValue(newUser)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Sounds.playMagicalSound(getApplicationContext()); // Play magical sound
                        showCustomAlertDialog("Success", "User created successfully! Set a profile pic.", this::navigateToProfileActivity);
                    } else {
                        Log.e("DatabaseError", "Error saving user data: ", task.getException());
                        showCustomAlertDialog("Error", "Failed to save user data.");
                    }
                });
    }

    // Handle Firebase Authentication errors
    private void handleAuthError(Exception exception) {
        if (exception instanceof FirebaseAuthUserCollisionException) {
            Sounds.playWarningSound(getApplicationContext());
            showCustomAlertDialog("Attention", "An account with this email already exists. Try logging in.");
        } else {
            Log.e("AuthError", "Error creating user: ", exception);
            Sounds.playWarningSound(getApplicationContext());
            showCustomAlertDialog("Error", "Something went wrong. Please try again.");
        }
    }

    // Validate all input fields
    private boolean areAllFieldsValid() {
        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(telefono) || TextUtils.isEmpty(ciudad) || TextUtils.isEmpty(pais)) {
            showCustomAlertDialog("Attention", "You must complete all fields");
            return false;
        }

        if (!isValidName(nombre)) {
            showCustomAlertDialog("Attention", "Invalid name");
            return false;
        }

        if (!isValidEmail(email)) {
            showCustomAlertDialog("Attention", "Invalid email");
            return false;
        }

        if (!isPasswordValid(password)) {
            showCustomAlertDialog("Attention", "Password must have at least 6 characters");
            return false;
        }

        if (!isValidPhoneNumber(telefono)) {
            showCustomAlertDialog("Attention", "Invalid phone number");
            return false;
        }

        if (!isValidField(ciudad)) {
            showCustomAlertDialog("Attention", "City cannot be empty");
            return false;
        }

        if (!CountryFlagUtils.isValidCountry(pais)) {
            showCustomAlertDialog("Attention", "Invalid country selection.");
            return false;
        }

        if (!isValidDeviceType(deviceType)) {
            showCustomAlertDialog("Attention", "Select a valid device type");
            return false;
        }

        return true;
    }

    // Utility methods for validation
    private boolean isValidName(String name) {
        String namePattern = "^[\\p{L}\\p{M}\\s.'-]+$";
        return name != null && name.matches(namePattern);
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPhoneNumber(String phone) {
        String phonePattern = "^\\+?[0-9]{1,3}[-\\s]?[0-9]{3,15}$";
        return phone.matches(phonePattern);
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 6;
    }

    private boolean isValidField(String field) {
        return field != null && !field.trim().isEmpty();
    }

    private boolean isValidDeviceType(String deviceType) {
        List<String> allowedDevices = Arrays.asList("Apple", "Android");
        return allowedDevices.contains(deviceType);
    }

    private String sanitizeInput(String input) {
        return input.replaceAll("[.#$\\[\\]]", "");
    }

    private void showCustomAlertDialog(String title, String message) {
        showCustomAlertDialog(title, message, null);
    }

    private void showCustomAlertDialog(String title, String message, Runnable onDismiss) {
        // Play the warning sound when the dialog is displayed
        Sounds.playWarningSound(this);

        AlertDialog dialog = new AlertDialog.Builder(Register.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    // Play the swoosh sound on dismissal
                    Sounds.playSwooshSound(this);

                    dialogInterface.dismiss();
                    if (onDismiss != null) onDismiss.run();
                })
                .setIcon(R.drawable.afrikanaonelogo)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        dialog.show();
    }

    private void navigateToProfileActivity() {
        playSwoosh();
        Intent intent = new Intent(Register.this, ProfileActivity.class);
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
}