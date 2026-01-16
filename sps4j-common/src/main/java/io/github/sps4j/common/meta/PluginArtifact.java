package io.github.sps4j.common.meta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nonnull;

/**
 * Represents a unique identifier for a plugin artifact, composed of its type and name.
 *
 * @author Allan-QLB
 */
@Getter
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class PluginArtifact {
    /**
     * The type of the plugin.
     */
    @Nonnull
    private final String type;
    /**
     * The name of the plugin.
     */
    @Nonnull
    private final String name;

    @Override
    public String toString() {
        return type + ":" + name;
    }
}