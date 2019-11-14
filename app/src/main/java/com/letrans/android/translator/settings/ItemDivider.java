package com.letrans.android.translator.settings;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.letrans.android.translator.R;
import com.letrans.android.translator.utils.Utils;

public class ItemDivider extends RecyclerView.ItemDecoration {
    private Context mContext;
    private Paint mPaint;
    private int mDividerSize;
    private int mOrientation;
    private int mPaddingLeft;

    public ItemDivider(Context context) {
        this(context, 0);
    }

    public ItemDivider(Context context, int paddingLeft) {
        this(context, Utils.dpToPx(1), 0, paddingLeft, LinearLayoutManager.VERTICAL);
    }

    public ItemDivider(Context context, int dividerSize, int color, int paddingLeft,
                       /*@RecyclerView.Orientation*/ int orientation) {
        if (orientation != LinearLayoutManager.HORIZONTAL
                && orientation != LinearLayoutManager.VERTICAL) {
            throw new IllegalArgumentException("orientation is not valid.");
        }
        mContext = context;
        mPaint = new Paint();
        if (color > 0) {
            mPaint.setColor(color);
        } else {
            mPaint.setColor(context.getColor(R.color.settings_divider_color));
        }
        mDividerSize = dividerSize;
        mOrientation = orientation;
        mPaddingLeft = paddingLeft;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (mOrientation == LinearLayoutManager.VERTICAL) {
            drawVertical(c, parent);
        } else {
            drawHorizontal(c, parent);
        }

    }

    private void drawVertical(Canvas c, RecyclerView parent) {
        int left = parent.getPaddingLeft() + mPaddingLeft;
        int right = parent.getWidth() - parent.getPaddingRight();
        int count = parent.getChildCount() - 1;
        for (int i = 0; i < count; i++) {
            View v = parent.getChildAt(i);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) v.getLayoutParams();
            int top = v.getBottom() + params.bottomMargin + Math.round(v.getTranslationY());
            int bottom = top + mDividerSize;
            c.drawRect(new Rect(left, top, right, bottom), mPaint);
        }
    }

    private void drawHorizontal(Canvas c, RecyclerView parent) {
        int top = parent.getPaddingTop();
        int bottom = parent.getHeight() - parent.getPaddingBottom();
        int count = parent.getChildCount() - 1;
        for (int i = 0; i < count; i++) {
            View v = parent.getChildAt(i);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) v.getLayoutParams();
            int left = v.getRight() + params.rightMargin + Math.round(v.getTranslationX());
            int right = left + mDividerSize;
            c.drawRect(new Rect(left, top, right, bottom), mPaint);
        }
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {

    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.set(0,0,0, mDividerSize);
    }
}
