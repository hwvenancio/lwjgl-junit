package org.cephalus.lwjgl.junit;

import org.cephalus.lwjgl.Fps;
import org.cephalus.lwjgl.Iterations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LwjglRunner.class)
@Iterations(10)
public class FrameRateTest {

    private volatile long startNanos;
    private volatile long endNanos;

    @Before
    public void startTimer() {
        startNanos = System.nanoTime();
    }

    @After
    public void stopTimer() {
        endNanos = System.nanoTime();
        float time = (endNanos - startNanos) / 1_000_000_000f;
        System.out.println(String.format("%f seconds", time));
    }

    @Test
    @Fps(5)
    public void fps5() {

    }

    @Test
    @Fps(10)
    public void fps10() {

    }

    @Test
    @Fps(20)
    public void fps20() {

    }
}
