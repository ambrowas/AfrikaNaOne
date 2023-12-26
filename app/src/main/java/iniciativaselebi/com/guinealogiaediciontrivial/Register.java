package iniciativaselebi.com.guinealogiaediciontrivial;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import iniciativaselebi.com.guinealogiaediciontrivial.R;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.recaptcha.RecaptchaTasksClient;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import org.checkerframework.common.reflection.qual.NewInstance;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import Model.User;
public class Register extends AppCompatActivity {
    Button btn_register;
    private FirebaseAuth mAuth;
    String password, email, nombre, ciudad, telefono, barrio, pais, deviceType;
    MediaPlayer swooshPlayer;

    TextView TextViewVolver2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        FirestoreQuestionManager questionManager = new FirestoreQuestionManager(this);


        AutoCompleteTextView dropdownTipoDispositivo = findViewById(R.id.deviceTypeDropdown);
        String[] devices = getResources().getStringArray(R.array.tipo_dispositivo_options);
        String[] items = new String[] {"Apple", "Android"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, items);
        dropdownTipoDispositivo.setAdapter(adapter);


        TextViewVolver2 = findViewById(R.id.TextViewVolver2);
        mAuth = FirebaseAuth.getInstance();
        btn_register = findViewById(R.id.btn_register);
        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);

        TextViewVolver2.setOnClickListener(v -> {
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
            EditText editTextPais = findViewById(R.id.pais);


            // Set the values
            nombre = editTextNombre.getText().toString().trim();
            email = editTextEmail.getText().toString().trim();
            password = editTextPassword.getText().toString().trim();
            telefono = editTextTelefono.getText().toString().trim();
            barrio = editTextBarrio.getText().toString().trim();
            ciudad = editTextCiudad.getText().toString().trim();
            pais = editTextPais.getText().toString().trim();
            deviceType = dropdownTipoDispositivo.getText().toString();


            if (!areAllFieldsValid()) {
                return;
            }

            // Sanitize and trim input data
            nombre = sanitizeInput(editTextNombre.getText().toString().trim());
            telefono = sanitizeInput(editTextTelefono.getText().toString().trim());
            barrio = sanitizeInput(editTextBarrio.getText().toString().trim());
            ciudad = sanitizeInput(editTextCiudad.getText().toString().trim());
            pais = sanitizeInput(editTextPais.getText().toString().trim());
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
                                                // Construct the AlertDialog and set the positive button action
                                                AlertDialog.Builder builder = new AlertDialog.Builder(Register.this);
                                                builder.setTitle("Éxito") // Set the title of the dialog
                                                        .setMessage("Usuario creado correctamente. Establece una foto de perfil") // Set the message to display
                                                        .setPositiveButton(android.R.string.ok, null) // Configure the positive button
                                                        .setIcon(R.drawable.logotrivial) // Set the icon of the dialog using drawable resource
                                                        .create(); // Create the AlertDialog

// Now, display the AlertDialog and then customize its window
                                                AlertDialog dialog = builder.show(); // Show the dialog and store the reference

// Get the Window of the AlertDialog and set the background drawable resource
                                                Window window = dialog.getWindow(); // Get the window of the dialog
                                                if (window != null) {
                                                    window.setBackgroundDrawableResource(R.drawable.dialog_background); // Set the custom background drawable
                                                }
                                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                                                    // Invoke assignBatchForNewUser method when OK button is clicked
                                                    questionManager.assignBatchForNewUser(new FirestoreQuestionManager.BatchAssignmentCallback() {
                                                        @Override
                                                        public void onBatchAssigned(int batchNumber) {
                                                            // Batch assignment is successful, dismiss the dialog
                                                            dialog.dismiss();
                                                            // Navigate to the profile activity
                                                            navigateToProfileActivity();
                                                        }

                                                        @Override
                                                        public void onError(Exception e) {
                                                            // Handle batch assignment error
                                                            dialog.dismiss(); // Optionally dismiss the dialog
                                                            Log.e("AssignBatch", "Error assigning batch: ", e);
                                                            // Show an error message to the user if necessary
                                                        }
                                                    });
                                                });
                                                // Make sure to define BatchAssignmentCallback in FirestoreQuestionManager class
                                            } else {
                                                showCustomAlertDialog("Error", "Error registrando usuario en la base de datos");
                                            }
                                        });
                            }
                        } else {
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                showCustomAlertDialog("Atención", "La cuenta ya existe, ¿quieres conectarte?");
                            } else {
                                showCustomAlertDialog("Error", "Error registrando usuario");
                            }
                        }
                    });
        });}


    public interface BatchAssignmentCallback {
        void onBatchAssigned(int batchNumber);
        void onError(Exception e);
    }

    private String sanitizeInput(String input) {
        // Firebase Realtime Database keys cannot contain '.', '#', '$', '[', or ']'
        return input.replaceAll("[.#$\\[\\]]", "");
    }

    private boolean areAllFieldsValid() {
        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(telefono) || TextUtils.isEmpty(barrio) || TextUtils.isEmpty(ciudad) || TextUtils.isEmpty(pais)) {
            showCustomAlertDialog("Atención", "Debes completar todos los campos");
            return false;
        }
        if (!isValidName(nombre)) {
            showCustomAlertDialog("Atención", "Nombre inválido");
            return false;
        }
        if (!isValidEmail(email)) {
            showCustomAlertDialog("Atención", "Email inválido");
            return false;
        }
        if (!isPasswordValid(password)) {
            showCustomAlertDialog("Atención", "La contraseña debe tener al menos 6 caracteres");
            return false;
        }
        if (!isValidPhoneNumber(telefono)) {
            showCustomAlertDialog("Atención", "Número de teléfono inválido");
            return false;
        }
        if (!isValidField(barrio)) {
            showCustomAlertDialog("Atención", "Barrio inválido");
            return false;
        }
        if (!isValidField(ciudad)) {
            showCustomAlertDialog("Atención", "Ciudad inválida");
            return false;
        }
        if (!isValidField(pais)) {
            showCustomAlertDialog("Atención", "País inválido");
            return false;
        }

        if (TextUtils.isEmpty(deviceType)) {
            showCustomAlertDialog("Atención", "Elige Dispositivo");
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
                .setIcon(R.drawable.logotrivial) // Include the icon
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        dialog.show();
    }


    private void showCustomAlertDialog(String title, String message, final Runnable onDismiss) {
        AlertDialog dialog = new AlertDialog.Builder(Register.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    if (onDismiss != null) {
                        onDismiss.run();
                    }
                })
                .setIcon(R.drawable.logotrivial) // Include the icon
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        dialog.show();
    }

    private void navigateToProfileActivity() {
        Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
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
