package io.github.sps4j.common.meta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.net.URL;

/**
 * Represents the metadata information of a plugin, combining its descriptor with its location URL.
 *
 * @author Allan-QLB
 */
@Getter
@Builder
@AllArgsConstructor
public class MetaInfo {
    /**
     * The descriptor of the plugin.
     */
    @Nonnull
    private final PluginDesc descriptor;
    /**
     * The URL pointing to the location of the plugin artifact (e.g., JAR file).
     */
    @Nonnull
    private final URL url;
}
