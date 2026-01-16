package io.github.sps4j.core.load;

import io.github.sps4j.common.meta.MetaInfo;
import io.github.sps4j.core.Sps4jPlugin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * A wrapper class that holds a loaded plugin instance along with its metadata.
 *
 * @author Allan-QLB
 */
@Getter
@Builder
@AllArgsConstructor
public class PluginWrapper {
    /**
     * The metadata of the plugin.
     */
    private final MetaInfo metaInfo;
    /**
     * The loaded plugin instance.
     */
    private final Sps4jPlugin plugin;

    /**
     * Casts the wrapped plugin to a specific plugin interface type.
     *
     * @param clazz The class of the plugin interface to cast to.
     * @param <T> The type of the plugin interface.
     * @return The plugin instance cast to the specified type.
     */
    @SuppressWarnings("unchecked")
    public <T extends Sps4jPlugin> T getPluginAs(Class<T> clazz) {
        return (T) plugin;
    }
}
