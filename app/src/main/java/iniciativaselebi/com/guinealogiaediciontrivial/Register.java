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

        TextView TextViewVolver2;
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_register);

            TextViewVolver2 = (TextView) findViewById(R.id.TextViewVolver2);
            mAuth = FirebaseAuth.getInstance();
            btn_register = (Button) findViewById(R.id.btn_register);
            swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);

            TextViewVolver2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playSwoosh();
                    Intent intent = new Intent(getApplicationContext(), Login.class);
                    startActivity(intent);
                    finish();
                }
            });

            btn_register.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText editTextNombre = findViewById(R.id.nombre);
                    EditText editTextEmail = findViewById(R.id.email);
                    EditText  editTextPassword = findViewById(R.id.password);
                    EditText editTextTelefono = findViewById(R.id.telefono);
                    EditText editTextBarrio = findViewById(R.id.barrio);
                    EditText editTextCiudad = findViewById(R.id.ciudad);
                    EditText editTextPais = findViewById(R.id.pais);
                    email = editTextEmail.getText().toString();
                    password = editTextPassword.getText().toString();
                    nombre = editTextNombre.getText().toString();
                    telefono = editTextTelefono.getText().toString();
                    barrio = editTextBarrio.getText().toString();
                    ciudad = editTextCiudad.getText().toString();
                    pais = editTextPais.getText().toString();


                    if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(nombre) || TextUtils.isEmpty(telefono) || TextUtils.isEmpty(barrio) || TextUtils.isEmpty(ciudad) || TextUtils.isEmpty(pais)) {
                        Toast.makeText(Register.this, "Completa todos los datos", Toast.LENGTH_SHORT).show();
                        return;
                    }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener( new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseDatabase.getInstance().getReference("user/"+ FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(new User(editTextNombre.getText().toString(), editTextEmail.getText().toString(),editTextTelefono.getText().toString(),editTextBarrio.getText().toString(),editTextCiudad.getText().toString(),editTextPais.getText().toString(), ""));
                                Toast.makeText(Register.this, "Usuario Registrado", Toast.LENGTH_SHORT).show();
                                FirebaseUser user = mAuth.getCurrentUser();
                                playSwoosh();
                                Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                task.getException().printStackTrace();
                                Toast.makeText(Register.this, "Error registrando usuario", Toast.LENGTH_SHORT).show();
                                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                    // User already exists
                                    Toast.makeText(Register.this, "La cuenta ya existe, ¿quieres conectarte?", Toast.LENGTH_SHORT).show();
                                }
                                Intent intent = new Intent(getApplicationContext(), Login.class);
                                playSwoosh();
                                startActivity(intent);
                                finish();
                            }

                            }
                        });


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

    }



