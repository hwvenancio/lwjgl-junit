package org.cephalus.lwjgl;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.lwjgl.opengl.GL11.*;

public class Recorder {

    private final ZipOutputStream zip;
    private String testName;
    private int frame;

    public Recorder(String testName) throws IOException {
        this.testName = testName;
        File file = new File("target/recorded-frames/" + testName + ".zip");
        file.getParentFile().mkdirs();
        this.zip = new ZipOutputStream(new FileOutputStream(file));
    }

    public void takeSnapshot() throws IOException, LWJGLException {
        glFlush();
        glFinish();
        glReadBuffer(GL_BACK);
        int width = Display.getDisplayMode().getWidth();
        int height = Display.getDisplayMode().getHeight();
        int bpp = 4;
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * bpp);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        String name = String.format(testName + "_%04d.png", ++frame);
        ZipEntry snapshot = new ZipEntry(name);

        String format = "PNG";
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for(int x = 0; x < width; x++)
        {
            for(int y = 0; y < height; y++)
            {
                int i = (x + (width * y)) * bpp;
                int r = buffer.get(i) & 0xFF;
                int g = buffer.get(i + 1) & 0xFF;
                int b = buffer.get(i + 2) & 0xFF;
                image.setRGB(x, height - (y + 1), (0xFF << 24) | (r << 16) | (g << 8) | b);
            }
        }

        zip.putNextEntry(snapshot);
        ImageIO.write(image, format, zip);
    }

    public void close() throws IOException {
        zip.flush();
        zip.close();
    }
}
