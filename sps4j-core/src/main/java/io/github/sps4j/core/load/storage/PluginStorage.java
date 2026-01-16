package io.github.sps4j.core.load.storage;

import java.util.List;

/**
 * An interface for storages that can hold plugin packages.
 *
 * @author Allan-QLB
 */
public interface PluginStorage {

    /**
     * Lists all available plugin packages from a given base URL.
     *
     * @param baseUrl The base URL to scan for plugin packages.
     * @return A list of {@link PluginPackage} objects found at the URL.
     */
    List<PluginPackage> listPackages(String baseUrl);

}
