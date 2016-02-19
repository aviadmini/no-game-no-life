package com.aviadmini.nogamenolife.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.aviadmini.nogamenolife.R;
import com.aviadmini.nogamenolife.compute.LifeCompute;

import java.util.Locale;

public class LifeDrawView
        extends View {

    private static final int LIFE_SIZE_DEFAULT = 5;

    public static final int CELL_POSITION_INVALID = -1;

    private int mLifeWidth  = LIFE_SIZE_DEFAULT;
    private int mLifeHeight = LIFE_SIZE_DEFAULT;

    private int[] mCellStates;

    private float mOffsetHorizontal;
    private float mOffsetVertical;
    private float mChunk;

    private final Paint mAlivePaint = new Paint();
    private final Paint mDeadPaint  = new Paint();

    private boolean mLifeDimensionsSet;

    private GestureDetector     mGestureDetector;
    private OnCellTouchListener mOnCellTouchListener;

    public LifeDrawView(final Context pContext) {
        super(pContext);

        this.init(pContext, null, 0);

    }

    public LifeDrawView(final Context pContext, final AttributeSet pAttrs) {
        super(pContext, pAttrs);

        this.init(pContext, pAttrs, 0);

    }

    public LifeDrawView(final Context pContext, final AttributeSet pAttrs, final int pDefStyleAttr) {
        super(pContext, pAttrs, pDefStyleAttr);

        this.init(pContext, pAttrs, pDefStyleAttr);

    }

    private void init(final Context pContext, AttributeSet pAttrs, int pDefStyle) {

        if (pAttrs != null) {

            final TypedArray typedArray = pContext.obtainStyledAttributes(pAttrs, R.styleable.LifeDrawView, pDefStyle, 0);

            this.mLifeWidth = typedArray.getInteger(R.styleable.LifeDrawView_lifeWidth, this.mLifeWidth);
            this.mLifeHeight = typedArray.getInteger(R.styleable.LifeDrawView_lifeHeight, this.mLifeHeight);

            typedArray.recycle();

        }

        this.mCellStates = new int[this.mLifeWidth * this.mLifeHeight];

        this.mDeadPaint.setAntiAlias(true);
        this.mDeadPaint.setStyle(Paint.Style.STROKE);
        this.mDeadPaint.setStrokeWidth(1.0f);
        this.mDeadPaint.setColor(Color.BLACK);

        this.mAlivePaint.setAntiAlias(true);
        this.mAlivePaint.setStyle(Paint.Style.FILL);
        this.mAlivePaint.setColor(Color.BLACK);

        this.mLifeDimensionsSet = false;

        final GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDown(final MotionEvent pMotionEvent) {

                if (mLifeDimensionsSet && mOnCellTouchListener != null) {

                    final int cellPosition = getCellPositionFromTouchCoordinates(pMotionEvent.getX(), pMotionEvent.getY());

                    mOnCellTouchListener.onCellTouched(cellPosition);

                }

                return super.onDown(pMotionEvent);
            }

        };
        this.mGestureDetector = new GestureDetector(this.getContext(), gestureListener);

    }

    @Override
    public boolean onTouchEvent(final MotionEvent pMotionEvent) {
        return this.mGestureDetector.onTouchEvent(pMotionEvent);
    }

    @Override
    protected void onMeasure(final int pWidthMeasureSpec, final int pHeightMeasureSpec) {

        final int width = getDefaultSize(this.getSuggestedMinimumWidth(), pWidthMeasureSpec);
        final int height = getDefaultSize(this.getSuggestedMinimumHeight(), pHeightMeasureSpec);
        final int widthWithoutPadding = width - this.getPaddingLeft() - this.getPaddingRight();
        final int heightWithoutPadding = height - this.getPaddingTop() - this.getPaddingBottom();

        final int min = Math.min(widthWithoutPadding, heightWithoutPadding);

        this.setMeasuredDimension(min + this.getPaddingLeft() + this.getPaddingRight(), min + this.getPaddingTop() + this.getPaddingBottom());

    }

    @Override
    protected void onDraw(final Canvas pCanvas) {

        if (!this.mLifeDimensionsSet) {
            this.setLifeDimensionsOnDraw();
        }

        pCanvas.drawColor(Color.LTGRAY);

        boolean alive;
        int chunksLeft;
        int chunksTop;

        for (int i = this.mCellStates.length - 1; i >= 0; i--) {

            alive = this.mCellStates[i] == LifeCompute.STATE_ALIVE;

            chunksLeft = i % this.mLifeWidth;
            chunksTop = i / this.mLifeWidth;

            pCanvas.drawRect(this.mOffsetHorizontal + this.mChunk * chunksLeft,//
                    this.mOffsetVertical + this.mChunk * chunksTop,//
                    this.mOffsetHorizontal + this.mChunk * (chunksLeft + 1),//
                    this.mOffsetVertical + this.mChunk * (chunksTop + 1),//
                    alive ? this.mAlivePaint : this.mDeadPaint);

        }

    }

    public void setLifeDimensions(final int pLifeWidth, final int pLifeHeight) {

        if (pLifeWidth <= 0 || pLifeHeight <= 0) {
            throw new IllegalArgumentException("Both width and height should be >0");
        }

        this.mLifeWidth = pLifeWidth;
        this.mLifeHeight = pLifeHeight;

        this.mLifeDimensionsSet = false;

    }

    private void setLifeDimensionsOnDraw() {

        this.mCellStates = new int[this.mLifeWidth * this.mLifeHeight];

        if (this.mLifeWidth >= this.mLifeHeight) {

            this.mChunk = (float) this.getWidth() / this.mLifeWidth;

            this.mOffsetHorizontal = 0.0f;
            this.mOffsetVertical = (this.getHeight() - this.mChunk * this.mLifeHeight) / 2.0f;

        } else {

            this.mChunk = (float) this.getHeight() / this.mLifeHeight;

            this.mOffsetHorizontal = (this.getWidth() - this.mChunk * this.mLifeWidth) / 2.0f;//!
            this.mOffsetVertical = 0.0f;

        }

        this.mLifeDimensionsSet = true;
    }

    private int getCellPositionFromTouchCoordinates(final float pX, final float pY) {

        if (pX - this.mOffsetHorizontal < 0.0f || pX + this.mOffsetHorizontal > this.getWidth() ||
                pY - this.mOffsetVertical < 0 || pY + this.mOffsetVertical > this.getHeight()) {
            return CELL_POSITION_INVALID;
        } else {

            final int x = (int) (((pX - this.mOffsetHorizontal) / this.mChunk));
            final int y = (int) (((pY - this.mOffsetVertical) / this.mChunk));

            return y * this.mLifeWidth + x;
        }

    }

    public void setCellStates(@NonNull final int[] pCellStates) {

        if (this.mCellStates.length != pCellStates.length) {
            throw new IllegalArgumentException(
                    String.format(Locale.US, "Cell states array size of %s is incorrect. Should be: %s", pCellStates.length,
                            this.mCellStates.length));
        }

        this.mCellStates = pCellStates;

        this.invalidate();

    }

    public int getLifeWidth() {
        return this.mLifeWidth;
    }

    public int getLifeHeight() {
        return this.mLifeHeight;
    }

    public int[] getCellStates() {
        return this.mCellStates;
    }

    public void setOnCellTouchListener(final OnCellTouchListener pOnCellTouchListener) {
        this.mOnCellTouchListener = pOnCellTouchListener;
    }

    public interface OnCellTouchListener {
        void onCellTouched(final int pCellPosition);
    }

}
