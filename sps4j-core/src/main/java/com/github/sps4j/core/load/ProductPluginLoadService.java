package com.github.sps4j.core.load;


import com.github.sps4j.common.meta.PluginDesc;
import com.github.zafarkhaja.semver.Version;

import javax.annotation.Nonnull;

/**
 * An interface that provides information about the host product.
 * It is used by the plugin manager to determine plugin compatibility and perform initialization.
 *
 * @author Allan-QLB
 */
public interface ProductPluginLoadService {
    /**
     * Gets the version of the host product.
     *
     * @return The product's semantic version.
     */
    @Nonnull
    Version productVersion();

    /**
     * Performs any necessary initialization for the product service.
     * This is called by the plugin manager during its initialization.
     */
    default void init() {
        //do nothing by default
    }

    /**
     * Checks if a plugin with the given descriptor can be loaded by the current product.
     *
     * @param descriptor The descriptor of the plugin to check.
     * @return {@code true} if the plugin is compatible, {@code false} otherwise.
     */
    default boolean canLoad(PluginDesc descriptor) {
        return true;
    }

}
