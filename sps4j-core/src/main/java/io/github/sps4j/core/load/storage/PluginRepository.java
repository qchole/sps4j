package io.github.sps4j.core.load.storage;

import java.util.List;

/**
 * An interface for storages that can hold plugin packages.
 *
 * @author Allan-QLB
 */
public interface PluginRepository {

    /**
     * Lists all available plugin packages.
     *
     * @return A list of {@link PluginPackage} objects found at the repository.
     */
    List<PluginPackage> listPackages();

}
