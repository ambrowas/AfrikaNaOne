package iniciativaselebi.com.guinealogiaediciontrivial;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import iniciativaselebi.com.guinealogiaediciontrivial.R;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import Model.User;
import de.hdodenhof.circleimageview.CircleImageView;

public class LeadersProfileActivity extends AppCompatActivity {


    private CircleImageView profilePic;
    private TextView textviewNoRanking, nameTitle, textViewBarrio, textViewCiudad, textViewPais, textViewPuntuacionAcumulada,textViewAciertosAcumulados, textViewFallosAcumulados, textViewPastaAcumulada, textViewRecord;
    Button buttonatras;
    private ValueAnimator animator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaders_profile);

         profilePic = (CircleImageView) findViewById(R.id.profilepic);
        String userId = getIntent().getStringExtra("user_id");
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(userId);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null && user.getProfilePicture() != null) {
                    Glide.with(LeadersProfileActivity.this)
                            .load(user.getProfilePicture())
                            .placeholder(R.drawable.baseline_account_circle_24) // Add a default placeholder image if needed
                            .into(profilePic);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });





        profilePic = findViewById(R.id.profilepic);
        nameTitle = findViewById(R.id.nameTitle);
        textViewBarrio = findViewById(R.id.textViewBarrio);
        textViewCiudad = findViewById(R.id.textViewCiudad);
        textViewPais = findViewById(R.id.textViewPais);
        textViewRecord = findViewById(R.id.textViewRecord);
        textViewPuntuacionAcumulada = findViewById(R.id.textViewPuntuacionAcumulada);
        textViewPastaAcumulada = findViewById(R.id.textViewPastaAcumulada);
        textViewAciertosAcumulados = findViewById(R.id.textViewAciertosAcumulados);
        textViewFallosAcumulados = findViewById(R.id.textViewFallosAcumulados);

        textviewNoRanking = findViewById(R.id.textviewNoRanking);
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


        buttonatras = (Button)findViewById(R.id.buttonatras);
        buttonatras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LeadersProfileActivity.this, RankingActivity.class);
                finish();
            }
        });



        loadUserData(userId);
    }

    private void loadUserData(String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(userId);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                textviewNoRanking.setText(String.valueOf(user.getPositionInLeaderboard()));
                nameTitle.setText(user.getFullname());
                textViewBarrio.setText( "Barrio:  " +  user.getBarrio());
                textViewCiudad.setText("Ciudad: " + user.getCiudad());
                textViewPais.setText("País: " + user.getPais());
                textViewRecord.setText("Record Personal: " + String.valueOf(user.getHighestScore() + " PUNTOS"));
                textViewPuntuacionAcumulada.setText("Puntuacion acumulada: " + String.valueOf(user.getAccumulatedPuntuacion()+ " PUNTOS"));
                textViewPastaAcumulada.setText(" Pasta acumulada: " + String.valueOf(user.getAccumulatedPuntuacion() + " FCFA"));
                textViewAciertosAcumulados.setText("Aciertos acumulados: " + String.valueOf(user.getAccumulatedAciertos()));
                textViewFallosAcumulados.setText("Fallos acumulados: " + String.valueOf(user.getAccumulatedFallos()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}