package com.iniciativaselebi.afrikanaone;


import static utils.CountryFlagUtils.getCountryAbbreviation;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Model.User;

public class RankingActivity extends AppCompatActivity {
    TableLayout tableLayout;
    Button button_retorno;
    private Map<String, ValueAnimator> flashingAnimators = new HashMap<>();
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
        // Stop all flashing animations
        for (ValueAnimator animator : flashingAnimators.values()) {
            if (animator != null) {
                animator.cancel();
            }
        }
        flashingAnimators.clear(); // Clear the map with animators

        if (swooshPlayer != null) {
            swooshPlayer.release();
            swooshPlayer = null;
        }

        super.onDestroy();
    }

    private TextView createDynamicTextView(Context context, String text, float initialFontSize, int gravity, int maxWidth) {
        TextView textView = new TextView(context);

        // Set initial properties
        textView.setText(text != null ? text : ""); // Use an empty string if text is null
        textView.setTextSize(initialFontSize);
        textView.setGravity(gravity);
        textView.setTextColor(context.getResources().getColor(android.R.color.white));
        textView.setMaxLines(1); // Restrict to a single line
        textView.setEllipsize(TextUtils.TruncateAt.END); // Ellipsize if needed

        // Dynamically adjust the font size to ensure the text fits in a single line
        textView.post(() -> {
            int availableWidth = maxWidth > 0 ? maxWidth : textView.getWidth(); // Use maxWidth if provided
            Paint paint = new Paint();
            paint.setTextSize(initialFontSize);

            // Ensure that the text is not null
            String safeText = text != null ? text : "";
            float textWidth = paint.measureText(safeText);

            float fontSize = initialFontSize;
            while (textWidth > availableWidth && fontSize > 8) { // Minimum font size is 8
                fontSize -= 1;
                paint.setTextSize(fontSize);
                textWidth = paint.measureText(safeText);
            }

            textView.setTextSize(fontSize);
        });

        return textView;
    }
    private void loadTopUsers() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("user");
        Query topUsersQuery = usersRef.orderByChild("accumulatedPuntuacion").limitToLast(20);

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
                    row.setPadding(0, 22, 0, 22); // Add vertical padding for increased row height

                    if (user.getUid().equals(currentUserId)) {
                        startFlashingAnimation(user.getUid(), row);
                    }

                    // Rank Column
                    TextView tvRank = createDynamicTextView(
                            RankingActivity.this,
                            String.valueOf(rank),
                            14,
                            Gravity.LEFT,
                            dpToPx(50, RankingActivity.this)
                    );
                    row.addView(tvRank);

// Name Column
                    String fullname = user.fullname != null ? user.fullname : "Unknown User"; // Default to "Unknown User" if null
                    TextView tvName = createDynamicTextView(
                            RankingActivity.this,
                            fullname,
                            14,
                            Gravity.LEFT,
                            dpToPx(120, RankingActivity.this)
                    );

// Add click listener to navigate to the user's profile
                    tvName.setOnClickListener(v -> {
                        playSwoosh();
                        Intent intent = new Intent(RankingActivity.this, LeadersProfileActivity.class);
                        intent.putExtra("user_id", user.getUid());
                        startActivity(intent);
                    });

                    row.addView(tvName);
                    ImageView flagView = new ImageView(RankingActivity.this);
                    int originalSize = dpToPx(45, RankingActivity.this); // Original size in dp
                    int reducedSize = (int) (originalSize * 0.50); // Reduce size by 50%
                    TableRow.LayoutParams flagParams = new TableRow.LayoutParams(reducedSize, reducedSize); // Reduced width and height
                    flagParams.gravity = Gravity.CENTER; // Center alignment
                    flagView.setLayoutParams(flagParams);

// Scale the image properly within the smaller square
                    flagView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    flagView.setAdjustViewBounds(true);

// Remove any existing padding
                    flagView.setPadding(0, 0, 0, 0);

