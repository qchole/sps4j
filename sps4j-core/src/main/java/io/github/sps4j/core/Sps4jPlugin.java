package io.github.sps4j.core;

import io.github.sps4j.common.meta.MetaInfo;
import java.util.Map;

/**
 * The base interface for all SPS4J plugins.
 * Plugin implementations must implement an interface that extends this one.
 * It defines the lifecycle methods for a plugin.
 *
 * @author Allan-QLB
 */
@SuppressWarnings("java:S112")
public interface Sps4jPlugin {

    /**
     * Called when the plugin is loaded and initialized.
     * This method can be used to perform setup tasks.
     *
     * @param conf a {@link Map} containing configuration for the plugin.
     * @param metaInfo the {@link MetaInfo} object containing metadata about the plugin.
     */
    default void onLoad(Map<String, Object> conf, MetaInfo metaInfo) {
    }

    /**
     * Called when the plugin is about to be unloaded.
     * This method should be used to release any resources held by the plugin.
     */
    default void onDestroy() {

    }

}
