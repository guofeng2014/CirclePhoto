package com.circlephoto;

import android.graphics.Matrix;

/**
 * 作者：guofeng
 * 日期:2016/12/17
 */

public class AutoScaleRunnable implements Runnable {

    private static final float BIGGER = 1.07f;
    private static final float SMALLER = 0.93f;
    private float targetScale;
    private float curScale;

    private float centerX;
    private float centerY;

    private Matrix scaleMatrix;
    private ZoomImageView zoomImageView;
    private OnScaleCompleteCallBack onScaleCompleteCallBack;

    public AutoScaleRunnable(ZoomImageView zoomImageView, Matrix scaleMatrix,
                             float centerX, float centerY, float targetScale, OnScaleCompleteCallBack onScaleCompleteCallBack) {
        this.zoomImageView = zoomImageView;
        this.scaleMatrix = scaleMatrix;
        this.centerX = centerX;
        this.centerY = centerY;
        this.targetScale = targetScale;
        this.onScaleCompleteCallBack = onScaleCompleteCallBack;
        init();
    }

    private static final String TAG = "AutoScaleRunnable";

    /**
     * compute the scale number for auto scale
     */
    private void init() {
        if (zoomImageView.getScale() < targetScale) {
            curScale = BIGGER;
        } else {
            curScale = SMALLER;
        }
    }

    @Override
    public void run() {
        //perform scale
        scaleMatrix.postScale(curScale, curScale, centerX, centerY);
        zoomImageView.scaleWhenCenter();
        zoomImageView.setImageMatrix(scaleMatrix);
        float scale = zoomImageView.getScale();
        //circle to invoke self
        if (curScale == BIGGER && scale < targetScale || curScale == SMALLER && scale > targetScale) {
            zoomImageView.postDelayed(this, 16);
        } else {
            float resetScale = targetScale / scale;
            scaleMatrix.postScale(resetScale, resetScale, centerX, centerY);
            zoomImageView.scaleWhenCenter();
            zoomImageView.setImageMatrix(scaleMatrix);
            if (onScaleCompleteCallBack != null) {
                onScaleCompleteCallBack.onCompleteListener();
            }
        }
    }


    public interface OnScaleCompleteCallBack {
        void onCompleteListener();
    }
}
