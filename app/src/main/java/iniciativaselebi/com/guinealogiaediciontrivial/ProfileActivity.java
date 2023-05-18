package iniciativaselebi.com.guinealogiaediciontrivial;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import Model.User;
import de.hdodenhof.circleimageview.CircleImageView;


public class ProfileActivity extends AppCompatActivity {

    
    private Uri imagePath;
    private CircleImageView profilepic;


    Button btn_jugar, buttonLogout, btn_upload, buttonatras;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(user.getUid());
        getPositionInLeaderboard();

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
        buttonLogout = (Button)findViewById(R.id.btn_logout);

        buttonatras = (Button)findViewById(R.id.buttonatras);
        buttonatras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, Modocompeticion.class);
                startActivity(intent);
                finish();
            };
        });

        btn_jugar = (Button)findViewById(R.id.btn_jugar);
        btn_jugar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Preguntas.class);
                startActivity(intent);
                finish();
            }
        });
        btn_upload = (Button)findViewById(R.id.btn_upload);
        btn_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });

        mAuth = FirebaseAuth.getInstance();
        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(getApplicationContext(), "desconectado", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), Modocompeticion.class);
                startActivity(intent);
                finish();
            }});


        profilepic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoIntent = new Intent(Intent.ACTION_PICK);
                photoIntent.setType("image/*");
                startActivityForResult(photoIntent, 1);

            }
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
                    Toast.makeText(ProfileActivity.this, "Foto Subida", Toast.LENGTH_SHORT).show();
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
                            textviewNoRanking.setText("X");
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