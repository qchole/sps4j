package io.github.sps4j.core.load;


import static org.junit.jupiter.api.Assertions.*;

class PluginMethodInvocationInterceptorTest {

    @org.junit.jupiter.api.Test
    void testIntercept() throws Throwable {
        PluginMethodInvocationInterceptor interceptor
                = new PluginMethodInvocationInterceptor();
        Test test = new Test();
        assertEquals("test", interceptor.intercept(test, Test.class.getMethod("test") , new Object[0]));
        assertThrows(UnsupportedOperationException.class,
                () -> interceptor.intercept(test, Test.class.getMethod("error") , new Object[0]));



    }


    static class Test {
        public String test() {
            return "test";
        }

        public String error() {
           throw new UnsupportedOperationException();
        }
    }

}