package com.aviadmini.nogamenolife;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aviadmini.nogamenolife.compute.LifeCompute;
import com.aviadmini.nogamenolife.compute.LifeComputeJava;
import com.aviadmini.nogamenolife.views.LifeDrawView;

import java.util.Locale;

public class MainActivity
        extends AppCompatActivity {

    private static final String COMPUTE_THREAD_NAME = "[LifeComputeThread]";

    private LifeCompute mLifeCompute;

    private LifeDrawView mLifeDrawView;

    private final HandlerThread mComputeHandlerThread = new HandlerThread(COMPUTE_THREAD_NAME);

    private Handler mComputeHandler;

    private LifeCompute.Callback mCallback = new LifeCompute.Callback() {

        @Override
        public void onTickComplete() {

            runOnUiThread(mDrawRunnable);

            if (mSimulating) {
                postTick(mTickDelay);
            }

        }

        @Override
        public void onCellStateSwapped() {
            //TODO
        }

    };

    private final Runnable mTickRunnable = new Runnable() {

        @Override
        public void run() {
            mLifeCompute.tick();
        }

    };

    private final Runnable mDrawRunnable = new Runnable() {

        @Override
        public void run() {

            mLifeDrawView.setCellStates(mLifeCompute.getCellStates());

            setStep();

        }

    };

    private long mTickDelay = 1000L;

    private boolean mSimulating;

    private Button   mStartStopButton;
    private TextView mStepTextView;
    private TextView mDelayTextView;

    @Override
    protected void onCreate(final Bundle pSavedInstanceState) {
        super.onCreate(pSavedInstanceState);

        // views

        this.setContentView(R.layout.activity_main);

        this.mStartStopButton = (Button) this.findViewById(R.id.activity_main_btn_start_stop);

        this.mStepTextView = (TextView) this.findViewById(R.id.activity_main_tv_step);

        this.mLifeDrawView = (LifeDrawView) this.findViewById(R.id.activity_main_life);

        this.mDelayTextView = (TextView) this.findViewById(R.id.activity_main_tv_delay);

        // draw view touches

        this.mLifeDrawView.setOnCellTouchListener(new LifeDrawView.OnCellTouchListener() {

            @Override
            public void onCellTouched(final int pCellPosition) {

                if (pCellPosition != LifeDrawView.CELL_POSITION_INVALID) {
                    requestChangeCellState(pCellPosition);
                }

                if (mSimulating) {
                    Toast.makeText(MainActivity.this, R.string.toast_simulation_online_cell_change, Toast.LENGTH_SHORT)
                         .show();
                }

            }

        });

        // create compute

        this.mLifeCompute = new LifeComputeJava(this.mLifeDrawView.getLifeWidth(), this.mLifeDrawView.getLifeHeight());
        this.mLifeCompute.setCallback(this.mCallback);

        final SeekBar speedSeekBar = (SeekBar) this.findViewById(R.id.activity_main_seek_speed);
        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(final SeekBar pSeekBar, final int pProgress, final boolean pFromUser) {
                setTickDelay(pProgress);
            }

            @Override
            public void onStartTrackingTouch(final SeekBar pSeekBar) {
            }

            @Override
            public void onStopTrackingTouch(final SeekBar pSeekBar) {
            }

        });

        // initialize step # & tick delay
        this.setStep();
        speedSeekBar.setProgress(480);

        this.mSimulating = false;

        // create HandlerThread which will be responsible for processing
        this.mComputeHandlerThread.start();
        this.mComputeHandler = new Handler(this.mComputeHandlerThread.getLooper());

    }

    @Override
    protected void onPause() {

        this.stopSimulation();

        super.onPause();
    }

    private void setTickDelay(final int pProgress) {

        this.mTickDelay = pProgress + 20;

        this.mDelayTextView.setText(String.format(Locale.US, "%s: %sms", this.getString(R.string.activity_main_tv_delay_text), pProgress + 20));

        if (this.mSimulating) {
            this.postTick(this.mTickDelay);
        }

    }

    @Override
    protected void onDestroy() {

        this.mComputeHandlerThread.quit();

        super.onDestroy();
    }

    private void requestChangeCellState(final int pCellPosition) {

        this.mComputeHandler.post(new Runnable() {

            @Override
            public void run() {

                mLifeCompute.changeCellState(pCellPosition,
                        mLifeCompute.getCellState(pCellPosition) == LifeCompute.STATE_ALIVE ? LifeCompute.STATE_DEAD : LifeCompute.STATE_ALIVE);

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        mLifeDrawView.setCellStates(mLifeCompute.getCellStates());
                    }

                });

            }

        });

    }

    public void btnClick(final View pView) {

        switch (pView.getId()) {

            case R.id.activity_main_btn_clear: {

                this.stopSimulation();

                this.mLifeCompute.clear();
                this.mLifeDrawView.setCellStates(this.mLifeCompute.getCellStates());

                this.setStep();

                break;
            }

            case R.id.activity_main_btn_next: {

                this.mComputeHandler.post(this.mTickRunnable);

                break;
            }

            case R.id.activity_main_btn_start_stop: {

                if (this.mSimulating) {
                    this.stopSimulation();
                } else {
                    this.startSimulation();
                }

                break;
            }


        }

    }

    private void setStep() {
        this.mStepTextView.setText(
                String.format(Locale.US, "%s: %s", this.getString(R.string.activity_main_tv_step_text), this.mLifeCompute.getStep()));
    }

    private void postTick() {

        this.mComputeHandler.removeCallbacks(this.mTickRunnable);
        this.mComputeHandler.post(this.mTickRunnable);

    }

    private void postTick(final long pDelay) {

        this.mComputeHandler.removeCallbacks(this.mTickRunnable);
        this.mComputeHandler.postDelayed(this.mTickRunnable, pDelay);

    }

    private void startSimulation() {

        this.postTick();

        this.mSimulating = true;

        this.mStartStopButton.setText(R.string.activity_main_btn_stop_text);

    }

    private void stopSimulation() {

        this.mComputeHandler.removeCallbacks(this.mTickRunnable);

        this.mSimulating = false;

        this.mStartStopButton.setText(R.string.activity_main_btn_start_text);

    }


}