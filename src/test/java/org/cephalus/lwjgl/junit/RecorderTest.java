package org.cephalus.lwjgl;

import org.cephalus.lwjgl.junit.LwjglRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

@RunWith(LwjglRunner.class)
@Fps(0)
public class RecorderTest {

    private Recorder recorder;
    private int frame;

    @Before
    public void setup() throws IOException {
        recorder = new Recorder("record");
    }

    @After
    public void close() throws IOException {
        recorder.close();
    }

    @Test
    public void record() throws IOException, LWJGLException {
        ++frame;
        float r = (120 - frame) / 120f;
        float g = frame / 120f;
        float b = 0f;
        float a = 1f;
        glClearColor(r, g, b, a);
        glClear(GL_COLOR_BUFFER_BIT);

        recorder.takeSnapshot();

        Display.swapBuffers();
    }
}