// Load the flag URL or set a default image
                    if (user.getFlagUrl() != null) {
                        Picasso.get().load(user.getFlagUrl()).into(flagView, new Callback() {
                            @Override
                            public void onSuccess() {
                                // Add black border to the flag image
                                addBlackBorderToImageView(flagView, dpToPx(2, getApplicationContext())); // Border width = 2dp
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e("ImageLoadError", "Failed to load flag image", e);
                                flagView.setImageResource(R.drawable.other); // Default flag
                                addBlackBorderToImageView(flagView, dpToPx(2, getApplicationContext())); // Border width = 2dp
                            }
                        });
                    } else {
                        flagView.setImageResource(R.drawable.other); // Default flag
                        addBlackBorderToImageView(flagView, dpToPx(2, getApplicationContext())); // Border width = 2dp
                    }

// Create TextView for the abbreviation
                    TextView abbreviationView = createDynamicTextView(
                            RankingActivity.this,
                            getCountryAbbreviation(user.getPais()), // Use the abbreviation function
                            14,
                            Gravity.CENTER,
                            dpToPx(50, RankingActivity.this)
                    );

                    row.addView(flagView); // Add the reduced-size flag ImageView to the row
                    row.addView(abbreviationView); // Add the country abbreviation TextView to the row
                    // Start alternating between flag and abbreviation
                    startAlternatingCountryColumn(row, flagView, abbreviationView);

                    // Score Column
                    TextView tvScore = createDynamicTextView(
                            RankingActivity.this,
                            String.valueOf(user.getAccumulatedPuntuacion()),
                            14,
                            Gravity.RIGHT,
                            dpToPx(70, RankingActivity.this)
                    );
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

    private void addBlackBorderToImageView(ImageView imageView, int borderWidth) {
        // Create a GradientDrawable with a square shape
        GradientDrawable borderDrawable = new GradientDrawable();
        borderDrawable.setColor(Color.TRANSPARENT); // Transparent background
        borderDrawable.setStroke(borderWidth, Color.BLACK); // Black border

        // Force the drawable to use a square shape
        borderDrawable.setShape(GradientDrawable.RECTANGLE); // RECTANGLE with equal dimensions becomes a square

        // Dynamically ensure the square is applied to the image content
        imageView.post(() -> {
            int size = Math.min(imageView.getWidth(), imageView.getHeight()); // Use the smaller dimension
            borderDrawable.setSize(size, size); // Set square dimensions
            imageView.setBackground(borderDrawable); // Apply the border as the background
            imageView.setPadding(borderWidth, borderWidth, borderWidth, borderWidth); // Apply padding for visibility
        });
    }
    private void startAlternatingCountryColumn(TableRow row, ImageView flagView, TextView abbreviationView) {
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1); // Dummy values to control time
        animator.setDuration(2000); // 2 seconds per toggle
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);

        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            if (value < 0.5) {
                flagView.setVisibility(View.VISIBLE);
                abbreviationView.setVisibility(View.GONE);
            } else {
                flagView.setVisibility(View.GONE);
                abbreviationView.setVisibility(View.VISIBLE);
            }
        });

        animator.start();
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

    private void startFlashingAnimation(String userId, final TableRow row) {
        // Define the flashing colors
        final int colorGreen = ContextCompat.getColor(this, R.color.green);
        final int colorRed = ContextCompat.getColor(this, R.color.red);
        final int colorWhite = ContextCompat.getColor(this, R.color.white);
        final int colorBlue = ContextCompat.getColor(this, R.color.blue);
        // The colorWhite and colorBlue are commented out, add them back if needed.

        final ValueAnimator colorAnimator = ValueAnimator.ofArgb(colorGreen, colorRed, colorWhite, colorBlue);

        colorAnimator.setDuration(4000); // Duration of the animation between two colors
        colorAnimator.setEvaluator(new ArgbEvaluator());
        colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
        colorAnimator.setRepeatCount(ValueAnimator.INFINITE);
        colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                // Change the text color for each TextView cell in the row
                for (int i = 0; i < row.getChildCount(); i++) {
                    View view = row.getChildAt(i);
                    if (view instanceof TextView) {
                        ((TextView) view).setTextColor((int) animator.getAnimatedValue());
                    }
                }
            }
        });

        colorAnimator.start();

        // Save the animator on the map
        flashingAnimators.put(userId, colorAnimator);
    }

}