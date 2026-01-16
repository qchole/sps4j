package io.github.sps4j.core.load;



import io.github.sps4j.common.meta.MetaInfo;
import io.github.sps4j.core.Sps4jPlugin;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Interface responsible for loading a plugin.
 *
 * @author Allan-QLB
 */
public interface Sps4jPluginLoader {

    /**
     * Loads a plugin from its metadata.
     *
     * @param pluginMetadata The metadata of the plugin to load.
     * @param cl The class loader to use. If null, a new one will be created.
     * @param conf The configuration for the plugin.
     * @return The loaded plugin instance.
     */
    Sps4jPlugin load(MetaInfo pluginMetadata, Sps4jPluginClassLoader cl, Map<String, Object> conf);

    /**
     * Creates an instance of the plugin.
     *
     * @param clazz The fully qualified name of the plugin class.
     * @param cl The class loader to use.
     * @return The created plugin instance.
     * @throws Exception if an error occurs during instantiation.
     */
    Sps4jPlugin createPluginInstance(String clazz, Sps4jPluginClassLoader cl) throws Exception;

    /**
     * A hook called right after the plugin instance is created, before {@link Sps4jPlugin#onLoad(Map, MetaInfo)} is called.
     *
     * @param pluginInstance The newly created plugin instance.
     * @param metadata The metadata of the plugin.
     * @return The plugin instance, possibly wrapped or modified.
     */
    default Sps4jPlugin pluginCreated(@Nonnull Sps4jPlugin pluginInstance, @Nonnull MetaInfo metadata) {
        return pluginInstance;
    }

    /**
     * A hook called after the plugin is fully loaded and {@link Sps4jPlugin#onLoad(Map, MetaInfo)} has been called.
     *
     * @param pluginInstance The loaded plugin instance.
     * @param metadata The metadata of the plugin.
     * @return The plugin instance, possibly wrapped or modified.
     */
    default Sps4jPlugin postLoadPlugin(@Nonnull Sps4jPlugin pluginInstance, @Nonnull MetaInfo metadata) {
        return pluginInstance;
    }

}
