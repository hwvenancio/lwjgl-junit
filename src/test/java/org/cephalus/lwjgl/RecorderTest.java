package org.cephalus.lwjgl;

import org.cephalus.lwjgl.junit.LwjglRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

@RunWith(LwjglRunner.class)
@Iterations(11)
public class RecorderTest {

    private final int max = 10;
    private final float den = max;

    private Recorder recorder;
    private int frame;

    @Before
    public void setup() throws IOException {
        recorder = new Recorder("record");
    }

    @After
    public void close() throws IOException {
        recorder.close();
        File zipFile = new File("target/recorded-frames/record.zip");
        try {
            ZipInputStream zip = new ZipInputStream(new FileInputStream(zipFile));
            int i;
            for(i = 0; i <= max; ++i) {
                ZipEntry entry = zip.getNextEntry();
                assertNotNull("Missing frame " + i, entry);
                BufferedImage frame = ImageIO.read(zip);
                int argb = frame.getRGB(0, 0);
                int actualA = (argb >> 24) & 0xFF;
                int actualR = (argb >> 16) & 0xFF;
                int actualG = (argb >> 8) & 0xFF;
                int actualB = (argb) & 0xFF;
                float expectedA = 1f;
                float expectedR = (max - i) / den;
                float expectedG = i / den;
                float expectedB = 0f;
                float delta = 0.002f;
                assertEquals(expectedA, actualA / 255f, delta);
                assertEquals(expectedR, actualR / 255f, delta);
                assertEquals(expectedG, actualG / 255f, delta);
                assertEquals(expectedB, actualB / 255f, delta);
            }
            assertNull("Extra frame " + i, zip.getNextEntry());
            zip.close();
        } finally {
            recorder.clear();
        }
    }

    @Test
    public void record() throws IOException, LWJGLException {
        float r = (max - frame) / den;
        float g = frame / den;
        float b = 0f;
        float a = 1f;
        glClearColor(r, g, b, a);
        glClear(GL_COLOR_BUFFER_BIT);

        recorder.saveSnapshot();

        Display.swapBuffers();
        ++frame;
    }
}
