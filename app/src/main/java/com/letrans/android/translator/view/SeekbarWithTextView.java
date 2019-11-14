package com.letrans.android.translator.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.letrans.android.translator.R;

import java.util.List;

public class SeekbarWithTextView extends View {
    private static final float SEEK_BG_SCALE = 0.75F / 2;
    private static final float SEEK_TEXT_SCALE = 2.5F / 3.5F;
    private static final int DEF_HEIGHT = 80;
    private static final int DEF_PADDING = 10;
    private static final int BG_HEIGHT = 5;
    private static final int SEEK_STROKE_SIZE = 1;

    private int viewWidth;
    private int viewHeight;
    private int seekBgColor;
    private int seekPbColor;
    private int seekBallSolidColor;
    private int seekBallStrokeColor;
    private int seekTextColor;
    private int seekTextSize;

    private Paint seekBgPaint;
    private Paint seekBallEndPaint;
    private Paint seekBallStrokePaint;
    private Paint seekPbPaint;
    private Paint seekTextPaint;
    private RectF seekBGRectF;
    private RectF seekPbRectF;

    private int seekBallRadio = 10;
    private int seekBallY;
    private int mSeekBallX;

    private List<String> data;
    private int seekTextY;
    private OnDragFinishedListener dragFinishedListener;
    private int mPosition = 1;
    private int downX;
    private int mBarLeft;
    private int count;

    public SeekbarWithTextView(Context context) {
        this(context, null);
    }

