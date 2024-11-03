package com.iniciativaselebi.afrikanaone;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

public class DisintegratingTextview extends androidx.appcompat.widget.AppCompatTextView {

    public DisintegratingTextview(Context context) {
        super(context);
        init();
    }

    public DisintegratingTextview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DisintegratingTextview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        Typeface disintegratingFont = Typeface.createFromAsset(getContext().getAssets(), "fonts/disintegrating_font.ttf");
        setTypeface(disintegratingFont);
    }
}
