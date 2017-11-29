package org.cephalus.lwjgl.junit;

import org.cephalus.lwjgl.*;
import org.cephalus.lwjgl.Window;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.*;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.cephalus.lwjgl.ImageComparator.calculateDivergence;
import static org.cephalus.lwjgl.ImageComparator.getDifferenceImage;
import static org.cephalus.lwjgl.Swap.Type.AUTO;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LwjglRunner extends ParentRunner<FrameworkMethod> {

    public LwjglRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    public Description getDescription() {
        Description description = Description.createSuiteDescription(getTestClass().getName());
        List<FrameworkMethod> testMethods = getTestClass().getAnnotatedMethods(Test.class);
        for(FrameworkMethod testMethod : testMethods) {
            description.addChild(Description.createTestDescription(getTestClass().getName(), testMethod.getName()));
        }
        return description;
    }

    @Override
    protected List<FrameworkMethod> getChildren() {
        return getTestClass().getAnnotatedMethods(Test.class);
    }

    @Override
    protected Description describeChild(FrameworkMethod testMethod) {
        return Description.createTestDescription(getTestClass().getName(), testMethod.getName());
    }

    @Override
    protected void runChild(final FrameworkMethod testMethod, final RunNotifier notifier) {
        Description testDescription = describeChild(testMethod);
        try {
            new Runner(notifier, getTestClass(), testMethod, testDescription).run();
        } catch (Throwable e) {
            notifier.fireTestFailure(new Failure(testDescription, e));
        }
    }

    private static class Runner implements Runnable {

        private final RunNotifier notifier;
        private final TestClass testClass;
        private final FrameworkMethod testMethod;
        private final Description testDescription;

        private Object testInstance;

        public Runner(RunNotifier notifier, TestClass testClass, FrameworkMethod testMethod, Description testDescription) {
            this.notifier = notifier;
            this.testClass = testClass;
            this.testMethod = testMethod;
            this.testDescription = testDescription;
        }

        @Override
        public void run() {
            try {
                testInstance = testClass.getOnlyConstructor().newInstance();

                Statement test = new LoopRunner(notifier, testClass, testMethod, testDescription, testInstance);
                test = withRules(test);
                test.evaluate();
            } catch (Throwable ex) {
                notifier.fireTestFailure(new Failure(testDescription, ex));
            }
        }

        private Statement withRules(Statement base) {
            List<TestRule> result = testClass.getAnnotatedFieldValues(testInstance, Rule.class, TestRule.class);
            return new RunRules(base, result, testDescription);
        }
    }

    @Configuration
    private static class LoopRunner extends Statement {

        private final RunNotifier notifier;
        private final TestClass testClass;
        private final FrameworkMethod testMethod;
        private final Description testDescription;
        private final Object testInstance;
        private final String title;
        private final List<Class<? extends Throwable>> exceptions;

        private CombinedConfiguration config;

        private List<Throwable> errors = new ArrayList<>();

        private int iterations = 0;

        public LoopRunner(RunNotifier notifier, TestClass testClass, FrameworkMethod testMethod, Description testDescription, Object testInstance) throws InitializationError {
            this.notifier = notifier;
            this.testClass = testClass;
            this.testMethod = testMethod;
            this.testDescription = testDescription;
            this.testInstance = testInstance;
            this.title = testDescription.getMethodName();
            this.exceptions = extractExpectedExceptions(testMethod);
        }

        @Override
        public void evaluate() throws ReflectiveOperationException, LWJGLException {
            notifier.fireTestStarted(testDescription);

            this.config = getConfiguration(testMethod);

            createWindow();
            try {
                runBefores();
                runTest();
                runAfters();
            } catch (Throwable error) {
                errors.add(error);
            } finally {
                disposeWindow();
            }

            for(Throwable error : errors) {
                if(!expectedException(error))
                    notifier.fireTestFailure(new Failure(testDescription, error));
            }
            for(Class<? extends Throwable> expected : exceptions) {
                if(errors.stream().noneMatch(error -> expected.isInstance(error)))
                    notifier.fireTestFailure(new Failure(testDescription
                            , new AssertionError("Expected exception: " + expected.getName())));
            }

            notifier.fireTestFinished(testDescription);
        }

        public void createWindow() throws LWJGLException {
            int major = config.profile / 100;
            int minor = config.profile % 100 / 10;
            PixelFormat pixelFormat = new PixelFormat();
            ContextAttribs contextAttributes = new ContextAttribs(major, minor)
                    .withForwardCompatible(true)
                    .withProfileCore(true);

            Display.setDisplayMode(new DisplayMode(config.width, config.height));
            Display.setTitle(title);
            Display.create(pixelFormat, contextAttributes);
        }

        public void disposeWindow() {
            Display.destroy();
        }

        private void invoke(FrameworkMethod method, final Object... params) {
            try {
                method.invokeExplosively(testInstance, params);
            } catch (Throwable e) {
                errors.add(e);
            }
        }

        private void invokeAll(List<FrameworkMethod> methods, final Object... params) {
            for (FrameworkMethod each : methods) {
                invoke(each, params);
            }
        }

        public void runBefores() {
            List<FrameworkMethod> befores = testClass.getAnnotatedMethods(Before.class);
            invokeAll(befores);
        }

        public void runAfters() {
            List<FrameworkMethod> afters = testClass.getAnnotatedMethods(After.class);
            invokeAll(afters);
        }

        public void runTest() throws LWJGLException {
            while (errors.isEmpty() && ++iterations <= config.iterations) {
                invoke(testMethod);
                compare();
                if(config.swap)
                    Display.swapBuffers();
                if(config.fps > 0 && errors.isEmpty()) {
                    Display.sync(config.fps);
                }
            }
        }

        private CombinedConfiguration getConfiguration(final FrameworkMethod testMethod) {
            Configuration defaultConfiguration = LoopRunner.class.getAnnotation(Configuration.class);
            return new CombinedConfiguration(defaultConfiguration, testClass, testMethod);
        }

        private boolean expectedException(Throwable error) {
            for(Class<? extends Throwable> errorClass : exceptions)
                if(errorClass.isInstance(error))
                    return true;
            return false;
        }

        private List<Class<? extends Throwable>> extractExpectedExceptions(FrameworkMethod testMethod) {
            Test test = testMethod.getAnnotation(Test.class);
            if(test == null || test.expected() == null || test.expected() == Test.None.class)
                return Collections.emptyList();
            return Collections.singletonList(test.expected());
        }

        private void compare() {
            if(config.compare != null) {
                config.compare.compareNext();
            }
        }
    }

    private static class CombinedConfiguration {
        private int profile;
        private int width;
        private int height;
        private int fps;
        private int iterations;
        private boolean swap;
        private CombinedCompare compare;

        public CombinedConfiguration(Configuration defaultConfiguration, TestClass testClass, FrameworkMethod testMethod) {
            apply(defaultConfiguration);

            applyAll(testClass);
            applyAll(testMethod);
            compare = CombinedCompare.create(testClass, testMethod);
        }

        private void applyAll(Annotatable source) {
            Configuration configuration = source.getAnnotation(Configuration.class);
            apply(configuration);
            Profile profile = source.getAnnotation(Profile.class);
            apply(profile);
            Window window = source.getAnnotation(Window.class);
            apply(window);
            Fps fps = source.getAnnotation(Fps.class);
            apply(fps);
            Iterations iterations = source.getAnnotation(Iterations.class);
            apply(iterations);
            Swap swap = source.getAnnotation(Swap.class);
            apply(swap);
        }

        private void apply(Configuration configuration) {
            if(configuration == null)
                return;
            profile = configuration.profile();
            width = configuration.width();
            height = configuration.height();
            fps = configuration.fps();
            iterations = configuration.iterations();
            swap = configuration.swap() == AUTO;
        }

        private void apply(Profile annotation) {
            if(annotation == null)
                return;
            profile = annotation.value();
        }

        private void apply(Window annotation) {
            if(annotation == null)
                return;
            width = annotation.width();
            height = annotation.height();
        }

        private void apply(Fps annotation) {
            if(annotation == null)
                return;
            fps = annotation.value();
        }

        private void apply(Iterations annotation) {
            if(annotation == null)
                return;
            iterations = annotation.value();
        }

        private void apply(Swap annotation) {
            if(annotation == null)
                return;
            swap = annotation.value() == AUTO;
        }
    }

    private static class CombinedCompare {

        private final Class<?> javaClass;
        private final String methodName;

        private String reference;
        private float maxDivergence;

        private ZipInputStream zip;

        public CombinedCompare(Class<?> javaClass, String methodName) {
            this.javaClass = javaClass;
            this.methodName = methodName;
        }

        public static CombinedCompare create(TestClass testClass, FrameworkMethod testMethod) {
            Compare methodCompare = testMethod.getAnnotation(Compare.class);
            Compare classCompare = testClass.getAnnotation(Compare.class);

            if(methodCompare == null && classCompare == null)
                return null;

            CombinedCompare instance = new CombinedCompare(testClass.getJavaClass(), testMethod.getName());
            instance.apply(testMethod);
            instance.apply(classCompare);
            instance.apply(methodCompare);
            instance.start();
            return instance;
        }

        public void compareNext() {
            try {
                ZipEntry entry = zip.getNextEntry();
                BufferedImage expected = ImageIO.read(zip);
                BufferedImage actual = Recorder.takeSnapshot();
                BufferedImage diff = getDifferenceImage(expected, actual);
                float divergence = calculateDivergence(diff);
                try {
                    assertTrue(divergence <= maxDivergence);
                } catch (AssertionError ex) {
                    save(methodName, entry.getName(), diff);
                    throw ex;
                }
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        private void save(String methodName, String frameName, BufferedImage diff) throws IOException {
            File file = new File("target/recorded-frames/diff_" + methodName + "_" + frameName);
            file.getParentFile().mkdirs();
            ImageIO.write(diff, "PNG", file);
        }

        private void start() {
            URL resource = javaClass.getResource(reference + ".zip");
            assertNotNull("Reference not found!", resource);
            zip = new ZipInputStream(javaClass.getResourceAsStream(reference + ".zip"));
        }

        private void apply(FrameworkMethod method) {
            if(reference == null || reference.isEmpty())
                reference = method.getName();
        }

        private void apply(Compare compare) {
            if(compare == null)
                return;

            if(compare.reference() != null && !compare.reference().isEmpty())
                this.reference = compare.reference();
            this.maxDivergence = compare.maxDivergence();
        }
    }
}
