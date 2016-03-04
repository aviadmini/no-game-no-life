package com.aviadmini.nogamenolife;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aviadmini.nogamenolife.compute.LifeCompute;
import com.aviadmini.nogamenolife.compute.LifeComputeGL;
import com.aviadmini.nogamenolife.compute.LifeComputeJava;
import com.aviadmini.nogamenolife.views.LifeDrawView;

import java.util.Locale;

public class MainActivity
        extends AppCompatActivity {

    private static final int COMPUTE_NONE = 0;
    private static final int COMPUTE_JAVA = 1;
    private static final int COMPUTE_GL   = 2;

    private LifeCompute mLifeCompute;
    private LifeCompute mSwapLifeCompute;

    private LifeDrawView mLifeDrawView;

    private final Handler mTickHandler = new Handler();

    private int mComputeImplForSwap;

    private LifeCompute.Callback mCallback = new LifeCompute.Callback() {

        @Override
        public void onInit() {

            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    // initialize step # & tick delay
                    setStep();

                    mSimulating = false;

                }

            });

        }

        @Override
        public void onTick() {
            mLifeCompute.requestCellStates();
        }

        @Override
        public void onCleared() {
            mLifeCompute.requestCellStates();
        }

        @Override
        public void onCellState(final int pCellPosition, final int pCellState) {
            mLifeCompute.requestChangeCellState(pCellPosition,
                    pCellState == LifeCompute.STATE_ALIVE ? LifeCompute.STATE_DEAD : LifeCompute.STATE_ALIVE);
        }

        @Override
        public void onCellStates(@NonNull final int[] pCellStates) {

            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    mLifeDrawView.setCellStates(pCellStates);

                    setStep();

                }

            });

            if (mSimulating) {
                postTick(mTickDelay);
            }

        }

        @Override
        public void onDestroyed() {

            if (mSwapLifeCompute != null) {

                mSwapLifeCompute.create();

                switch (mComputeImplForSwap) {

                    case COMPUTE_JAVA: {

                        mCurrentImplName = "Java";

                        break;
                    }

                    case COMPUTE_GL: {

                        mCurrentImplName = "OpenGL";

                        break;
                    }

                    default: {

                        mCurrentImplName = null;

                        break;
                    }
                }

                if (mCurrentImplName != null) {

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    String.format(Locale.US, "%s to: %s", getResources().getString(R.string.toast_implementation_changed),
                                            mCurrentImplName), Toast.LENGTH_SHORT)
                                 .show();
                        }

                    });

                }

            }

            if (mSwapLifeCompute != null) {

                mLifeCompute = mSwapLifeCompute;
                mSwapLifeCompute = null;

                mLifeCompute.requestCellStates();

            }

        }

        @Override
        public void onCellStateChanged(final int pCellPosition, final int pNewState) {
            mLifeCompute.requestCellStates();
        }

    };

    private final Runnable mTickRunnable = new Runnable() {

        @Override
        public void run() {
            mLifeCompute.tick();
        }

    };

    private long mTickDelay = 1000L;

    private boolean mSimulating = false;

    private Button   mStartStopButton;
    private TextView mStepTextView;
    private TextView mDelayTextView;

    private String mCurrentImplName;

    @Override
    protected void onCreate(final Bundle pSavedInstanceState) {
        super.onCreate(pSavedInstanceState);

        // views

        this.setContentView(R.layout.activity_main);

        this.mStartStopButton = (Button) this.findViewById(R.id.activity_main_btn_start_stop);

        this.mStepTextView = (TextView) this.findViewById(R.id.activity_main_tv_step);

        this.mLifeDrawView = (LifeDrawView) this.findViewById(R.id.activity_main_life);

        this.mDelayTextView = (TextView) this.findViewById(R.id.activity_main_tv_delay);

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

        // draw view touches

        this.mLifeDrawView.setOnCellTouchListener(new LifeDrawView.OnCellTouchListener() {

            @Override
            public void onCellTouched(final int pCellPosition) {

                if (pCellPosition != LifeDrawView.CELL_POSITION_INVALID) {
                    mLifeCompute.requestCellState(pCellPosition);
                }

                if (mSimulating) {
                    Toast.makeText(MainActivity.this, R.string.toast_simulation_online_cell_change, Toast.LENGTH_SHORT)
                         .show();
                }

            }

        });

        // start initializing LifeCompute

        this.mLifeCompute = new LifeComputeJava(this.mLifeDrawView.getLifeWidth(), this.mLifeDrawView.getLifeHeight(), this.mCallback);
        this.mCurrentImplName = "Java";

        // initialize step # & tick delay
        this.setStep();
        speedSeekBar.setProgress(480);

        this.mSimulating = false;

    }

    @Override
    protected void onPause() {

        this.stopSimulation();

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu pMenu) {

        this.getMenuInflater()
            .inflate(R.menu.activity_main, pMenu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem pMenuItem) {

        switch (pMenuItem.getItemId()) {

            case R.id.activity_main_menu_action_java: {

                this.changeImplementation(COMPUTE_JAVA);

                break;
            }

            case R.id.activity_main_menu_action_gl: {

                this.changeImplementation(COMPUTE_GL);

                break;
            }

        }

        return super.onOptionsItemSelected(pMenuItem);
    }

    private void changeImplementation(final int pComputeImpl) {

        this.stopSimulation();

        this.mComputeImplForSwap = pComputeImpl;

        switch (pComputeImpl) {

            case COMPUTE_JAVA: {

                this.mSwapLifeCompute = new LifeComputeJava(this.mLifeCompute);

                break;
            }

            case COMPUTE_GL: {

                this.mSwapLifeCompute = new LifeComputeGL(this.mLifeCompute);

                break;
            }

        }

        this.mLifeCompute.destroy();

    }

    @Override
    protected void onDestroy() {

        this.mLifeCompute.destroy();

        super.onDestroy();
    }

    private void setTickDelay(final int pProgress) {

        this.mTickDelay = pProgress + 20;

        this.mDelayTextView.setText(String.format(Locale.US, "%s: %sms", this.getString(R.string.activity_main_tv_delay_text), pProgress + 20));

        if (this.mSimulating) {
            this.postTick(this.mTickDelay);
        }

    }

    public void btnClick(final View pView) {

        switch (pView.getId()) {

            case R.id.activity_main_btn_clear: {

                this.stopSimulation();

                this.mLifeCompute.clear();

                this.mLifeCompute.requestCellStates();

                break;
            }

            case R.id.activity_main_btn_next: {

                this.mTickHandler.removeCallbacks(this.mTickRunnable);
                this.mTickHandler.post(this.mTickRunnable);

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
        this.mStepTextView.setText(String.format(Locale.US, "%s step: %s", this.mCurrentImplName, this.mLifeCompute.getStep()));
    }

    private void postTick() {

        this.mTickHandler.removeCallbacks(this.mTickRunnable);
        this.mTickHandler.post(this.mTickRunnable);

    }

    private void postTick(final long pDelay) {

        this.mTickHandler.removeCallbacks(this.mTickRunnable);
        this.mTickHandler.postDelayed(this.mTickRunnable, pDelay);

    }

    private void startSimulation() {

        this.postTick();

        this.mSimulating = true;

        this.mStartStopButton.setText(R.string.activity_main_btn_stop_text);

    }

    private void stopSimulation() {

        this.mTickHandler.removeCallbacks(this.mTickRunnable);

        this.mSimulating = false;

        this.mStartStopButton.setText(R.string.activity_main_btn_start_text);

    }


}