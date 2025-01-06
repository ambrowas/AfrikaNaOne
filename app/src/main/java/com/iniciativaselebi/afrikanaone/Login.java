package com.iniciativaselebi.afrikanaone;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
    Button buttonLogIn, btn_crear, btn_return3;
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
            Intent intent = new Intent(getApplicationContext(), Modocompeticion.class);
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
        btn_return3 = findViewById(R.id.btn_return3);


        btn_return3.setOnClickListener(new View.OnClickListener() {
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
                String email = editTextEmail.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    Sounds.playWarningSound(getApplicationContext());
                    showAlert("Attention", "Enter your email");
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    Sounds.playWarningSound(getApplicationContext());
                    showAlert("Attention", "Enter your password");
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    playSwoosh();
                                    Intent intent = new Intent(Login.this, Modocompeticion.class);
                                    startActivity(intent);
                                    finish();
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressBar.setVisibility(View.GONE);
                                if (e instanceof FirebaseAuthInvalidUserException) {
                                    showRegistrationPrompt();
                                } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                    Sounds.playWarningSound(getApplicationContext());
                                    showAlert("Attention", "Incorrect password. Try again");
                                } else {
                                    Sounds.playWarningSound(getApplicationContext());
                                    showAlert("Attention", e.getLocalizedMessage());
                                }
                            }
                        });
            }
        });


    }

    private void showAlert(String title, String message, DialogInterface.OnClickListener positiveClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Login.this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", positiveClickListener != null ? positiveClickListener : new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(R.drawable.afrikanaonelogo);

        AlertDialog dialog = builder.create();

        // Set a dismiss listener to play the swoosh sound
        dialog.setOnDismissListener(dialogInterface -> Sounds.playSwooshSound(Login.this));

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        dialog.show();
    }

    private void showAlert(String title, String message) {
        showAlert(title, message, null);
    }

    private void showRegistrationPrompt() {
        Sounds.playWarningSound(getApplicationContext());
        new AlertDialog.Builder(Login.this)
                .setTitle("Attention")
                .setMessage("No account associated with this email. Do you want to register?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        navigateToRegister();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(R.drawable.afrikanaonelogo)
                .show();
    }

    private void navigateToRegister() {
        playSwoosh();
        Intent intent = new Intent(Login.this, Register.class);
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