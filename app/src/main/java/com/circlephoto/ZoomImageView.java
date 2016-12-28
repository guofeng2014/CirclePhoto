package com.circlephoto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * 作者：guofeng
 * 日期:2016/12/16
 */
public class ZoomImageView extends ImageView implements ScaleGestureDetector.OnScaleGestureListener {


    private ScaleGestureDetector mScaleGestureDetector;

    private GestureDetector mGestureDetector;

    private RectF dragMaxMargin = new RectF();

    private RectF mScaleRect = new RectF();

    private Matrix mScaleMatrix = new Matrix();

    private final float[] mMatrixValue = new float[9];

    private static final float SCALE_DEFAULT = 3;

    private float scaleMax;

    protected Bitmap bitmap;

    public ZoomImageView(Context context) {
        this(context, null);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        mGestureDetector = new GestureDetector(context, doubleListener);
        getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        this.bitmap = bm;
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        this.bitmap = BitmapFactory.decodeResource(getResources(), resId);
    }

    /**
     * To monitor imageView  onMeasure complete ,
     * in this callback we can get ImageView real width and real height
     */
    ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void onGlobalLayout() {
            getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
            initRectF();
            imageToCenter();
            onMeasureComplete();
        }
    };

    private void initRectF() {
        float x = getTranslateX();
        float y = getTranslateY();
        try {
            Drawable drawable = getImageDrawable();
            float width = drawable.getIntrinsicWidth() * getScale();
            float height = drawable.getIntrinsicHeight() * getScale();
            mScaleRect.set(x, y, width + x, height + y);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float defaultScale;

    /**
     * init imageView size,fitCenter value
     * translate imageView to the screen center
     */
    private void imageToCenter() {
        Drawable drawable;
        try {
            drawable = getImageDrawable();
            // imageView container real size
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();
            // bitmap origin real size
            int dw = drawable.getIntrinsicWidth();
            int dh = drawable.getIntrinsicHeight();
            //post bitmap to imageView center,
            // default position is left|top
            mScaleMatrix.postTranslate((width - dw) >> 1, (height - dh) >> 1);
            // bitmap is smaller than imageView
            if (width > dw || height > dh) {
                defaultScale = Math.min(width / dw, height / dh);
            } else {
                defaultScale = Math.max(width / dw, height / dh);
            }
            scaleMax = SCALE_DEFAULT * defaultScale;
            //Scale bitmap size until bitmap max size equal imageView max size
            mScaleMatrix.postScale(defaultScale, defaultScale, width >> 1, height >> 1);
            setImageMatrix(mScaleMatrix);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * CallBack for ImageView  Double listener
     */
    private GestureDetector.SimpleOnGestureListener doubleListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            onDoubleTabListener(e);
            return true;
        }
    };

    private boolean isAutoScaling = false;

    private void onDoubleTabListener(MotionEvent e) {
        if (isAutoScaling) return;
        float x = e.getX();
        float y = e.getY();
        isAutoScaling = true;
        //perform scale big event
        if (getScale() < scaleMax) {
            postDelayed(new AutoScaleRunnable(this, mScaleMatrix, x, y, scaleMax, onCompleteListener), 16);
        } else {
            //perform scale small event
            postDelayed(new AutoScaleRunnable(this, mScaleMatrix, x, y, defaultScale, onCompleteListener), 16);
        }
    }

    private AutoScaleRunnable.OnScaleCompleteCallBack onCompleteListener = new AutoScaleRunnable.OnScaleCompleteCallBack() {
        @Override
        public void onCompleteListener() {
            isAutoScaling = false;
        }

    };

    private float lastX;
    private float lastY;
    private int lastTouchPointCount;

    /**
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event)) return true;
        mScaleGestureDetector.onTouchEvent(event);
        //get touch point count
        final int pointCount = event.getPointerCount();
        float x = 0;
        float y = 0;
        for (int i = 0; i < pointCount; i++) {
            x += event.getX(i);
            y += event.getY(i);
        }
        //compute the average of touch point location
        x = x / pointCount;
        y = y / pointCount;
        if (pointCount != lastTouchPointCount) {
            lastX = x;
            lastY = y;
        }
        lastTouchPointCount = pointCount;
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dx = x - lastX;
                float dy = y - lastY;
                if (Math.abs(dx) > 5 && Math.abs(dy) > 5) {
                    if (mScaleRect.left + dx >= dragMaxMargin.left || mScaleRect.right + dx <= dragMaxMargin.right) {
                        dx = 0;
                    }
                    if (mScaleRect.top + dy > dragMaxMargin.top || mScaleRect.bottom + dy <= dragMaxMargin.bottom) {
                        dy = 0;
                    }
                    mScaleMatrix.postTranslate(dx, dy);
                    setImageMatrix(mScaleMatrix);
                    lastY = y;
                    lastX = x;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                lastTouchPointCount = 0;
                break;
        }
        return true;
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        super.setImageMatrix(matrix);
        invalidate();
    }


    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        float scale = getScale();
        float scaleFactor = scaleGestureDetector.getScaleFactor();
        // check whether the bitmap can perform scale event
        if ((scale < scaleMax && scaleFactor > 1) || (scale > defaultScale && scaleFactor < 1)) {
            float tempScale = scaleFactor * scale;
            if (tempScale < defaultScale) {
                scaleFactor = defaultScale / scale;
            }
            if (tempScale > scaleMax) {
                scaleFactor = scaleMax / scale;
            }
            mScaleMatrix.postScale(scaleFactor, scaleFactor,
                    scaleGestureDetector.getFocusX(), scaleGestureDetector.getFocusY());
            scaleWhenCenter();
            setImageMatrix(mScaleMatrix);
        }
        return true;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // each time to scale or translate bitmap ,
        // need to  measure bitmap margin left,top,right,bottom
        float x = (int) getTranslateX();
        float y = (int) getTranslateY();
        try {
            Drawable drawable = getImageDrawable();
            float width = drawable.getIntrinsicWidth() * getScale();
            float height = drawable.getIntrinsicHeight() * getScale();
            mScaleRect.set(x, y, width + x, height + y);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {

    }

    protected float getScale() {
        mScaleMatrix.getValues(mMatrixValue);
        return mMatrixValue[Matrix.MSCALE_X];
    }

    protected float getTranslateX() {
        mScaleMatrix.getValues(mMatrixValue);
        return mMatrixValue[Matrix.MTRANS_X];
    }

    protected float getTranslateY() {
        mScaleMatrix.getValues(mMatrixValue);
        return mMatrixValue[Matrix.MTRANS_Y];
    }

    /**
     * Sub Class or  Other Class  to invoke this method,
     * this method to control Marin left,top,right,bottom when you drag imageView,
     * default margin value is zero
     *
     * @param maxLeft   max margin left
     * @param maxTop    max margin top
     * @param maxRight  max margin right
     * @param maxBottom max margin bottom
     */
    public void setDragMarin(float maxLeft, float maxTop, float maxRight, float maxBottom) {
        dragMaxMargin.set(maxLeft, maxTop, maxRight, maxBottom);
    }

    /**
     * This method is for subClass ,when imageView onMeasure complete,
     * this method will be invoke only once
     */
    public void onMeasureComplete() {

    }

    private RectF centerRect = new RectF();

    protected void scaleWhenCenter() {
        Matrix matrix = mScaleMatrix;
        try {
            Drawable drawable = getImageDrawable();
            centerRect.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            matrix.mapRect(centerRect);

            float dx = 0;
            float dy = 0;

            int width = getMeasuredWidth();
            int height = getMeasuredHeight();

            // bitmap width bigger than imageView width
            if (centerRect.width() >= width) {
                if (centerRect.left > 0) {
                    dx = -centerRect.left;
                }
                if (centerRect.right < width) {
                    dx = width - centerRect.right;
                }
            }
            //bitmap height bigger than imageView height
            if (centerRect.height() >= height) {
                if (centerRect.top > 0) {
                    dy = -centerRect.top;
                }
                if (centerRect.bottom < height) {
                    dy = height - centerRect.bottom;
                }
            }
            // bitmap width  smaller than imageView width
            if (centerRect.width() < width) {
                dx = width * 0.5f - centerRect.right + 0.5f * centerRect.width();
            }
            //bitmap height smaller than imageView width
            if (centerRect.height() < height) {
                dy = height * 0.5f - centerRect.bottom + 0.5f * centerRect.height();
            }
            mScaleMatrix.postTranslate(dx, dy);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    protected Drawable getImageDrawable() throws Exception {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            throw new NullPointerException("drawable can not be null");
        }
        return drawable;
    }

}
