package com.iniciativaselebi.afrikanaone;

import static com.iniciativaselebi.afrikanaone.ProfileActivity.dpToPx;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import Model.User;
import de.hdodenhof.circleimageview.CircleImageView;
public class LeadersProfileActivity extends AppCompatActivity {

    private CircleImageView profilePic;
    private ImageView imageviewFlag;

    Button buttonatras;
    MediaPlayer swooshPlayer;
    TextView nameTitle, textViewCiudad, textViewPais, textViewRecord, textViewPuntuacionAcumulada,
            textViewPastaAcumulada, textViewAciertosAcumulados, textViewFallosAcumulados, textviewNoRanking;
    private ValueAnimator animator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaders_profile);

        swooshPlayer = MediaPlayer.create(this, R.raw.swoosh);
        profilePic = findViewById(R.id.profilepic);
        imageviewFlag = findViewById(R.id.imageviewFlag);

        String userId = getIntent().getStringExtra("user_id");
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(userId);

        // Fetch user profile picture
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!isFinishing() && !isDestroyed()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null && user.getProfilePicture() != null) {
                        Glide.with(LeadersProfileActivity.this)
                                .load(user.getProfilePicture())
                                .placeholder(R.drawable.baseline_account_circle_24)
                                .into(profilePic);
                    }

                    // Placeholder styling logic
                    String flagUrl = dataSnapshot.child("flagUrl").getValue(String.class);
                    if (flagUrl != null && !flagUrl.isEmpty()) {
                        Picasso.get()
                                .load(flagUrl)
                                .placeholder(R.drawable.other)
                                .error(R.drawable.other)
                                .into(imageviewFlag, new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        addBlackBorderToImageView(imageviewFlag, dpToPx(2, getApplicationContext()));
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        applyPlaceholderStyling(imageviewFlag); // Ensure consistent layout
                                    }
                                });
                    } else {
                        applyPlaceholderStyling(imageviewFlag); // Directly apply placeholder styling
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
                Log.e("DatabaseError", "Failed to fetch user data", databaseError.toException());
            }
        });

        nameTitle = findViewById(R.id.nameTitle);
        textViewCiudad = findViewById(R.id.textViewCiudad);
        textViewPais = findViewById(R.id.textViewPais);
        textViewRecord = findViewById(R.id.textViewRecord);
        textViewPuntuacionAcumulada = findViewById(R.id.textViewPuntuacionAcumulada);
        textViewPastaAcumulada = findViewById(R.id.textViewPastaAcumulada);
        textViewAciertosAcumulados = findViewById(R.id.textViewAciertosAcumulados);
        textViewFallosAcumulados = findViewById(R.id.textViewFallosAcumulados);
        textviewNoRanking = findViewById(R.id.textviewNoRanking);

        // Set up the animated ranking text color
        final int[] colors = {Color.RED, Color.GREEN, Color.BLUE}; // Colors to cycle through
        animator = ValueAnimator.ofInt(colors);
        animator.setDuration(1000); // Set duration for each color change
        animator.setEvaluator(new ArgbEvaluator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animator -> {
            int color = (int) animator.getAnimatedValue();
            textviewNoRanking.setTextColor(color);
        });
        animator.start();

        buttonatras = findViewById(R.id.buttonatras);
        buttonatras.setOnClickListener(v -> {
            playSwoosh();
            finish(); // Return to the previous activity
        });

        // Load user data
        loadUserData(userId);
    }
    private void applyPlaceholderStyling(ImageView imageView) {
        // Match the size used for flags
        int size = dpToPx(45, this);

        // Ensure LayoutParams are consistent with the parent layout (e.g., TableRow or ConstraintLayout)
        if (imageView.getLayoutParams() instanceof TableRow.LayoutParams) {
            TableRow.LayoutParams params = (TableRow.LayoutParams) imageView.getLayoutParams();
            params.width = size;
            params.height = size;
            params.gravity = Gravity.CENTER; // Center alignment
            imageView.setLayoutParams(params);
        } else {
            ViewGroup.LayoutParams params = imageView.getLayoutParams();
            params.width = size;
            params.height = size;
            imageView.setLayoutParams(params);
        }

        // Scale the placeholder properly within the square
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setAdjustViewBounds(true);

        // Apply a black border
        addBlackBorderToImageView(imageView, dpToPx(2, this)); // Border width = 2dp
    }

    private void addBlackBorderToImageView(ImageView imageView, int borderWidth) {
        GradientDrawable borderDrawable = new GradientDrawable();
        borderDrawable.setColor(Color.TRANSPARENT); // Transparent background
        borderDrawable.setStroke(borderWidth, Color.BLACK); // Black border with specified width
        imageView.setBackground(borderDrawable);
        imageView.setPadding(borderWidth, borderWidth, borderWidth, borderWidth); // Add padding for border visibility
    }

    public static int dpToPx(int dp, Context context) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
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

        Glide.with(getApplicationContext()).clear(profilePic); // Cancel Glide requests
        super.onDestroy();
    }

    private void loadUserData(String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(userId);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);

                if (user != null) {
                    // Ensure Ranking displays correctly
                    textviewNoRanking.setText(user.getPositionInLeaderboard() != 0
                            ? String.valueOf(user.getPositionInLeaderboard()).toUpperCase()
                            : "?");

                    // Center align the name title
                    nameTitle.setText(user.getFullname().toUpperCase());
                    nameTitle.setGravity(Gravity.CENTER);


                    // Convert int values to strings and pass them to formatAlignedText
                    textViewCiudad.setText(formatAlignedText("CITY: ", user.getCiudad()));
                    textViewPais.setText(formatAlignedText("COUNTRY: ", user.getPais()));
                    textViewRecord.setText(formatAlignedText("RECORD: ", String.valueOf(user.getHighestScore()) + " POINTS"));
                    textViewPuntuacionAcumulada.setText(formatAlignedText("TOTAL SCORE: ", String.valueOf(user.getAccumulatedPuntuacion()) + " POINTS"));
                    textViewPastaAcumulada.setText(formatAlignedText("TOTAL CASH: ", String.valueOf(user.getAccumulatedPuntuacion()) + " AFROS"));
                    textViewAciertosAcumulados.setText(formatAlignedText("TOTAL CORRECT ANSWERS: ", String.valueOf(user.getAccumulatedAciertos())));
                    textViewFallosAcumulados.setText(formatAlignedText("TOTAL INCORRECT ANSWERS: ", String.valueOf(user.getAccumulatedFallos())));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle any potential errors here
            }
        });
    }

    private SpannableString formatAlignedText(String label, String value) {
        label = label.toUpperCase();
        value = value.toUpperCase();

        SpannableString spannable = new SpannableString(label + value);

        spannable.setSpan(new ForegroundColorSpan(Color.BLACK), 0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(Color.BLACK), label.length(), spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannable;
    }
}