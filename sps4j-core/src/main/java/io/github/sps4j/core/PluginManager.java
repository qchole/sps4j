package io.github.sps4j.core;

import io.github.sps4j.common.meta.MetaInfo;
import io.github.sps4j.common.meta.PluginArtifact;
import io.github.sps4j.core.load.PluginWrapper;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

/**
 * The central interface for managing plugins within the SPS4J framework.
 * It handles the entire lifecycle of plugins, including discovery, loading, retrieval, and unloading.
 *
 * @author Allan-QLB
 */
public interface PluginManager {
    /**
     * Initializes the plugin manager. This typically involves discovering plugin interfaces and loading metadata.
     */
    default void init() {
    }

    /**
     * Resets the entire plugin manager. It unloads all currently loaded plugins and reloads all metadata.
     */
    void resetAll();

    /**
     * Resets a specific plugin artifact. It unloads the specific plugin and reloads its metadata.
     *
     * @param artifact The plugin artifact to reset.
     */
    void reset(@Nonnull PluginArtifact artifact);

    /**
     * Retrieves the metadata for the latest available version of a plugin.
     *
     * @param artifact The plugin artifact to get metadata for.
     * @return The {@link MetaInfo} for the plugin, or {@code null} if not found.
     */
    MetaInfo getPluginMetaInfo(@Nonnull PluginArtifact artifact);

    /**
     * Gets a wrapper for a loaded plugin of the specified type and name.
     * If the plugin is not already loaded, it will be loaded.
     *
     * @param type The type of the plugin.
     * @param name The name of the plugin.
     * @return A {@link PluginWrapper} containing the plugin instance and its metadata.
     */
    PluginWrapper getPlugin(@Nonnull String type, @Nonnull String name);

    /**
     * Gets a wrapper for a loaded plugin identified by its artifact.
     * If the plugin is not already loaded, it will be loaded.
     *
     * @param artifact The plugin artifact.
     * @return A {@link PluginWrapper} containing the plugin instance and its metadata.
     */
    PluginWrapper getPlugin(@Nonnull PluginArtifact artifact);

    /**
     * Gets a wrapper for a loaded plugin with a specific configuration.
     *
     * @param type The type of the plugin.
     * @param name The name of the plugin.
     * @param config A map of configuration properties for the plugin.
     * @return A {@link PluginWrapper} containing the plugin instance and its metadata.
     */
    PluginWrapper getPlugin(@Nonnull String type, @Nonnull String name, @Nonnull Map<String, Object> config);

    /**
     * Gets a wrapper for a loaded plugin by its interface and name, with a specific configuration.
     *
     * @param pluginInterface The interface class of the plugin.
     * @param name The name of the plugin.
     * @param config A map of configuration properties for the plugin.
     * @return A {@link PluginWrapper} containing the plugin instance and its metadata.
     */
    PluginWrapper getPlugin(@Nonnull Class<?> pluginInterface, @Nonnull String name, @Nonnull Map<String, Object> config);


    /**
     * Gets the raw, unwrapped plugin instance, cast to the specified interface type.
     *
     * @param pluginInterface The plugin interface class to cast to.
     * @param artifact The plugin artifact.
     * @param <T> The type of the plugin interface.
     * @return The unwrapped plugin instance.
     */
    <T extends Sps4jPlugin> T getPluginUnwrapped(@Nonnull Class<T> pluginInterface, @Nonnull PluginArtifact artifact);

    /**
     * Gets the raw, unwrapped plugin instance with a specific configuration, cast to the specified interface type.
     *
     * @param pluginInterface The plugin interface class to cast to.
     * @param artifact The plugin artifact.
     * @param config A map of configuration properties for the plugin.
     * @param <T> The type of the plugin interface.
     * @return The unwrapped plugin instance.
     */
    <T extends Sps4jPlugin> T getPluginUnwrapped(@Nonnull Class<T> pluginInterface, @Nonnull PluginArtifact artifact, @Nonnull Map<String, Object> config);

    /**
     * Gets the raw, unwrapped plugin instance with a specific configuration, cast to the specified interface type.
     *
     * @param pluginInterface The plugin interface class to cast to.
     * @param name The name of the plugin.
     * @param config A map of configuration properties for the plugin.
     * @param <T> The type of the plugin interface.
     * @return The unwrapped plugin instance.
     */
    <T extends Sps4jPlugin> T getPluginUnwrapped(@Nonnull Class<T> pluginInterface, @Nonnull String name, @Nonnull Map<String, Object> config);

    /**
     * Gets wrappers for all available plugins of a specific type.
     *
     * @param type The type of the plugins.
     * @return A list of {@link PluginWrapper}s.
     */
    List<PluginWrapper> getPlugins(@Nonnull String type);

    /**
     * Gets wrappers for all available plugins of a specific type, with a shared configuration.
     *
     * @param type The type of the plugins.
     * @param conf A map of configuration properties to apply to all plugins.
     * @return A list of {@link PluginWrapper}s.
     */
    List<PluginWrapper> getPlugins(@Nonnull String type, Map<String, Object> conf);

    /**
     * Gets wrappers for all available plugins of a specific interface, with a shared configuration.
     *
     * @param pluginInterface The interface class of the plugins.
     * @param conf A map of configuration properties to apply to all plugins.
     * @return A list of {@link PluginWrapper}s.
     */
    List<PluginWrapper> getPlugins(Class<? extends Sps4jPlugin> pluginInterface, Map<String, Object> conf);

    /**
     * Gets the raw, unwrapped instances of all available plugins for a specific interface, with a shared configuration.
     *
     * @param pluginInterface The plugin interface class to cast to.
     * @param conf A map of configuration properties to apply to all plugins.
     * @param <T> The type of the plugin interface.
     * @return A list of unwrapped plugin instances.
     */
    <T extends Sps4jPlugin> List<T> getPluginsUnwrapped(Class<T> pluginInterface, Map<String, Object> conf);

    /**
     * Loads all plugins of the given types using a shared class loader.
     * <p>
     * WARNING: This method should be used with caution. It ensures that any already-loaded plugins
     * are not reloaded and continue to use their original class loader. It is primarily for scenarios
     * where multiple plugin types need to share classes.
     *
     * @param first The first plugin type to load.
     * @param rest  Additional plugin types to load.
     * @return A list of {@link PluginWrapper}s for all loaded plugins of the specified types.
     */
    List<PluginWrapper> getPluginsSharingClassLoader(@Nonnull String first, String... rest);

    /**
     * Unloads and destroys a specific plugin instance.
     *
     * @param artifact The artifact of the plugin to unload.
     */
    void unload(@Nonnull PluginArtifact artifact);

    /**
     * Unloads and destroys all currently loaded plugins.
     */
    void unloadAll();

    /**
     * Unloads and destroys all plugins of a specific type.
     *
     * @param type The type of plugins to unload.
     */
    void unload(@Nonnull String type);

    /**
     * Unloads and destroys all plugins of a specific interface.
     *
     * @param pluginInterface The interface class of the plugins to unload.
     */
    void unload(@Nonnull Class<?> pluginInterface);
}
