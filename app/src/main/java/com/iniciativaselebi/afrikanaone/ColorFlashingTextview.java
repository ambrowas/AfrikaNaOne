package com.iniciativaselebi.afrikanaone;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import java.util.ArrayList;

public class ColorFlashingTextview extends AppCompatTextView {

    private static final int DEFAULT_FLASH_INTERVAL = 500; // in milliseconds
    private Integer[] mColors = {Color.RED, Color.GREEN, Color.BLUE, Color.WHITE};

    private Handler mHandler = new Handler();
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            // Change the text color based on the current color index
            SpannableString spannableString = new SpannableString(getText());
            for (String substring : mFlashingSubstrings) {
                int start = getText().toString().indexOf(substring);
                if (start >= 0) {
                    int end = start + substring.length();
                    spannableString.setSpan(new ForegroundColorSpan(mColors[mColorIndex]), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            setText(spannableString);
            // Increment the color index
            mColorIndex = (mColorIndex + 1) % mColors.length;
            // Schedule the next color change
            mHandler.postDelayed(mRunnable, mFlashInterval);
        }
    };
    private int mFlashInterval = DEFAULT_FLASH_INTERVAL;
    private ArrayList<String> mFlashingSubstrings = new ArrayList<>();
    private int mColorIndex;

    public ColorFlashingTextview(Context context) {
        super(context);
        init();
    }

    public ColorFlashingTextview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorFlashingTextview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mColorIndex = 0;
        // start flashing the text color
        mHandler.post(mRunnable);
    }

    public void setFlashInterval(int flashInterval) {
        mFlashInterval = flashInterval;
    }

    public int getFlashInterval() {
        return mFlashInterval;
    }

    public void setFlashingSubstrings(ArrayList<String> flashingSubstrings) {
        mFlashingSubstrings = flashingSubstrings;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // stop flashing the text color
        mHandler.removeCallbacks(mRunnable);
    }
}