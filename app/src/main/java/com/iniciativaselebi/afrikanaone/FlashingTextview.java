package com.iniciativaselebi.afrikanaone;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

public class FlashingTextview extends AppCompatTextView {

    private static final int DEFAULT_FLASH_INTERVAL = 500; // in milliseconds

    private Handler mHandler = new Handler();
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            // toggle the visibility of the text
            setVisibility(getVisibility() == VISIBLE ? INVISIBLE : VISIBLE);
            // schedule the next flash
            mHandler.postDelayed(mRunnable, mFlashInterval);
        }
    };
    private int mFlashInterval = DEFAULT_FLASH_INTERVAL;

    public FlashingTextview(Context context) {
        super(context);
        init();
    }

    public FlashingTextview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FlashingTextview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // start flashing the text
        mHandler.post(mRunnable);
    }

    public void setFlashInterval(int flashInterval) {
        mFlashInterval = flashInterval;
    }

    public int getFlashInterval() {
        return mFlashInterval;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // stop flashing the text
        mHandler.removeCallbacks(mRunnable);
    }
}
