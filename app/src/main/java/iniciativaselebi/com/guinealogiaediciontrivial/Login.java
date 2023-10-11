package iniciativaselebi.com.guinealogiaediciontrivial;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import iniciativaselebi.com.guinealogiaediciontrivial.R;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {

    TextInputEditText editTextEmail, editTextPassword;
    Button buttonLogIn, btn_crear;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    MediaPlayer swooshPlayer;

    TextView TextViewVolver3;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            Intent intent = new Intent(getApplicationContext(), Preguntas.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);
        mAuth = FirebaseAuth.getInstance();
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonLogIn = findViewById(R.id.btn_login);
        btn_crear = findViewById(R.id.btn_crear);
        progressBar = findViewById(R.id.progressBar);
        TextViewVolver3 = findViewById(R.id.TextViewVolver3);


        TextViewVolver3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSwoosh();
                Intent intent = new Intent(getApplicationContext(), Modocompeticion.class);
                startActivity(intent);
                finish();
            }
        });

        btn_crear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSwoosh();
                Intent intent = new Intent(Login.this, Register.class);
                startActivity(intent);
                finish();

            }
        });



// Create a blinking animation for the TextView
        AlphaAnimation blinkAnimation = new AlphaAnimation(1, 0);
        blinkAnimation.setDuration(500);
        blinkAnimation.setInterpolator(new AccelerateInterpolator());
        blinkAnimation.setRepeatCount(Animation.INFINITE);
        blinkAnimation.setRepeatMode(Animation.REVERSE);



        buttonLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                String email, password;
                email = String.valueOf(editTextEmail.getText());
                password = String.valueOf(editTextPassword.getText());

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(Login.this, "Introduce tu email", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(Login.this, "Introduce tu contraseña", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    String profilePicUrl = String.valueOf(user.getPhotoUrl());
                                    if (profilePicUrl != null) {
                                        Toast.makeText(Login.this, "Conectado", Toast.LENGTH_SHORT).show();
                                        playSwoosh();
                                        Intent intent = new Intent(Login.this, Modocompeticion.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(Login.this, "Sube una imagen de perfil", Toast.LENGTH_SHORT).show();
                                        playSwoosh();
                                        Intent intent = new Intent(Login.this, ProfileActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }

                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressBar.setVisibility(View.GONE);
                                if (e instanceof FirebaseAuthInvalidUserException) {
                                    Toast.makeText(Login.this, "No hay ninguna cuenta asociada con ese email. Quieres registrarte?", Toast.LENGTH_LONG).show();
                                } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                    Toast.makeText(Login.this, "Contraseña incorrecta. Intentalo de nuevo", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(Login.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
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