package com.letrans.android.translator.settings.storage;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.letrans.android.translator.R;

public class StorageTableView extends View {
    private int mWidth;
    private int mHeight;

    private CharSequence[] mTopColumns;
    private CharSequence[] mBottomColumns;
    private int mTextColor;
    private int mTextSize;
    private Drawable mDivider;
    private Drawable mTopBackground;
    private Drawable mBottomBackground;

    private float mColumnWidth;
    private float mRowHeight;

    private Paint mPaint;

    public StorageTableView(Context context) {
        this(context, null);
    }

    public StorageTableView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StorageTableView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public StorageTableView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.StorageTableView);
        mTopColumns = typedArray.getTextArray(R.styleable.StorageTableView_topColumns);
        mBottomColumns = typedArray.getTextArray(R.styleable.StorageTableView_bottomColumns);
        if (mTopColumns.length != mBottomColumns.length) {
            throw new IllegalArgumentException("The length of topColumns and bottomColumns must be same");
        }
        mTextColor = typedArray.getColor(R.styleable.StorageTableView_textColor, Color.BLACK);
        mTextSize = typedArray.getDimensionPixelSize(R.styleable.StorageTableView_textSize, 20);
        mDivider = typedArray.getDrawable(R.styleable.StorageTableView_dividerColor);
        mTopBackground = typedArray.getDrawable(R.styleable.StorageTableView_topRowBackground);
        mBottomBackground = typedArray.getDrawable(R.styleable.StorageTableView_bottomRowBackground);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mTopBackground.setBounds(0, 0, (int) mWidth, (int) mRowHeight);
        mTopBackground.draw(canvas);
        mBottomBackground.setBounds(0, (int) mRowHeight, (int) mWidth, (int) mHeight);
        mBottomBackground.draw(canvas);
        mDivider.setBounds(0, (int) mRowHeight, mWidth, (int) mRowHeight + 1);
        mDivider.draw(canvas);
        for (int i = 0; i < mTopColumns.length - 1; i++) {
            mDivider.setBounds((int) (mColumnWidth * (i + 1)),
                    0, (int) (mColumnWidth * (i + 1) + 1),
                    mHeight);
            mDivider.draw(canvas);
        }
        mPaint.setColor(mTextColor);
        mPaint.setTextSize(mTextSize);
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        float textHeight = fontMetrics.descent - fontMetrics.ascent;
        float sub = (mRowHeight - textHeight) / 2;
        // for text center vertical:
        // because: int descentY = mRowHeight - sub = baseY + fontMetrics.descent;
        // so: baseY = mRowHeight - sub - fontMetrics.descent;
        float baseY = mRowHeight - sub - fontMetrics.descent;
        float textWidth = 0;
        for (int i = 0; i < mTopColumns.length; i++) {
            String text = mTopColumns[i].toString();
            textWidth = mPaint.measureText(text);
            canvas.drawText(text, (mColumnWidth - textWidth) / 2 + mColumnWidth * i,
                    baseY, mPaint);
            text = mBottomColumns[i].toString();
            textWidth = mPaint.measureText(text);
            canvas.drawText(text, (mColumnWidth - textWidth) / 2 + mColumnWidth * i,
                    baseY + mRowHeight, mPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        /*int mode = MeasureSpec.getMode(widthMeasureSpec);
        if (mode != MeasureSpec.EXACTLY) {
            throw new IllegalArgumentException("width must have exact size.");
        }
        mode = MeasureSpec.getMode(heightMeasureSpec);
        if (mode != MeasureSpec.EXACTLY) {
            throw new IllegalArgumentException("height must have exact size.");
        }*/
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        mColumnWidth = mWidth * 1.0f / mTopColumns.length;
        mRowHeight = mHeight / 2.0f;
    }

    /**
     *
     * @param columns
     */
    public void setBottomColumns(CharSequence[] columns) {
        mBottomColumns = columns;
        invalidate();
    }
}
