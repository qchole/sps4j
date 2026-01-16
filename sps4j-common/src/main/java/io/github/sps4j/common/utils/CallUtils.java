package io.github.sps4j.common.utils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

/**
 * Utility class for executing operations with a specific {@link ClassLoader} set as the context class loader.
 * This is particularly useful for plugin environments where class loading context needs to be managed.
 *
 * @author Allan-QLB
 */
@UtilityClass
public class CallUtils {

    /**
     * Executes a {@link Call} operation with a specified {@link ClassLoader} as the current thread's
     * context class loader. The original context class loader is restored after the operation.
     *
     * @param classLoader The {@link ClassLoader} to set as the context class loader.
     * @param call The {@link Call} operation to execute.
     * @param <R> The return type of the {@link Call} operation.
     * @return The result of the {@link Call} operation.
     * @throws Throwable If the {@link Call} operation throws an exception.
     */
    @SneakyThrows
    public static <R> R executeWithContextLoader(ClassLoader classLoader, Call<R> call)  {
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        if (current == null) {
            return call.call();
        }
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            return call.call();
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    /**
     * Executes a {@link Runnable} operation with a specified {@link ClassLoader} as the current thread's
     * context class loader. The original context class loader is restored after the operation.
     *
     * @param classLoader The {@link ClassLoader} to set as the context class loader.
     * @param runnable The {@link Runnable} operation to execute.
     */
    @SneakyThrows
    public static void runWithContextLoader(ClassLoader classLoader, Runnable runnable) {
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        if (current == null) {
            runnable.run();
        }
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            runnable.run();
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }
}
