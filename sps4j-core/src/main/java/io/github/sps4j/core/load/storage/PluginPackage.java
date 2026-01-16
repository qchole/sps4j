package io.github.sps4j.core.load.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a package containing plugin resources, such as a JAR file.
 * It provides access to the contents of the package and is auto-closeable.
 *
 * @author Allan-QLB
 */
public interface PluginPackage extends AutoCloseable {

    /**
     * Gets the base URL of the plugin package.
     *
     * @return The base URL as a string.
     */
    String getBaseUrl();

    /**
     * Retrieves an resource from the package as an {@link InputStream}.
     *
     * @param resource The name of the resource to retrieve.
     * @return An {@link InputStream} for the specified resource.
     * @throws IOException if the resource is not found or an I/O error occurs.
     */
    InputStream getResource(String resource) throws IOException;

    /**
     * Checks if the package contains a specific resource.
     *
     * @param resource The name of the resource to check.
     * @return {@code true} if the resource exists, {@code false} otherwise.
     */
    boolean contains(String resource);

}
