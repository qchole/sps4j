package io.github.sps4j.common.utils;

/**
 * A functional interface representing a callable operation that can return a result and throw an exception.
 * This is similar to {@link java.util.concurrent.Callable} but allows throwing any {@link Throwable}.
 *
 * @param <R> The type of the result returned by the call.
 * @author Allan-QLB
 */
@FunctionalInterface
public interface Call<R> {
    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return The computed result.
     * @throws Throwable If unable to compute a result.
     */
    R call() throws Throwable;
}
