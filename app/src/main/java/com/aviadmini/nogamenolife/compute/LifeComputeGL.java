package com.aviadmini.nogamenolife.compute;

import android.support.annotation.NonNull;

import com.aviadmini.nogamenolife.opengl.LifeRenderer;
import com.aviadmini.nogamenolife.opengl.OffscreenGLContextManager;

public class LifeComputeGL
        extends LifeCompute {

    private LifeRenderer mRenderer;

    private OffscreenGLContextManager mOffscreen;

    private int mStep = 0;

    private LifeCompute mOtherCompute;

    @SuppressWarnings("FieldCanBeLocal")
    private final LifeRenderer.Callback mRendererCallback = new LifeRenderer.Callback() {

        @Override
        public void onFrameDrawn() {
            mStep++;
        }

    };

    public LifeComputeGL(final int pWidth, final int pHeight, @NonNull final Callback pCallback) {
        super(pWidth, pHeight, pCallback);
    }

    public LifeComputeGL(@NonNull final LifeCompute pOther) {
        super(pOther);

        this.mOtherCompute = pOther;

    }

    @Override
    protected void init() {

        if (this.mOtherCompute != null) {

            final int[] cellStates = this.mOtherCompute.getCachedCellStates();

            //Timber.v("cell states for init: %s", Arrays.toString(cellStates));

            if (cellStates != null) {
                this.mRenderer = new LifeRenderer(this.mRendererCallback, cellStates);
            } else {
                this.mRenderer = new LifeRenderer(this.mRendererCallback);
            }

            this.mOtherCompute = null;

        }

        this.mOffscreen = new OffscreenGLContextManager(this.mRenderer, this.getWidth(), this.getHeight());

        this.mOffscreen.create();

    }

    @Override
    protected synchronized void tickInternal() {
        this.mOffscreen.render();
    }

    @Override
    protected synchronized int changeCellStateInternal(final int pCellPosition, final int pNewState) {

        this.mRenderer.changeCellState(pCellPosition, pNewState);

        return pNewState;
    }

    @Override
    protected synchronized int getCellStateInternal(final int pCellPosition) {
        return this.mRenderer.getCellState(pCellPosition);
    }

    @NonNull
    @Override
    protected synchronized int[] getCellStates() {
        return this.mRenderer.getCellStates();
    }

    @Override
    protected synchronized void clearInternal() {

        this.mStep = 0;

        this.mRenderer.clear();

    }

    @Override
    public synchronized int getStep() {
        return this.mStep;
    }

    @Override
    protected synchronized void destroyInternal() {
        this.mOffscreen.destroy();
    }

}
