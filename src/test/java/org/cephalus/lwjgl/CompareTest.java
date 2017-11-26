package org.cephalus.lwjgl;

import org.cephalus.lwjgl.junit.LwjglRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.cephalus.lwjgl.Swap.Type.AUTO;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glDetachShader;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

@RunWith(LwjglRunner.class)
@Iterations(1)
@Swap(AUTO)
public class CompareTest {

    private int vs;
    private int fs;
    private int program;

    private int position;
    private int errPosition;
    private int color;
    private int okVao;
    private int errVao;

    @Before
    public void shader() {
        vs = loadShader("simple-color.vs", GL_VERTEX_SHADER);
        fs = loadShader("simple-color.fs", GL_FRAGMENT_SHADER);
        program = linkProgram(vs, fs);

        glUseProgram(program);
    }

    @Before
    public void points() {
        position = loadBuffer(
                -0.5f, -0.5f, 0f
                , 0f, 0.707f, 0f
                , 0.5f, -0.5f, 0f
        );

        errPosition = loadBuffer(
                -0.5f, -0.5f, 0f
                , 0f, 0.6f, 0f
                , 0.5f, -0.5f, 0f
        );

        color = loadBuffer(
                1f, 0f, 0f
                , 0f, 1f, 0f
                , 0f, 0f, 1f
        );

        okVao = glGenVertexArrays();
        glBindVertexArray(okVao);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, position);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, color);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        errVao = glGenVertexArrays();
        glBindVertexArray(errVao);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, errPosition);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, color);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    @After
    public void dispose() {
        glBindVertexArray(0);
        glDeleteBuffers(color);
        glDeleteBuffers(errPosition);
        glDeleteBuffers(position);
        glDeleteVertexArrays(okVao);
        glDeleteVertexArrays(errVao);

        glUseProgram(0);
        glDetachShader(program, vs);
        glDetachShader(program, fs);
        glDeleteShader(fs);
        glDeleteShader(vs);
        glDeleteProgram(program);
    }

    @Test
    @Compare
    public void triangle() throws LWJGLException, IOException {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(program);
        glBindVertexArray(okVao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);
        glUseProgram(0);
    }

    @Test(expected = AssertionError.class)
    @Compare(reference = "triangle")
    public void differentTriangle() throws LWJGLException, IOException {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(program);
        glBindVertexArray(errVao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);
        glUseProgram(0);
    }

    private static int loadShader(String resource, int type) {
        int shader = glCreateShader(type);
        InputStream source = CompareTest.class.getResourceAsStream(resource);
        try(Scanner scanner = new Scanner(source, StandardCharsets.UTF_8.name())) {
            String text = scanner.useDelimiter("\\A").next();
            glShaderSource(shader, text);
        }
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException(glGetShaderInfoLog(shader, 1024));
        }
        return shader;
    }

    private static int linkProgram(int... shaders) {
        int program = glCreateProgram();
        for(int shader : shaders) {
            glAttachShader(program, shader);
        }
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException(glGetProgramInfoLog(program, 1024));
        }
        return program;
    }

    private static int loadBuffer(float... values) {
        FloatBuffer data = BufferUtils.createFloatBuffer(values.length);
        data.put(values).flip();
        return loadBuffer(data);
    }

    private static int loadBuffer(FloatBuffer data) {
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return vbo;
    }
}
