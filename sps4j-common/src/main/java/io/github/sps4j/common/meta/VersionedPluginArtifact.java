package io.github.sps4j.common.meta;

import com.github.zafarkhaja.semver.Version;
import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nonnull;

/**
 * Represents a plugin artifact with a specific semantic version.
 *
 * @author Allan-QLB
 */
@Getter
@Builder
public class VersionedPluginArtifact {
    /**
     * The plugin artifact identifier (type and name).
     */
    @Nonnull
    private  final PluginArtifact artifact;
    /**
     * The semantic version of the plugin artifact.
     */
    @Nonnull
    private final Version version;

    @Override
    public String toString() {
        return artifact + ":" + version;
    }
}
