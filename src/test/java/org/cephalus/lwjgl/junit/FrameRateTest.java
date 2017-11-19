package org.cephalus.lwjgl.junit;

import org.cephalus.lwjgl.Fps;
import org.cephalus.lwjgl.Iterations;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Stopwatch;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

@RunWith(LwjglRunner.class)
@Iterations(10)
public class FrameRateTest {

    @Rule
    public Stopwatch timer = new Stopwatch() {
        @Override
        protected void succeeded(long nanos, Description description) {
            float time = nanos / 1_000_000_000f;
            System.out.println(String.format("%f seconds", time));
        }
    };

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