    public SeekbarWithTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeekbarWithTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
//        setBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SeekbarWithTextView, defStyleAttr, R.style.def_seebarwithtext_style);
        int indexCount = typedArray.getIndexCount();
        for (int i = 0; i < indexCount; i++) {
            int attr = typedArray.getIndex(i);
            switch (attr) {
                case R.styleable.SeekbarWithTextView_seek_bg_color:
                    seekBgColor = typedArray.getColor(attr, Color.BLACK);
                    break;
                case R.styleable.SeekbarWithTextView_seek_pb_color:
                    seekPbColor = typedArray.getColor(attr, Color.BLACK);
                    break;
                case R.styleable.SeekbarWithTextView_seek_ball_solid_color:
                    seekBallSolidColor = typedArray.getColor(attr, Color.BLACK);
                    break;
                case R.styleable.SeekbarWithTextView_seek_ball_stroke_color:
                    seekBallStrokeColor = typedArray.getColor(attr, Color.BLACK);
                    break;
                case R.styleable.SeekbarWithTextView_seek_text_color:
                    seekTextColor = typedArray.getColor(attr, Color.BLACK);
                    break;
                case R.styleable.SeekbarWithTextView_seek_text_size:
                    seekTextSize = typedArray.getDimensionPixelSize(attr, 0);
                    break;
            }
        }
        typedArray.recycle();
        init();
    }

    private void init() {
        seekTextPaint = creatPaint(seekTextColor, seekTextSize, Paint.Style.FILL, 0);
        seekBgPaint = creatPaint(seekBgColor, 0, Paint.Style.FILL, 0);
        seekPbPaint = creatPaint(seekPbColor, 0, Paint.Style.FILL, 0);
        seekBallEndPaint = creatPaint(seekPbColor, 0, Paint.Style.FILL, 0);
        seekBallStrokePaint = creatPaint(seekBallStrokeColor, 0, Paint.Style.FILL, 0);
        seekBallStrokePaint.setShadowLayer(5, 2, 2, seekBallStrokeColor);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewHeight = h;
        viewWidth = w;

        seekBallY = (int) (viewHeight * SEEK_BG_SCALE + BG_HEIGHT / 2.F);
        seekTextY = (int) (viewHeight * SEEK_TEXT_SCALE);
//        mSeekBallX = viewWidth - seekBallRadio - DEF_PADDING;

        mBarLeft = DEF_PADDING + seekBallRadio;
        seekBGRectF = new RectF(mBarLeft, viewHeight * SEEK_BG_SCALE, viewWidth - seekBallRadio - DEF_PADDING, viewHeight * SEEK_BG_SCALE + BG_HEIGHT);
        seekPbRectF = new RectF(mBarLeft, viewHeight * SEEK_BG_SCALE, mSeekBallX, viewHeight * SEEK_BG_SCALE + BG_HEIGHT);

        mSeekBallX = getCurrentSeekX(mPosition);
        seekPbRectF.right = mSeekBallX;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int measureHeight;
        if (heightMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.UNSPECIFIED) {
            measureHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEF_HEIGHT, getContext().getResources().getDisplayMetrics());
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(measureHeight, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTexts(canvas);
        drawSeekBG(canvas);
        drawSeekPB(canvas);
        drawCircle(canvas);
    }

    private void drawTexts(Canvas canvas) {
        if (null == data) return;
        int size = data.size();
        int unitWidth = getUnitWidth(size - 1);
        for (int i = 0; i < size; i++) {
            String tempDesc = data.get(i);
            float measureTextWidth = seekTextPaint.measureText(tempDesc);

            //Log.d("guocl", "tempDesc = " + tempDesc +", measureTextWidth="+measureTextWidth);
            canvas.drawText(tempDesc, DEF_PADDING + seekBallRadio + unitWidth * i - measureTextWidth / 2, seekTextY, seekTextPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = (int) event.getX();
                mSeekBallX = downX;
                seekPbRectF.right = mSeekBallX;
                break;
            case MotionEvent.ACTION_MOVE:
                int moveX = (int) event.getX();
                mSeekBallX = moveX;
                seekPbRectF.right = mSeekBallX;
                break;
            case MotionEvent.ACTION_UP:
                int upX = (int) event.getX();
                mPosition = getDataPosition(upX);
                mSeekBallX = getCurrentSeekX(mPosition);

                if (null != dragFinishedListener) {
                    dragFinishedListener.dragFinished(mPosition);
                }
                break;
        }
        if (mSeekBallX < seekBallRadio + DEF_PADDING) {
            mSeekBallX = seekBallRadio + DEF_PADDING;
        }
        if (mSeekBallX > viewWidth - seekBallRadio - DEF_PADDING) {
            mSeekBallX = viewWidth - seekBallRadio - DEF_PADDING;
        }
        seekPbRectF.right = mSeekBallX;
        invalidate();
        return true;
    }

    private void drawSeekPB(Canvas canvas) {
        canvas.drawRect(seekPbRectF, seekPbPaint);
    }

    private void drawCircle(Canvas canvas) {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        canvas.drawCircle(mSeekBallX, seekBallY, seekBallRadio, seekBallStrokePaint);
        canvas.drawCircle(mSeekBallX, seekBallY, seekBallRadio - SEEK_STROKE_SIZE, seekBallEndPaint);
    }

    private void drawSeekBG(Canvas canvas) {
        canvas.drawRect(seekBGRectF, seekBgPaint);
    }


    private Paint creatPaint(int paintColor, int textSize, Paint.Style style, int lineWidth) {
        Paint paint = new Paint();
        paint.setColor(paintColor);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(lineWidth);
        paint.setDither(true);
        paint.setTextSize(textSize);
        paint.setStyle(style);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        return paint;
    }

    private int getUnitWidth(int count) {
        return (viewWidth - 2 * DEF_PADDING - 2 * seekBallRadio) / count;
    }

    private int getCurrentSeekX(int position) {
        if (null == data) {
            return 0;
        }

        int unitWidth = getUnitWidth(data.size() - 1);
        return unitWidth * position + DEF_PADDING + seekBallRadio;
    }

    private int getDataPosition(int upX) {

        int p = 0;
        if (null == data || count < 2) {
            return p;
        }

        int unitWidth = getUnitWidth(count - 1);

        int halfWidth = unitWidth / 2;
        //拿到从起点开始的长度
        int length = (upX - DEF_PADDING - seekBallRadio);
        if (length < 0 || length <= halfWidth) {

        } else {
            p = (length - halfWidth) / unitWidth + 1;
        }
        if (p < 0) p = 0;
        if (p >= data.size()) p = data.size() - 1;
        return p;
    }

    public void setPosition(int position) {
        if (position < data.size()) {
            mPosition = position;
        }
    }

    public void setData(List<String> data, OnDragFinishedListener dragFinishedListener) {
        this.dragFinishedListener = dragFinishedListener;
        this.data = data;
        if (null != data && !data.isEmpty()) {
            count = data.size();
            mPosition = data.size() - 1;
        }
    }

    public interface OnDragFinishedListener {
        void dragFinished(int postion);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        invalidate();
    }
}
