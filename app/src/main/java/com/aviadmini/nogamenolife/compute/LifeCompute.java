package com.aviadmini.nogamenolife.compute;

import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class LifeCompute {

    private static final String COMPUTE_THREAD_NAME = "[LifeComputeThread %s]";

    private static final AtomicInteger THREAD_ID = new AtomicInteger();

    public static final int STATE_DEAD  = 0;
    public static final int STATE_ALIVE = 1;

    private final int mWidth;
    private final int mHeight;

    private HandlerThread mComputeHandlerThread;

    private Handler mComputeHandler;

    private final Callback mCallback;

    private int[] mCachedCellStates;

    public LifeCompute(final int pWidth, final int pHeight, @NonNull final Callback pCallback) {

        if (pWidth <= 0 || pHeight <= 0 || pWidth + pHeight < 3) {
            throw new IllegalArgumentException("Both width and height should be >0 and their sum should be >=3");
        }

        this.mCallback = pCallback;

        this.mWidth = pWidth;
        this.mHeight = pHeight;

        this.initThread();

    }

    /**
     * Constructs LifeCompute from another LifeCompute, useful when need to swap implementations. Need to call {@link #create()} afterwards
     *
     * @param pOther old LifeCompute
     */
    public LifeCompute(@NonNull final LifeCompute pOther) {

        this.mWidth = pOther.getWidth();
        this.mHeight = pOther.getHeight();

        this.mCallback = pOther.getCallback();

    }

    /**
     * After changing implementation need to use this to create the internals
     */
    public void create() {
        this.initThread();
    }

    /**
     * Creates HandlerThread which will compute things in background and runs {@link #init()} in the thread
     */
    private void initThread() {

        // create HandlerThread which will be responsible for processing
        this.mComputeHandlerThread = new HandlerThread(String.format(Locale.US, "%s %s", COMPUTE_THREAD_NAME, THREAD_ID.getAndIncrement()));
        this.mComputeHandlerThread.start();
        this.mComputeHandler = new Handler(this.mComputeHandlerThread.getLooper());

        this.mComputeHandler.post(new Runnable() {

            @Override
            public void run() {

                init();

                mCallback.onInit();

            }

        });

    }

    /**
     * All initialization that is not thread safe should occur in this method
     */
    protected abstract void init();

    /**
     * Initiates tick and calls  {@link Callback#onTick()}  when it's complete
     */
    public final void tick() {

        this.mComputeHandler.post(new Runnable() {

            @Override
            public void run() {

                tickInternal();

                mCallback.onTick();

            }

        });

    }

    /**
     * Implement tick logic here
     */
    protected abstract void tickInternal();

    /**
     * Requests to change cell state and calls {@link Callback#onCellStateChanged(int, int)} when it's complete
     *
     * @param pCellX    cell position X
     * @param pCellY    cell position Y
     * @param pNewState state to assign
     */
    public final void requestChangeCellState(final int pCellX, final int pCellY, final int pNewState) {
        this.requestChangeCellState(pCellX + this.mHeight * pCellY, pNewState);
    }

    /**
     * Requests to change cell state and calls {@link Callback#onCellStateChanged(int, int)} when it's complete
     *
     * @param pCellPosition cell position
     * @param pNewState     state to assign
     */
    public final void requestChangeCellState(final int pCellPosition, final int pNewState) {

        this.checkCellCoordinates(pCellPosition);

        this.mComputeHandler.post(new Runnable() {

            @Override
            public void run() {
                mCallback.onCellStateChanged(pCellPosition, changeCellStateInternal(pCellPosition, pNewState));
            }

        });

    }

    /**
     * @param pCellPosition cell position
     * @param pNewState     new state to assign
     * @return state that was assigned
     */
    protected abstract int changeCellStateInternal(final int pCellPosition, final int pNewState);

    /**
     * @param pCellPosition cell position to check
     * @throws IllegalArgumentException if cell position is not on board
     */
    protected final void checkCellCoordinates(final int pCellPosition) {

        if (pCellPosition < 0 || pCellPosition >= this.mWidth * this.mHeight) {
            throw new IllegalArgumentException("Cell position invalid");
        }

    }

    /**
     * Requests to get cell state which is returned in {@link Callback#onCellState(int, int)}
     *
     * @param pCellX cell position X
     * @param pCellY cell position Y
     */
    public final void requestCellState(final int pCellX, final int pCellY) {
        this.requestCellState(pCellX + this.mHeight * pCellY);
    }

    /**
     * Requests to get cell state which is returned in {@link Callback#onCellState(int, int)}
     *
     * @param pCellPosition cell position
     */
    public final void requestCellState(final int pCellPosition) {

        this.checkCellCoordinates(pCellPosition);

        this.mComputeHandler.post(new Runnable() {

            @Override
            public void run() {
                mCallback.onCellState(pCellPosition, getCellStateInternal(pCellPosition));
            }

        });

    }

    /**
     * @param pCellPosition cell position
     * @return cell state
     */
    protected abstract int getCellStateInternal(final int pCellPosition);

    /**
     * Requests to get cell states which are returned in {@link Callback#onCellStates(int[])}
     */
    public final void requestCellStates() {

        this.mComputeHandler.post(new Runnable() {

            @Override
            public void run() {
                mCallback.onCellStates(getCellStates());
            }

        });

    }

    /**
     * @return cell states
     */
    @NonNull
    protected abstract int[] getCellStates();

    /**
     * @return board width
     */
    public final int getWidth() {
        return this.mWidth;
    }

    /**
     * @return board height
     */
    public final int getHeight() {
        return this.mHeight;
    }

    /**
     * Clears board and calls {@link Callback#onCleared()} when done
     */
    public final void clear() {

        this.mComputeHandler.post(new Runnable() {

            @Override
            public void run() {

                clearInternal();

                mCallback.onCleared();

            }

        });

    }

    /**
     * Call that indicates that board should be cleared.
     */
    protected abstract void clearInternal();

    /**
     * @return current Game of Life step
     */
    public abstract int getStep();

    /**
     * Destroys everything and calls {@link Callback#onDestroyed()} when done
     */
    public final void destroy() {

        this.mComputeHandler.post(new Runnable() {

            @Override
            public void run() {

                cacheCellStates(getCellStates());

                destroyInternal();

                mComputeHandlerThread.quit();

                mCallback.onDestroyed();

            }

        });

    }

    /**
     * Call that indicates that this LifeCompute will be not used anymore. Destroy/clear everything that needs to be destroyed/cleared
     */
    protected abstract void destroyInternal();

    /**
     * Caches cell states so that they can be accessed synchronously
     *
     * @param pCellStates cell states to cache
     */
    private void cacheCellStates(@NonNull final int[] pCellStates) {
        this.mCachedCellStates = pCellStates.clone();
    }

    /**
     * @return cached cell states or null if caching was not performed
     */
    @Nullable
    protected final int[] getCachedCellStates() {
        return this.mCachedCellStates;
    }

    /**
     * @return Callback that is set
     */
    public final Callback getCallback() {
        return this.mCallback;
    }

    /**
     * Allows subclasses to post events to compute thread
     *
     * @param pRunnable event to post
     */
    protected void postEvent(@NonNull final Runnable pRunnable) {
        this.mComputeHandler.post(pRunnable);
    }

    /**
     * Callback of LifeCompute - methods are triggered on main events
     */
    public interface Callback {

        void onInit();

        void onTick();

        void onCleared();

        void onCellState(final int pCellPosition, final int pCellState);

        void onCellStates(@NonNull final int[] pCellStates);

        void onDestroyed();

        void onCellStateChanged(final int pCellPosition, final int pNewState);

    }

}