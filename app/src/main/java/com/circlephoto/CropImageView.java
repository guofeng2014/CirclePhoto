package com.circlephoto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;

/**
 * 作者：guofeng
 * 日期:2016/12/16
 */

public class CropImageView extends ZoomImageView {

    private Paint mPaint;
    private int bgColor;
    private float radius;

    public CropImageView(Context context) {
        this(context, null);
    }

    public CropImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CropImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initRes();
        initPaint();
    }

    /**
     * initialization resource information {#like color,and so on}
     */
    private void initRes() {
        bgColor = ContextCompat.getColor(getContext(), R.color.background);
    }

    private void initPaint() {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(2);
        mPaint.setAntiAlias(true);
        mPaint.setColor(bgColor);
    }


    private RectF shadowRect = new RectF();
    private Xfermode xfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
    private boolean isFirst = false;

    @Override
    public void onMeasureComplete() {
        super.onMeasureComplete();
        isFirst = true;
        //refresh view
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        shadowRect.set(0, 0, width, height);
        // center transparent circle
        int sc = canvas.saveLayer(shadowRect, null, Canvas.CLIP_SAVE_FLAG);
        canvas.drawRect(shadowRect, mPaint);
        mPaint.setXfermode(xfermode);
        float cx = width >> 1;
        float cy = height >> 1;
        radius = Math.min((int) cx >> 1, (int) cy >> 1);
        canvas.drawCircle(cx, cy, radius, mPaint);
        canvas.restoreToCount(sc);
        mPaint.setXfermode(null);
        mPaint.setColor(bgColor);
        if (isFirst) {
            setDragMarin(cx - radius, cy - radius, cx + radius, cy + radius);
            isFirst = false;
        }
    }

    public Bitmap cropBitmap() {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Drawable drawable = getDrawable();
        if (drawable == null) return null;
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        float dx = getTranslateX() - (getMeasuredWidth() - w) / 2;
        float dh = getTranslateY() - (getMeasuredHeight() - h) / 2;

        int bW = bitmap.getWidth();
        int bH = bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(getScale(), getScale());
        Bitmap sourceBitmap = Bitmap.createBitmap(bitmap, 0, 0, bW, bH, matrix, true);
        Bitmap targetBitmap = Bitmap.createBitmap((int) radius * 2, (int) radius * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(targetBitmap);
        canvas.drawCircle(radius, radius, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(sourceBitmap, -(w / 2 - radius) + dx, -(h / 2 - radius) + dh, paint);
        return targetBitmap;
    }

}
