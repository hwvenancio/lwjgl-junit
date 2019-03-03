package org.cephalus.lwjgl.junit;

import org.cephalus.lwjgl.Iterations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.internal.requests.ClassRequest;
import org.junit.internal.requests.FilterRequest;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.RunWith;
import org.junit.runner.manipulation.Filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LwjglRunnerTest {

    private static Map<Class<?>, List<String>> methodCalls = new HashMap<>();

    @Test
    public void fullyAnnotated() {
        List<String> methodList = runTestClass(FullyAnnotatedTest.class);

        assertThat(methodList)
                .containsExactly("beforeClass", "before", "test1", "after", "before", "test2", "after", "afterClass");
    }

    @Test
    public void filterFullyAnnotated() {
        Filter filter = new Filter() {
            @Override
            public boolean shouldRun(Description description) {
                return "test2".equals(description.getMethodName());
            }

            @Override
            public String describe() {
                return "Custom Filter";
            }
        };

        List<String> methodList = runTestClass(FullyAnnotatedTest.class, filter);

        assertThat(methodList)
                .containsExactly("beforeClass", "before", "test2", "after", "afterClass");
    }

    @Test
    public void classIgnored() {
        List<String> methodList = runTestClass(ClassIgnoredTest.class);

        assertThat(methodList)
                .isEmpty();
    }

    @Test
    public void singleIgnored() {
        List<String> methodList = runTestClass(SingleIgnoredTest.class);

        assertThat(methodList)
                .containsExactly("beforeClass", "before", "test1", "after", "afterClass");
    }

    private static List<String> runTestClass(Class<?> testClass) {
        return runTestClass(testClass, null);
    }

    private static synchronized List<String> runTestClass(Class<?> testClass, Filter filter) {
        methodCalls.put(testClass, new ArrayList<>());

        JUnitCore junit = new JUnitCore();

        Request request = new ClassRequest(testClass);
        if(filter != null)
            request = new FilterRequest(request, filter);

        junit.run(request);

        return methodCalls.remove(testClass);
    }

    @RunWith(LwjglRunner.class)
    @Iterations(1)
    public static class FullyAnnotatedTest {

        private static void add(String methodName) {
            methodCalls.get(FullyAnnotatedTest.class).add(methodName);
        }

        @BeforeClass
        public static void beforeClass() {
            add("beforeClass");
        }

        @AfterClass
        public static void afterClass() {
            add("afterClass");
        }

        @Before
        public void before() {
            add("before");
        }

        @After
        public void after() {
            add("after");
        }

        @Test
        public void test1() {
            add("test1");
        }

        @Test
        public void test2() {
            add("test2");
        }
    }

    @RunWith(LwjglRunner.class)
    @Iterations(1)
    @Ignore
    public static class ClassIgnoredTest {

        private static void add(String methodName) {
            methodCalls.get(ClassIgnoredTest.class).add(methodName);
        }

        @BeforeClass
        public static void beforeClass() {
            add("beforeClass");
        }

        @AfterClass
        public static void afterClass() {
            add("afterClass");
        }

        @Before
        public void before() {
            add("before");
        }

        @After
        public void after() {
            add("after");
        }

        @Test
        public void test1() {
            add("test1");
        }

        @Test
        public void test2() {
            add("test2");
        }
    }

    @RunWith(LwjglRunner.class)
    @Iterations(1)
    public static class SingleIgnoredTest {

        private static void add(String methodName) {
            methodCalls.get(SingleIgnoredTest.class).add(methodName);
        }

        @BeforeClass
        public static void beforeClass() {
            add("beforeClass");
        }

        @AfterClass
        public static void afterClass() {
            add("afterClass");
        }

        @Before
        public void before() {
            add("before");
        }

        @After
        public void after() {
            add("after");
        }

        @Test
        public void test1() {
            add("test1");
        }

        @Test
        @Ignore
        public void test2() {
            add("test2");
        }
    }
}
