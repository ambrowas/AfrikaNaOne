package iniciativaselebi.com.guinealogiaediciontrivial;


import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
        button_retorno = (Button)findViewById(R.id.button_retorno);
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
        Query topUsersQuery = usersRef.orderByChild("accumulatedPuntuacion").limitToLast(25);

        topUsersQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                tableLayout = findViewById(R.id.tableLayout);

                // Remove all rows except the header
                while (tableLayout.getChildCount() > 1) {
                    tableLayout.removeViewAt(1);
                }


                List<DataSnapshot> dataSnapshots = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    dataSnapshots.add(snapshot);
                }
                Collections.reverse(dataSnapshots);

                int rank = 1;
                for (DataSnapshot snapshot : dataSnapshots) {
                    User user = snapshot.getValue(User.class);
                    user.setUid(snapshot.getKey());

                    updateUserPositionInDatabase(user, rank);
                    checkAndLaunchNumberOneActivity(user, rank);


                    TableRow row = new TableRow(RankingActivity.this);

                    String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    if (currentUserId.equals(user.getUid())) {
                        if (user.getUid().equals(currentUserId)) {
                            final int[] colors = {Color.RED, Color.GREEN, Color.BLUE};
                            animator = ValueAnimator.ofInt(colors);
                            animator.setDuration(1000);
                            animator.setEvaluator(new ArgbEvaluator());
                            animator.setRepeatCount(ValueAnimator.INFINITE);

                            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animator) {
                                    int color = (int) animator.getAnimatedValue();
                                    // Set the text color for all TextViews in the row
                                    for (int i = 0; i < row.getChildCount(); i++) {
                                        View child = row.getChildAt(i);
                                        if (child instanceof TextView) {
                                            ((TextView) child).setTextColor(color);
                                        }
                                    }
                                }
                            });
                            animator.start();
                        }

                    }

                    row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));

                    TextView tvRank = new TextView(RankingActivity.this);
                    tvRank.setText(String.valueOf(rank));
                    tvRank.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1));
                    tvRank.setGravity(Gravity.LEFT);
                    tvRank.setEllipsize(TextUtils.TruncateAt.END);
                    row.addView(tvRank);

                    TextView tvName = new TextView(RankingActivity.this);
                    tvName.setText(user.fullname);
                    tvName.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                    tvName.setGravity(Gravity.START);
                    tvName.setMaxLines(1);
                    tvName.setEllipsize(TextUtils.TruncateAt.END);
                    row.addView(tvName);

                    tvName.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(RankingActivity.this, LeadersProfileActivity.class);
                            intent.putExtra("user_id", user.getUid());
                            playSwoosh();
                            startActivity(intent);
                        }
                    });


                    TextView tvCity = new TextView(RankingActivity.this);
                    tvCity.setText(user.ciudad);
                    tvCity.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                    tvCity.setGravity(Gravity.LEFT);
                    tvCity.setMaxLines(1);
                    tvCity.setEllipsize(TextUtils.TruncateAt.END);
                    row.addView(tvCity);

                    TextView tvScore = new TextView(RankingActivity.this);
                    try {
                        tvScore.setText(String.valueOf(user.getAccumulatedPuntuacion()));
                    } catch (Exception e) {
                        Log.e("RankingActivity", "Error setting tvScore text", e);
                        tvScore.setText("0");
                    }
                    tvScore.setText(String.valueOf(user.getAccumulatedPuntuacion()));
                    tvScore.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                    tvScore.setGravity(Gravity.LEFT);
                    tvScore.setMaxLines(1);
                    tvScore.setEllipsize(TextUtils.TruncateAt.END);
                    row.addView(tvScore);


                    tableLayout.addView(row);
                    rank++;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                FirebaseCrashlytics.getInstance().recordException(new Exception("Realtime Database Error: " + error.getMessage()));

            }
        });
    }

    private void updateUserPositionInDatabase(User user, int newPosition) {
        if (user.getUid() != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user");
            userRef.child(user.getUid())
                    .child("positionInLeaderboard").setValue(newPosition);
        }
    }


    // Usar este metodo cuando decidas celebrar adquirir puesto No.1
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




