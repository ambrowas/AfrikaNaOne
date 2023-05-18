package iniciativaselebi.com.guinealogiaediciontrivial;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import Model.User;
import de.hdodenhof.circleimageview.CircleImageView;

public class LeadersProfileActivity extends AppCompatActivity {


    private CircleImageView profilePic;
    private TextView position, nameTitle, textViewBarrio, textViewCiudad, textViewPais, textViewRecord, textViewPuntosAcumulados, textViewDineroAcumulado, textViewAciertosAcumulados;
    Button buttonatras;

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
        position = findViewById(R.id.posicion);
        nameTitle = findViewById(R.id.nameTitle);
        textViewBarrio = findViewById(R.id.textViewBarrio);
        textViewCiudad = findViewById(R.id.textViewCiudad);
        textViewPais = findViewById(R.id.textViewPais);
        textViewRecord = findViewById(R.id.textViewRecord);
        textViewPuntosAcumulados = findViewById(R.id.textViewpuntosacumulados);
        textViewDineroAcumulado = findViewById(R.id.textViewdineroacumulado);
        textViewAciertosAcumulados = findViewById(R.id.textViewaciertosacumulados);
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
                position.setText("Puesto en el Ranking Global: " +  String.valueOf(user.getPositionInLeaderboard()));
                nameTitle.setText(user.getFullname());
                textViewBarrio.setText( "Barrio:  " +  user.getBarrio());
                textViewCiudad.setText("Ciudad: " + user.getCiudad());
                textViewPais.setText("País: " + user.getPais());
                textViewRecord.setText("Record Personal: " + String.valueOf(user.getHighestScore() + " PUNTOS"));
                textViewPuntosAcumulados.setText("Puntuacion acumulada: " + String.valueOf(user.getAccumulatedPuntuacion()+ " PUNTOS"));
                textViewDineroAcumulado.setText(" Pasta acumulada: " + String.valueOf(user.getAccumulatedPuntuacion() + " FCFA"));
                textViewAciertosAcumulados.setText("Aciertos acumulados " + String.valueOf(user.getAccumulatedAciertos()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}