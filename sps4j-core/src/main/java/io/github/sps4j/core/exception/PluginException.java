package io.github.sps4j.core.exception;

/**
 * A runtime exception thrown for errors that occur during plugin management,
 * such as loading, unloading, or execution.
 *
 * @author Allan-QLB
 */
public class PluginException extends RuntimeException {

    /**
     * Constructs a new plugin exception with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the cause.
     */
    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new plugin exception with the specified detail message.
     *
     * @param message the detail message.
     */
    public PluginException(String message) {
        super(message);
    }
}
