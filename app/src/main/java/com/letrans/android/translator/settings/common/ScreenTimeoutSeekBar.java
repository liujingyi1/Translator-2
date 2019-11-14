package com.letrans.android.translator.settings.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.letrans.android.translator.R;
import com.letrans.android.translator.utils.Utils;

public class ScreenTimeoutSeekBar extends View {
    private int mWidth;
    private int mHeight;

    private int mMaxProgress;
    private int mCurrentProgress;
    private float mPixPerProgress;

    private OnSeekBarChangeListener mOnSeekBarChangeListener;

    private Drawable mDrawable;
    private Rect mDstRect;
    private int mPadding;

    private int mLeftSpace;
    private int mRightSpace;

    public ScreenTimeoutSeekBar(Context context) {
        this(context, null);
    }

    public ScreenTimeoutSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScreenTimeoutSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ScreenTimeoutSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray mTypedArray = context.obtainStyledAttributes(attrs,
                R.styleable.ScreenTimeoutSeekBar);
        mMaxProgress = mTypedArray.getInt(
                R.styleable.ScreenTimeoutSeekBar_max, 4);
        mDrawable = mTypedArray.getDrawable(R.styleable.ScreenTimeoutSeekBar_thumb);
        mCurrentProgress = mTypedArray.getInt(
                R.styleable.ScreenTimeoutSeekBar_progress, 0);
        CharSequence[] textArray = mTypedArray.getTextArray(R.styleable.ScreenTimeoutSeekBar_entries);
        String[] texts = null;
        if (textArray != null) {
            texts = new String[textArray.length];
            for (int i = 0; i < textArray.length; i++) {
                texts[i] = textArray[i].toString();
            }
        }
        mTypedArray.recycle();

        mDstRect = new Rect(0, 0,
                mDrawable.getIntrinsicWidth(), mDrawable.getIntrinsicHeight());
        mDrawable.setBounds(mDstRect);
        mPadding = Utils.dpToPx(46) >> 1;
        if (texts == null) {
            throw new IllegalArgumentException("The entries is null.");
        }
        TimeoutLevelDrawable timeoutLevelDrawable = new TimeoutLevelDrawable(context,
                mPadding, mDrawable.getIntrinsicWidth() >> 1,
                texts);
        mLeftSpace = timeoutLevelDrawable.getLeftSpace();
        mRightSpace = timeoutLevelDrawable.getRightSpace();
        setBackground(timeoutLevelDrawable);
        //setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mWidth == 0 || mHeight == 0) {
            return;
        }
        canvas.save();
        canvas.translate(mCurrentProgress * mPixPerProgress
                        + (mLeftSpace - (mDrawable.getIntrinsicWidth() >> 1)),
                (mHeight - mDrawable.getIntrinsicWidth()) >> 1);
        mDrawable.draw(canvas);
        canvas.restore();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int progress = mCurrentProgress;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener.onStartTrackingTouch(this);
                }
                setPressed(true);
                progress = getProgress(x, y);
                if (progress != mCurrentProgress) {
                    mCurrentProgress = progress;
                    invalidate();
                    if (mOnSeekBarChangeListener != null) {
                        mOnSeekBarChangeListener.onProgressChanged(this, mCurrentProgress);
                    }
                }
                attemptClaimDrag();
                break;
            case MotionEvent.ACTION_MOVE:
                progress = getProgress(x, y);
                if (progress != mCurrentProgress) {
                    mCurrentProgress = progress;
                    invalidate();
                    if (mOnSeekBarChangeListener != null) {
                        mOnSeekBarChangeListener.onProgressChanged(this, mCurrentProgress);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                setPressed(false);
                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener.onStopTrackingTouch(this);
                }
                break;
        }
        return true;
    }

    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mPixPerProgress = (mWidth - mLeftSpace - mRightSpace) / (mMaxProgress);
        mHeight = h;
    }

    private int getProgress(float x, float y) {
        float levelRadius = mPixPerProgress / 2;
        if (x <= levelRadius + mLeftSpace) {
            return 0;
        }
        if (x > mWidth - levelRadius - mRightSpace) {
            return mMaxProgress;
        }
        int progress = mCurrentProgress;
        for (int i = 1; i < mMaxProgress; i++) {
            float px = mPixPerProgress * i + mLeftSpace;
            if (x > px - levelRadius && x <= px + levelRadius) {
                progress = i;
                break;
            }
        }
        return progress;
    }

    public void setMax(int progress) {
        mMaxProgress = progress;
    }

    public void setProgress(int progress) {
        mCurrentProgress = progress;
        invalidate();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        invalidate();
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        mOnSeekBarChangeListener = listener;
    }

    public interface OnSeekBarChangeListener {
        void onStartTrackingTouch(ScreenTimeoutSeekBar seekBar);

        void onProgressChanged(ScreenTimeoutSeekBar seekBar, int progress);

        void onStopTrackingTouch(ScreenTimeoutSeekBar seekBar);
    }
}
