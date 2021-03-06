Support for lwjgl testing using junit4

## Example

```java
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
```

## Configuring tests:

Most test configurations are annotation based:

- `@Iterations`: Number of times the test method will be looped
- `@Swap`: [AUTO|MANUAL] If double buffer swapping should be done automatically or manually
- `@Window`: Sets width and height of test window
- `@Profile`: Integer value of OpenGL Profile to be used (e.g.: 330 is OpenGL 3.3)
- `@Fps`: Tries to match supplied frames per second

or:

- `@Configuration`: configures all previous values with one annotation

## Comparing with reference:

You can provide reference frames via a zip file, and that will be used to validate frames of a test:

- `@Compare(reference = "triangle", maxDivergence = 0.05f)`: will compare frames with reference from triangle.zip, with a max divergence of 5%

If no name is supplied, assumes that file name is the same as the test method name.

A diff image file will be generated if it passes maxDivergence threshold