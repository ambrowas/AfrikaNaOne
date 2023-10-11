package iniciativaselebi.com.guinealogiaediciontrivial;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import iniciativaselebi.com.guinealogiaediciontrivial.R;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.UploadTask;
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


    TextView textViewRecord, emailAddressTitle, nameTitle, textViewTelefono, textViewBarrio, textViewCiudad, textViewPais, textviewNoRanking;
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

                        emailAddressTitle = findViewById(R.id.emailAddressTitle);
                        emailAddressTitle.setText(user.getEmail());

                        textViewTelefono = findViewById(R.id.textViewTelefono);
                        textViewTelefono.setText(user.getTelefono());

                        textViewBarrio = findViewById(R.id.textViewBarrio);
                        textViewBarrio.setText(user.getBarrio());

                        textViewCiudad = findViewById(R.id.textViewCiudad);
                        textViewCiudad.setText(user.getCiudad());

                        textViewPais = findViewById(R.id.textViewPais);
                        textViewPais.setText(user.getPais());

                        profilepic = findViewById(R.id.profilepic);
                        FirebaseDatabase.getInstance().getReference("user/"+FirebaseAuth.getInstance().getCurrentUser().getUid()+"/profilePicture").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String profilePicUrl = snapshot.getValue(String.class);
                                if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                                    Picasso.get().load(profilePicUrl).into(profilepic);
                                } else {
                                    // handle case where profile picture URL is missing or empty
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(ProfileActivity.this, "Error al cargar datos de usuario", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                }
                loadUserHighestScore();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Error al cargar datos de usuario", Toast.LENGTH_SHORT).show();

            }


        });

        textViewRecord = (TextView)findViewById(R.id.textViewRecord);
        profilepic = (CircleImageView) findViewById(R.id.profilepic);
        btn_borrarusuario = (Button)findViewById(R.id.btn_borrarusuario);

        buttonatras = (Button)findViewById(R.id.buttonatras);
        buttonatras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSwoosh();
                Intent intent = new Intent(ProfileActivity.this, Modocompeticion.class);
                startActivity(intent);
                finish();
            };
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
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setTitle("Confirmar acción");
        builder.setMessage("¿Seguro que quieres borrar este usuario? Esta acción es irreversible.");

        builder.setPositiveButton("SÍ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                reauthenticateAndDelete();  // call re-authentication process
            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
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
                            Toast.makeText(ProfileActivity.this, "Error deleting user from Realtime Database: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Log deleted user
                        logDeletedUser(user.getDisplayName(), user.getEmail());
                        Toast.makeText(ProfileActivity.this, "Usuario y datos asociados borrados con exito.Bye Bye", Toast.LENGTH_SHORT).show();

                        // Navigate back to MenuPrincipal
                        playSwoosh();
                        Intent intent = new Intent(ProfileActivity.this, Menuprincipal.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    Toast.makeText(ProfileActivity.this, "Error deleting user from Firebase Authentication: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(ProfileActivity.this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show();
        }
    }

    private void reauthenticateAndDelete() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            // Prompt the user to re-enter their password
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Reintroduce tu contraseña");

            final EditText passwordField = new EditText(this);
            builder.setView(passwordField);

            builder.setPositiveButton("Enviar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String password = passwordField.getText().toString();

                    // Re-authenticate using email and password
                    AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
                    user.reauthenticate(credential).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            deleteUserProcess();
                        } else {
                            Toast.makeText(ProfileActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            builder.setNegativeButton("Cancelar", null);

            builder.show();
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
                Toast.makeText(ProfileActivity.this, "Failed to save user to database: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
           // Toast.makeText(ProfileActivity.this, "Successfully saved user to the database.", Toast.LENGTH_SHORT).show();
        });
    }

    private void uploadImage() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Subiendo ...");
        progressDialog.show();

        FirebaseStorage.getInstance().getReference("images/"+ UUID.randomUUID().toString()).putFile(imagePath).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if(task.isSuccessful()){
                    task.getResult().getStorage().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()){
                                updateProfilePicture(task.getResult().toString());
                            }
                        }
                    });
                    progressDialog.dismiss();
                    playSwoosh();
                    Toast.makeText(ProfileActivity.this, "Foto Subida", Toast.LENGTH_SHORT).show();
                    btn_upload.setVisibility(View.INVISIBLE);
                }else {
                    Toast.makeText(ProfileActivity.this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
                progressDialog.dismiss();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                double progress = 100.0 * snapshot.getBytesTransferred()/ snapshot.getTotalByteCount();
                progressDialog.setMessage("Subido " + (int) progress + "%");
            }
        });
    }

    private void updateProfilePicture(String url) {
        FirebaseDatabase.getInstance().getReference("user/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "/profilePicture").setValue(url);
        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("profilePictureUri", url);
        editor.apply();

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
                        Toast.makeText(ProfileActivity.this, "¡Nuevo récord alcanzado!", Toast.LENGTH_SHORT).show();
                        textViewRecord.setText("Record: " + newScore + " Puntos"); // Update the textview with the new record value
                    } else {
                        textViewRecord.setText("Record: " + (highestScore == null ? 0 : highestScore + " Puntos"));
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
    private void getImageInImageView() {
        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imagePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        profilepic.setImageBitmap(bitmap);

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
                        Toast.makeText(getApplicationContext(), "El usuario no tiene nivel en este ranking", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(getApplicationContext(), "Error recogiendo datos", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // User is not logged in. Handle this case.
        }
    }

    }