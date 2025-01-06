package com.iniciativaselebi.afrikanaone;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

public class Rotate3dAnimation extends Animation {
    private static final int DURATION = 1000; // duration of the animation in milliseconds
    private static final int ROTATE_ANGLE = 360; // angle by which to rotate the image

    public Rotate3dAnimation(int i, int i1, int i2, int i3, int i4, boolean b) {
    }

    public Rotate3dAnimation() {

    }

    public static void start(ImageView imageView) {
        RotateAnimation rotateAnimation = new RotateAnimation(0, ROTATE_ANGLE,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setDuration(DURATION);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        imageView.startAnimation(rotateAnimation);
    }

    public static void stop(ImageView imageView) {
        imageView.clearAnimation();
    }

    public void setDuration(int i) {
    }

    public void setInterpolator(AccelerateDecelerateInterpolator accelerateDecelerateInterpolator) {
    }
}
