package org.cephalus.lwjgl.junit;

import org.cephalus.lwjgl.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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

import java.util.ArrayList;
import java.util.List;

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
        CombinedConfiguration configuration = getConfiguration(testMethod);

        Description testDescription = describeChild(testMethod);
        try {
            new Runner(notifier, configuration, getTestClass(), testMethod, testDescription).evaluate();
        } catch (Throwable e) {
            notifier.fireTestFailure(new Failure(testDescription, e));
        }
    }

    private CombinedConfiguration getConfiguration(final FrameworkMethod testMethod) {
        Configuration defaultConfiguration = Runner.class.getAnnotation(Configuration.class);
        return new CombinedConfiguration(defaultConfiguration, getTestClass(), testMethod);
    }

    @Configuration
    private static class Runner extends Statement {

        private final RunNotifier notifier;
        private final CombinedConfiguration config;
        private final TestClass testClass;
        private final FrameworkMethod testMethod;
        private final Description testDescription;
        private final String title;

        private Object testInstance;

        private List<Throwable> errors = new ArrayList<>();

        private int iterations = 0;

        public Runner(RunNotifier notifier, CombinedConfiguration config, TestClass testClass, FrameworkMethod testMethod, Description testDescription) throws InitializationError {
            this.notifier = notifier;
            this.config = config;
            this.testClass = testClass;
            this.testMethod = testMethod;
            this.testDescription = testDescription;
            this.title = testDescription.getMethodName();
        }

        @Override
        public void evaluate() throws ReflectiveOperationException, LWJGLException {
            testInstance = testClass.getOnlyConstructor().newInstance();

            notifier.fireTestStarted(testDescription);

            createWindow();

            runBefores();
            runTest();
            runAfters();

            disposeWindow();

            for(Throwable error : errors) {
                notifier.fireTestFailure(new Failure(testDescription, error));
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

        public void runTest() {
            while (errors.isEmpty() && ++iterations <= config.iterations) {
                invoke(testMethod);
                if(config.fps > 0) {
                    Display.sync(config.fps);
                }
            }
        }
    }

    private static class CombinedConfiguration {
        private int profile;
        private int width;
        private int height;
        private int fps;
        private int iterations;

        public CombinedConfiguration(Configuration defaultConfiguration, TestClass testClass, Annotatable testMethod) {
            apply(defaultConfiguration);

            applyAll(testClass);
            applyAll(testMethod);
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
        }

        private void apply(Configuration configuration) {
            if(configuration == null)
                return;
            profile = configuration.profile();
            width = configuration.width();
            height = configuration.height();
            fps = configuration.fps();
            iterations = configuration.iterations();
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
    }
}
