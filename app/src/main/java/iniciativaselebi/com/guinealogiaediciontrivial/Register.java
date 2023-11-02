package iniciativaselebi.com.guinealogiaediciontrivial;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import Model.User;
public class Register extends AppCompatActivity {
    Button btn_register;
    private FirebaseAuth mAuth;
    String password, email, nombre, ciudad, telefono, barrio, pais;
    MediaPlayer swooshPlayer;
    FirestoreQuestionManager firestoreQuestionManager; // Assume this is your manager class

    TextView TextViewVolver2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        TextViewVolver2 = findViewById(R.id.TextViewVolver2);
        mAuth = FirebaseAuth.getInstance();
        btn_register = findViewById(R.id.btn_register);
        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);
        firestoreQuestionManager = new FirestoreQuestionManager(this); // Initialize your FirestoreQuestionManager

        TextViewVolver2.setOnClickListener(v -> {
            playSwoosh();
            startActivity(new Intent(getApplicationContext(), Login.class));
            finish();
        });

        btn_register.setOnClickListener(v -> {
            EditText editTextNombre = findViewById(R.id.nombre);
            EditText editTextEmail = findViewById(R.id.email);
            EditText editTextPassword = findViewById(R.id.password);
            EditText editTextTelefono = findViewById(R.id.telefono);
            EditText editTextBarrio = findViewById(R.id.barrio);
            EditText editTextCiudad = findViewById(R.id.ciudad);
            EditText editTextPais = findViewById(R.id.pais);

            // Trim the strings to remove any leading or trailing white spaces
            email = editTextEmail.getText().toString().trim();
            password = editTextPassword.getText().toString().trim();
            nombre = editTextNombre.getText().toString().trim();
            telefono = editTextTelefono.getText().toString().trim();
            barrio = editTextBarrio.getText().toString().trim();
            ciudad = editTextCiudad.getText().toString().trim();
            pais = editTextPais.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(nombre) || TextUtils.isEmpty(telefono) || TextUtils.isEmpty(barrio) || TextUtils.isEmpty(ciudad) || TextUtils.isEmpty(pais)) {
                Toast.makeText(Register.this, "Completa todos los datos", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // User registration succeeded
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                User newUser = new User(nombre, email, telefono, barrio, ciudad, pais, "");
                                FirebaseDatabase.getInstance().getReference("user/" + user.getUid())
                                        .setValue(newUser)
                                        .addOnCompleteListener(task1 -> {
                                            if (task1.isSuccessful()) {
                                                Toast.makeText(Register.this, "Usuario Registrado", Toast.LENGTH_SHORT).show();

                                                // Call assignBatchForNewUser from FirestoreQuestionManager
                                                firestoreQuestionManager.assignBatchForNewUser(new FirestoreQuestionManager.BatchAssignmentCallback() {
                                                    @Override
                                                    public void onBatchAssigned(int batchNumber) {
                                                        // Batch has been assigned, proceed to ProfileActivity
                                                        Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
                                                        startActivity(intent);
                                                        finish();
                                                    }

                                                    @Override
                                                    public void onError(Exception e) {
                                                        // Handle the error scenario, maybe retry or prompt the user
                                                        e.printStackTrace();
                                                        // You might want to stay in the register screen or show an error message
                                                    }
                                                });
                                            } else {
                                                Toast.makeText(Register.this, "Error registrando usuario en la base de datos", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            // Handle registration failure
                            task.getException().printStackTrace();
                            Toast.makeText(Register.this, "Error registrando usuario", Toast.LENGTH_SHORT).show();

                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                Toast.makeText(Register.this, "La cuenta ya existe, ¿quieres conectarte?", Toast.LENGTH_SHORT).show();
                            }

                            // Redirect to Login activity
                            startActivity(new Intent(getApplicationContext(), Login.class));
                            playSwoosh();
                            finish();
                        }
                    });
        });
    }

    public interface BatchAssignmentCallback {
        void onBatchAssigned();
        void onError(Exception e);
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


