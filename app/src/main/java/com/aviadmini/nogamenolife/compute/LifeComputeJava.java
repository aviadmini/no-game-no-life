package com.aviadmini.nogamenolife.compute;

import android.support.annotation.NonNull;

public class LifeComputeJava
        extends LifeCompute {

    protected final int mCellStatesSize;

    protected final int[] mCellStatesPing;
    protected final int[] mCellStatesPong;

    private LifeCompute mOtherCompute;

    protected boolean mUsingPing;

    private int mStep = 0;

    public LifeComputeJava(final int pWidth, final int pHeight, final Callback pCallback) {
        super(pWidth, pHeight, pCallback);

        this.mCellStatesSize = pWidth * pHeight;

        this.mCellStatesPing = new int[this.mCellStatesSize];
        this.mCellStatesPong = new int[this.mCellStatesSize];

        this.clear();

        this.mUsingPing = true;

    }

    public LifeComputeJava(@NonNull final LifeCompute pOther) {
        super(pOther);

        this.mOtherCompute = pOther;

        this.mCellStatesSize = pOther.getWidth() * pOther.getHeight();

        this.mCellStatesPing = new int[this.mCellStatesSize];
        this.mCellStatesPong = new int[this.mCellStatesSize];

        this.mUsingPing = true;

    }

    @Override
    protected void init() {

        if (this.mOtherCompute != null) {

            final int[] cellStates = this.mOtherCompute.getCachedCellStates();

            if (cellStates != null) {
                System.arraycopy(cellStates, 0, this.mCellStatesPing, 0, this.mCellStatesSize);
            }

            this.mOtherCompute = null;

        }

    }

    @Override
    protected synchronized void tickInternal() {

        // Definitely not the most efficient algorithm ever

        this.mStep++;

        final int[] current = this.mUsingPing ? this.mCellStatesPing : this.mCellStatesPong;
        final int[] next = this.mUsingPing ? this.mCellStatesPong : this.mCellStatesPing;

        for (int i = this.mCellStatesSize - 1; i >= 0; i--) {

            int aliveCellsNearby = 0;

            aliveCellsNearby += this.getAddFromNeighbour(i + 1);
            aliveCellsNearby += this.getAddFromNeighbour(i - 1);

            aliveCellsNearby += this.getAddFromNeighbour(i + this.getWidth());
            aliveCellsNearby += this.getAddFromNeighbour(i - this.getWidth());

            aliveCellsNearby += this.getAddFromNeighbour(i - this.getWidth() + 1);
            aliveCellsNearby += this.getAddFromNeighbour(i - this.getWidth() - 1);

            aliveCellsNearby += this.getAddFromNeighbour(i + this.getWidth() + 1);
            aliveCellsNearby += this.getAddFromNeighbour(i + this.getWidth() - 1);

            if (current[i] == STATE_ALIVE) {

                if (aliveCellsNearby == 2 || aliveCellsNearby == 3) {
                    next[i] = STATE_ALIVE;
                } else {
                    next[i] = STATE_DEAD;
                }

            } else if (current[i] == STATE_DEAD && aliveCellsNearby == 3) {
                next[i] = STATE_ALIVE;
            } else {
                next[i] = STATE_DEAD;
            }

        }

        this.mUsingPing = !this.mUsingPing;

    }

    private int getAddFromNeighbour(final int pNew) {

        int index;

        if (pNew < 0) {
            index = pNew + this.mCellStatesSize;
        } else if (pNew > this.mCellStatesSize - 1) {
            index = pNew - this.mCellStatesSize;
        } else {
            index = pNew;
        }

        return this.getCellStates()[index] == STATE_ALIVE ? 1 : 0;
    }

    @Override
    protected synchronized int changeCellStateInternal(final int pCellPosition, final int pNewState) {

        if (this.mUsingPing) {
            this.mCellStatesPing[pCellPosition] = pNewState;
        } else {
            this.mCellStatesPong[pCellPosition] = pNewState;
        }

        return pNewState;
    }

    @Override
    protected synchronized int getCellStateInternal(final int pCellPosition) {
        return this.mUsingPing ? this.mCellStatesPing[pCellPosition] : this.mCellStatesPong[pCellPosition];
    }

    @NonNull
    @Override
    protected synchronized int[] getCellStates() {
        return this.mUsingPing ? this.mCellStatesPing : this.mCellStatesPong;
    }

    @Override
    public synchronized int getStep() {
        return this.mStep;
    }

    @Override
    protected synchronized void destroyInternal() {
    }

    @Override
    protected synchronized void clearInternal() {

        this.mStep = 0;

        for (int i = this.mCellStatesSize - 1; i >= 0; i--) {

            this.mCellStatesPing[i] = STATE_DEAD;
            this.mCellStatesPong[i] = STATE_DEAD;

        }

    }

}