package iniciativaselebi.com.guinealogiaediciontrivial;


import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Model.User;

public class RankingActivity extends AppCompatActivity {
    TableLayout tableLayout;
    Button button_retorno;
    ValueAnimator animator;
    MediaPlayer swooshPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);
        button_retorno = (Button) findViewById(R.id.button_retorno);
        button_retorno.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSwoosh();
                Intent intent;
                intent = new Intent(RankingActivity.this, Modocompeticion.class);
                startActivity(intent);
                finish();


            }
        });


        loadTopUsers();
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

    private void loadTopUsers() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("user");
        Query topUsersQuery = usersRef.orderByChild("accumulatedPuntuacion").limitToLast(15);

        topUsersQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                tableLayout = findViewById(R.id.tableLayout);

                // Remove all rows except the header
                while (tableLayout.getChildCount() > 1) {
                    tableLayout.removeViewAt(1);
                }

                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                List<DataSnapshot> dataSnapshots = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    dataSnapshots.add(snapshot);
                }
                Collections.reverse(dataSnapshots);
                int rank = 1; // Start ranking from 1
                for (DataSnapshot snapshot : dataSnapshots) {
                    User user = snapshot.getValue(User.class);
                    if (user == null) continue;
                    user.setUid(snapshot.getKey());
                    updateUserPositionInDatabase(user, rank);

                    // Create a new TableRow
                    TableRow row = new TableRow(RankingActivity.this);
                    row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                    row.setPadding(0, 18, 0, 18); // Add vertical padding for increased row height

                    // Create and configure the Rank TextView
                    TextView tvRank = new TextView(RankingActivity.this);
                    tvRank.setText(String.valueOf(rank));
                    tvRank.setLayoutParams(new TableRow.LayoutParams(dpToPx(0, RankingActivity.this), TableRow.LayoutParams.WRAP_CONTENT));
                    tvRank.setGravity(Gravity.LEFT);
                    tvRank.setTextSize(16); // Set text size as needed
                    row.addView(tvRank);

                    // Create and configure the Name TextView
                    TextView tvName = new TextView(RankingActivity.this);
                    tvName.setText(user.fullname);
                    TableRow.LayoutParams nameParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f); // Weighted layout parameters for Name TextView
                    nameParams.setMargins(dpToPx(-20, RankingActivity.this), 0, 0, 0); // Adjusting left margin to bring Name closer to Rank
                    tvName.setGravity(Gravity.LEFT);
                    tvName.setTextSize(16); // Set text size as needed
                    tvName.setOnClickListener(v -> {
                        Intent intent = new Intent(RankingActivity.this, LeadersProfileActivity.class);
                        intent.putExtra("user_id", user.getUid());
                        startActivity(intent);
                    });
                    row.addView(tvName);


// Score TextView
                    // Score TextView
                    TextView tvScore = new TextView(RankingActivity.this);
                    tvScore.setText(String.valueOf(user.getAccumulatedPuntuacion()));
                    TableRow.LayoutParams scoreParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
                    scoreParams.setMargins(dpToPx(-20, RankingActivity.this), 0, 0, 0); // Adjusting left margin
                    tvScore.setLayoutParams(scoreParams);
                    tvScore.setGravity(Gravity.LEFT);
                    tvScore.setTextSize(16); // Set text size as needed
                    tvScore.setMaxLines(1);
                    tvScore.setEllipsize(TextUtils.TruncateAt.END);
                    row.addView(tvScore);


                    tableLayout.addView(row); // Add the TableRow to the TableLayout
                    rank++;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                FirebaseCrashlytics.getInstance().recordException(new Exception("Realtime Database Error: " + error.getMessage()));
            }
        });
    }



    public static int dpToPx(int dp, Context context) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    private void updateUserPositionInDatabase(User user, int newPosition) {
        if (user.getUid() != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user");
            userRef.child(user.getUid())
                    .child("positionInLeaderboard").setValue(newPosition);
        }
    }

    private void checkAndLaunchNumberOneActivity(User user, int rank) {
        String currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (rank == 1 && currentUserUid.equals(user.getUid())) {
            LayoutInflater inflater = getLayoutInflater();
            View toastLayout = inflater.inflate(R.layout.toast_custom, findViewById(R.id.toast_layout));

            ImageView toastImage = toastLayout.findViewById(R.id.toast_image);
            TextView toastText = toastLayout.findViewById(R.id.toast_text);

            toastImage.setImageResource(R.drawable.logotrivial);
            toastText.setText("Felicidades, eres el No.1 del ranking");
            toastText.setGravity(Gravity.CENTER);

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(toastLayout);
            toast.show();
        }
    }
}