package com.aviadmini.nogamenolife.compute;

import android.support.annotation.NonNull;

public abstract class LifeCompute {

    public static final int STATE_DEAD  = 0;
    public static final int STATE_ALIVE = 1;

    protected final int mWidth;
    protected final int mHeight;

    protected Callback mCallback;

    public LifeCompute(final int pWidth, final int pHeight) {

        if (pWidth <= 0 || pHeight <= 0 || pWidth + pHeight < 3) {
            throw new IllegalArgumentException("Both width and height should be >0 and their sum should be >=3");
        }

        this.mWidth = pWidth;
        this.mHeight = pHeight;

    }

    // This allows changing implementation at runtime
    public LifeCompute(@NonNull final LifeCompute pOther) {

        this.mWidth = pOther.getWidth();
        this.mHeight = pOther.getHeight();

    }

    public abstract void tick();

    public void setCallback(final Callback pCallback) {
        this.mCallback = pCallback;
    }

    public void changeCellState(final int pCellX, final int pCellY, final int pNewState) {
        this.changeCellState(pCellX + this.mHeight * pCellY, pNewState);
    }

    public void changeCellState(final int pCellPosition, final int pNewState) {

        this.checkCellCoordinates(pCellPosition);

        this.changeCellStateSafe(pCellPosition, pNewState);

    }

    protected abstract void changeCellStateSafe(final int pCellPosition, final int pNewState);

    public int getCellState(final int pCellX, final int pCellY) {
        return this.getCellState(pCellX + this.mHeight * pCellY);
    }

    public int getCellState(final int pCellPosition) {

        this.checkCellCoordinates(pCellPosition);

        return this.getCellStateSafe(pCellPosition);
    }

    protected abstract int getCellStateSafe(final int pCellPosition);

    protected void checkCellCoordinates(final int pCellPosition) {

        if (pCellPosition < 0 || pCellPosition >= this.mWidth * this.mHeight) {
            throw new IllegalArgumentException("Cell position invalid");
        }

    }

    @NonNull
    public abstract int[] getCellStates();

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public abstract void clear();

    public abstract int getStep();

    public abstract void destroy();

    public interface Callback {

        void onTickComplete();

        void onCellStateSwapped();

    }

}
