package com.letrans.android.translator.settings.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.letrans.android.translator.utils.Utils;

public class TimeoutLevelDrawable extends Drawable {
    private Context mContext;
    private Paint mPaint;

    private int mWidth;
    private int mHeight;
    private int mPadding;

    private int mLevelCount;
    private int mLevelWidth;
    private int mLevelHeight;
    private int mProgressHeight;

    private String[] mEntryValues;

    private int mLeftSpace;
    private int mRightSpace;
    private int mProgressStartOffset;

    private float startTextWidth;
    private float endTextWidth;

    public TimeoutLevelDrawable(Context context, int padding, int progressStartOffset,
                                String[] textArray) {
        mContext = context;
        mPadding = padding;
        mProgressStartOffset = progressStartOffset;
        mEntryValues = textArray;
        mProgressHeight = Utils.dpToPx(2);
        mLevelCount = mEntryValues == null ? 0 : mEntryValues.length;
        mLevelWidth = Utils.dpToPx(2);
        mLevelHeight = Utils.dpToPx(8);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(0xFFC0C0C0);
        mPaint.setTextSize(Utils.dpToPx(16));
        startTextWidth = mPaint.measureText(mEntryValues[0]);
        endTextWidth = mPaint.measureText(mEntryValues[mEntryValues.length - 1]);
        mLeftSpace = (int) (startTextWidth / 2 + 0.5f);
        mLeftSpace = Math.max(mProgressStartOffset, mLeftSpace);
        mRightSpace = (int) (endTextWidth / 2 + 0.5f);
        mRightSpace = Math.max(mProgressStartOffset, mRightSpace);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        int topY = (mHeight - mProgressHeight) >> 1;
        float left = mLeftSpace - mLevelWidth / 2.0f;
        float right = mWidth - (mRightSpace - mLevelWidth / 2.0f);
        canvas.drawRect(left, topY, right, topY + mProgressHeight, mPaint);

        float gap = (right - left) / (mLevelCount - 1.0f);
        topY = (mHeight - mLevelHeight) >> 1;
        canvas.drawRect(left, topY, left + mLevelWidth,
                topY + mLevelHeight, mPaint);
        Paint.FontMetrics f = mPaint.getFontMetrics();
        float textHeight = f.descent - f.ascent;
        // textTop = baseY + f.ascent && textTop = topY + mLevelHeight + 2
        // (Attention the textTop value is where the text will display)
        // So: topY + mLevelHeight + 2 = baseY + f.ascent ==> baseY = (topY + mLevelHeight + 2) - f.ascent
        float baseY = (topY + mLevelHeight + mPadding) - f.ascent;
        canvas.drawText(mEntryValues[0], mLeftSpace - startTextWidth / 2, baseY, mPaint);

        canvas.drawRect(right - mLevelWidth, topY, right,
                topY + mLevelHeight, mPaint);
        canvas.drawText(mEntryValues[mEntryValues.length - 1],
                mWidth - mRightSpace - endTextWidth / 2, baseY, mPaint);
        canvas.save();
        canvas.translate(mLeftSpace, topY);
        baseY = (mLevelHeight + mPadding) - f.ascent;
        String text = null;
        for (int i = 1; i < mLevelCount - 1; i++) {
            canvas.translate(gap, 0);
            canvas.drawRect(0 - (mLevelWidth >> 1), 0,
                    mLevelWidth >> 1, mLevelHeight, mPaint);
            text = mEntryValues[i];
            canvas.drawText(text, 0 - (mPaint.measureText(text) / 2), baseY, mPaint);
        }
        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        int old = mPaint.getAlpha();
        if (alpha != old) {
            mPaint.setAlpha(alpha);
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        mWidth = bounds.width();
        mHeight = Utils.dpToPx(90);//bounds.height();
    }

    public void setPadding(int padding) {
        mPadding = padding;
    }

    public int getLeftSpace() {
        return mLeftSpace;
    }

    public int getRightSpace() {
        return mRightSpace;
    }
}
