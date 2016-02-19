package com.aviadmini.nogamenolife;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.test.ActivityInstrumentationTestCase2;
import android.test.InstrumentationTestCase;
import android.test.TouchUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;

import com.aviadmini.nogamenolife.views.LifeDrawView;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class MainActivityTest
        extends ActivityInstrumentationTestCase2<MainActivity> {

    private Instrumentation mInstrumentation;

    private MainActivity mMainActivity;

    private Button       mNextButton;
    private LifeDrawView mLifeDrawView;

    public MainActivityTest(final Class<MainActivity> activityClass) {
        super(activityClass);
    }

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp()
            throws Exception {
        super.setUp();

        this.setActivityInitialTouchMode(false);

        this.mInstrumentation = this.getInstrumentation();

        this.mMainActivity = this.getActivity();

        this.mNextButton = (Button) this.mMainActivity.findViewById(R.id.activity_main_btn_next);

        this.mLifeDrawView = (LifeDrawView) this.mMainActivity.findViewById(R.id.activity_main_life);

    }

    // not much to expect from this test
    public void testViewContents()
            throws Exception {

        touchView(this, this.mLifeDrawView, 1, 1);
        touchView(this, this.mLifeDrawView, 1, this.mLifeDrawView.getHeight() - 1);

        final int[] initial = this.mLifeDrawView.getCellStates()
                                                .clone();

        TouchUtils.clickView(this, this.mNextButton);

        final int[] end = this.mLifeDrawView.getCellStates()
                                            .clone();

        assertThat(initial, is(not(equalTo(end))));

        touchView(this, this.mLifeDrawView, 1, 1);
        touchView(this, this.mLifeDrawView, 1, this.mLifeDrawView.getHeight() - 1);

        final int[] initial2 = this.mLifeDrawView.getCellStates()
                                                 .clone();

        TouchUtils.clickView(this, this.mNextButton);

        touchView(this, this.mLifeDrawView, 1, 1);
        touchView(this, this.mLifeDrawView, 1, this.mLifeDrawView.getHeight() - 1);

        final int[] end2 = this.mLifeDrawView.getCellStates()
                                             .clone();

        assertThat(initial2, is(equalTo(end2)));

    }

    @Override
    public void tearDown()
            throws Exception {

        this.getActivity()
            .finish();

        super.tearDown();
    }

    private static void touchView(@NonNull final InstrumentationTestCase pInstrumentationTestCase, @NonNull final View pView, final float pX,
                                  final float pY) {

        final Instrumentation test = pInstrumentationTestCase.getInstrumentation();

        final int[] xy = new int[2];
        pView.getLocationOnScreen(xy);

        final int viewWidth = pView.getWidth();
        final int viewHeight = pView.getHeight();

        final float x = xy[0] + pX;
        float y = xy[1] + pY;

        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0);
        test.sendPointerSync(event);
        test.waitForIdleSync();

        eventTime = SystemClock.uptimeMillis();
        final int touchSlop = ViewConfiguration.get(pView.getContext())
                                               .getScaledTouchSlop();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, x + (touchSlop / 2.0f), y + (touchSlop / 2.0f), 0);
        test.sendPointerSync(event);
        test.waitForIdleSync();

        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
        test.sendPointerSync(event);
        test.waitForIdleSync();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
