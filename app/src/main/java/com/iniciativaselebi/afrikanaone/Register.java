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
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import Model.User;
public class Register extends AppCompatActivity {
    Button btn_register, btn_volver;
    private FirebaseAuth mAuth;
    String password, email, nombre, ciudad, telefono, barrio, pais, deviceType;
    MediaPlayer swooshPlayer;




    @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_register);

            FirestoreQuestionManager questionManager = new FirestoreQuestionManager(this);


            AutoCompleteTextView dropdownTipoDispositivo = findViewById(R.id.deviceTypeDropdown);
            String[] devices = getResources().getStringArray(R.array.tipo_dispositivo_options);
            String[] items = new String[] {"Apple", "Android"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item, items);
            dropdownTipoDispositivo.setAdapter(adapter);

            AutoCompleteTextView paisDropdown = findViewById(R.id.paisDropdown);
            String[] countries = getResources().getStringArray(R.array.countries_array);
            ArrayAdapter<String> countriesAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, countries);
            paisDropdown.setAdapter(countriesAdapter);


            btn_volver = findViewById(R.id.btn_volver);
            mAuth = FirebaseAuth.getInstance();
            btn_register = findViewById(R.id.btn_register);
            swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);

            btn_volver.setOnClickListener(v -> {
                playSwoosh();
                startActivity(new Intent(getApplicationContext(), Login.class));
                finish();
            });

            btn_register.setOnClickListener(v -> {
                // Retrieve input data
                EditText editTextNombre = findViewById(R.id.nombre);
                EditText editTextEmail = findViewById(R.id.email);
                EditText editTextPassword = findViewById(R.id.password);
                EditText editTextTelefono = findViewById(R.id.telefono);
                EditText editTextBarrio = findViewById(R.id.barrio);
                EditText editTextCiudad = findViewById(R.id.ciudad);



                // Set the values
                nombre = editTextNombre.getText().toString().trim();
                email = editTextEmail.getText().toString().trim();
                password = editTextPassword.getText().toString().trim();
                telefono = editTextTelefono.getText().toString().trim();
                barrio = editTextBarrio.getText().toString().trim();
                ciudad = editTextCiudad.getText().toString().trim();
                pais = paisDropdown.getText().toString();
                deviceType = dropdownTipoDispositivo.getText().toString();


                if (!areAllFieldsValid()) {
                    return;
                }

                // Sanitize and trim input data
                nombre = sanitizeInput(editTextNombre.getText().toString().trim());
                telefono = sanitizeInput(editTextTelefono.getText().toString().trim());
                barrio = sanitizeInput(editTextBarrio.getText().toString().trim());
                ciudad = sanitizeInput(editTextCiudad.getText().toString().trim());
                email = editTextEmail.getText().toString().trim();
                password = editTextPassword.getText().toString().trim();
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                String currentDate = sdf.format(new Date());

                // Proceed with Firebase authentication

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                                if (firebaseUser != null) {
                                    User newUser = new User(nombre, email, telefono, barrio, ciudad, pais, "", deviceType, currentDate);
                                    FirebaseDatabase.getInstance().getReference("user/" + firebaseUser.getUid())
                                            .setValue(newUser)
                                            .addOnCompleteListener(task1 -> {
                                                if (task1.isSuccessful()) {
                                                    // Construct the AlertDialog
                                                    AlertDialog dialog = new AlertDialog.Builder(Register.this)
                                                            .setTitle("Success")
                                                            .setMessage("User has been created. Set a profile pic")
                                                            .setPositiveButton(android.R.string.ok, null) // Initially, don't set an OnClickListener
                                                            .setIcon(R.drawable.afrikanaonelogo)
                                                            .create();
                                                    Log.d("MyApp", "Database path: user/" + firebaseUser.getUid());
                                                    dialog.setOnShowListener(dialogInterface -> {
                                                        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                                                        positiveButton.setOnClickListener(view -> {
                                                            // Invoke assignBatchForNewUser method when OK button is clicked
                                                            questionManager.assignBatchForNewUser(new FirestoreQuestionManager.BatchAssignmentCallback() {
                                                                @Override
                                                                public void onBatchAssigned(int batchNumber) {
                                                                    // Batch assignment is successful, dismiss the dialog and navigate
                                                                    dialog.dismiss();
                                                                    navigateToProfileActivity();
                                                                }

                                                                @Override
                                                                public void onError(Exception e) {
                                                                    // Handle batch assignment error
                                                                    dialog.dismiss(); // Dismiss the dialog
                                                                    Log.e("AssignBatchError", "Error assigning batch: ", e);
                                                                    // Optional: Show an error message to the user
                                                                }
                                                            });
                                                        });
                                                    });

                                                    Window window = dialog.getWindow();
                                                    if (window != null) {
                                                        window.setBackgroundDrawableResource(R.drawable.dialog_background);
                                                    }

                                                    dialog.show(); // Display the dialog after setting up the OnShowListener

                                                } else {
                                                    // Handle error on database user registration
                                                    showCustomAlertDialog("Error", "Something went wrong. Try again");
                                                }
                                            })
                                            .addOnFailureListener(e -> Log.e("MyApp", "Error saving user data: ", e)); // Log any failure during saving
                                }
                            } else {
                                // Handle exceptions
                                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                    showCustomAlertDialog("Attention", "An account with this email already exists. Try logging in");
                                } else {
                                    showCustomAlertDialog("Error", "Something went wrong");
                                }
                            }
                        });

            });}

        private String sanitizeInput(String input) {
            // Firebase Realtime Database keys cannot contain '.', '#', '$', '[', or ']'
            return input.replaceAll("[.#$\\[\\]]", "");
        }

        private boolean areAllFieldsValid() {
            if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(telefono) || TextUtils.isEmpty(barrio) || TextUtils.isEmpty(ciudad) || TextUtils.isEmpty(pais)) {
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
            if (!isValidField(barrio)) {
                showCustomAlertDialog("Attention", "Invalid entry");
                return false;
            }
            if (!isValidField(ciudad)) {
                showCustomAlertDialog("Attention", "Invalid entry");
                return false;
            }
            if (!isValidField(pais)) {
                showCustomAlertDialog("Attention", "Invalid entry");
                return false;
            }

            if (TextUtils.isEmpty(deviceType)) {
                showCustomAlertDialog("Attention", "Select your device type");
                return false;
            }

            return true;
        }

        private boolean isValidEmail(String email) {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
        }

        private boolean isValidName(String name) {
            if (name == null || name.trim().isEmpty()) {
                return false;
            }
            // Allow Unicode letters, apostrophes, hyphens, spaces, and numbers
            String namePattern = "[\\p{L}\\s.'-]+|\\d+";
            return name.matches(namePattern);
        }

        private boolean isValidPhoneNumber(String phone) {
            String phonePattern = "^[+0-9]{1,}[0-9\\-\\s]{3,15}$";
            return phone.matches(phonePattern);
        }

        private boolean isPasswordValid(String password) {
            return password.length() >= 6;
        }

        private boolean isValidField(String field) {
            return field != null && !field.trim().isEmpty();
        }

        private void showCustomAlertDialog(String title, String message) {
            AlertDialog dialog = new AlertDialog.Builder(Register.this)
                    .setTitle(title) // Set the title
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss())
                    .setIcon(R.drawable.afrikanaonelogo) // Include the icon
                    .create();

            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(R.drawable.dialog_background);
            }

            dialog.show();
        }

        private void navigateToProfileActivity() {
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
