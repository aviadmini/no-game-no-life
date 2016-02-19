package com.aviadmini.nogamenolife;

import com.aviadmini.nogamenolife.compute.LifeCompute;
import com.aviadmini.nogamenolife.compute.LifeComputeJava;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class LifeComputeJavaTest {

    private static final int[] GLIDER_5_0 = {//
            0, 0, 1, 0, 0,//
            1, 0, 1, 0, 0,//
            0, 1, 1, 0, 0,//
            0, 0, 0, 0, 0,//
            0, 0, 0, 0, 0,//
    };//glider 5x5, step 0

    private static final int[] GLIDER_5_5 = {//
            0, 0, 0, 0, 0,//
            0, 0, 1, 0, 0,//
            0, 0, 0, 1, 1,//
            0, 0, 1, 1, 0,//
            0, 0, 0, 0, 0,//
    };//glider 5x5, step 5

    private int mTicks;

    private LifeCompute mLifeCompute;

    @Before
    public void init() {

        this.mTicks = 5;

        this.mLifeCompute = new LifeComputeJava(5, 5);
        for (int i = GLIDER_5_0.length - 1; i >= 0; i--) {
            this.mLifeCompute.changeCellState(i, GLIDER_5_0[i]);
        }

    }

    @After
    public void deInit() {
        this.mLifeCompute.destroy();
    }

    @Test
    public void testTick()
            throws Exception {

        for (int i = 0; i < this.mTicks; i++) {
            this.mLifeCompute.tick();
        }

        assertArrayEquals("result not correct", this.mLifeCompute.getCellStates(), GLIDER_5_5);

        assertEquals("steps not equal", this.mLifeCompute.getStep(), this.mTicks);

    }

}