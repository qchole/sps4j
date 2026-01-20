package io.github.sps4j.common.meta;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.zafarkhaja.semver.Version;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;

import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Represents the descriptor for a plugin, containing all its metadata.
 * This class is used for serialization and deserialization of plugin information.
 *
 * @author Allan-QLB
 */
@Getter
@Builder
@FieldNameConstants
public class PluginDesc {
    /**
     * The type of the plugin.
     */
    private final String type;
    /**
     * The unique name of the plugin.
     */
    private final String name;
    /**
     * The version of the plugin, following semantic versioning.
     */
    @JsonDeserialize(using = VersionDeserializer.class)
    @JsonSerialize(using = VersionSerializer.class)
    private final Version version;
    /**
     * A brief description of the plugin's functionality.
     */
    private final String description;
    /**
     * The fully qualified class name of the main plugin implementation.
     */
    private final String className;
    /**
     * A human-readable display name for the plugin.
     */
    private final String displayName;
    /**
     * The semantic version constraint for the product with which this plugin is compatible.
     */
    private final String productVersionConstraint;

    /**
     * A list of tags for categorizing and searching the plugin.
     */
    private List<String> tags;
    /**
     * A map of custom attributes for the plugin.
     */
    private Map<String, String> attributes;

    /**
     * Constructs a new PluginDesc.
     *
     * @param type The type of the plugin.
     * @param name The unique name of the plugin.
     * @param version The version of the plugin.
     * @param description A brief description of the plugin.
     * @param className The fully qualified class name of the plugin.
     * @param displayName A human-readable display name for the plugin.
     * @param productVersionConstraint The version constraint for product compatibility.
     * @param tags A list of tags for the plugin.
     * @param attributes A map of custom attributes for the plugin.
     */
    @JsonCreator
    public PluginDesc(@JsonProperty(Fields.type) String type,
                            @JsonProperty(Fields.name) String name,
                            @JsonProperty(Fields.version) @JsonDeserialize(using = VersionDeserializer.class) Version version,
                            @JsonProperty(Fields.description) String description,
                            @JsonProperty(Fields.className) String className,
                            @JsonProperty(Fields.displayName) String displayName,
                            @JsonProperty(Fields.productVersionConstraint) String productVersionConstraint,
                            @JsonProperty(Fields.tags) List<String> tags,
                            @JsonProperty(Fields.attributes) Map<String, String> attributes
    ) {
        this.type = type;
        this.name = name;
        this.version =version;
        this.description = description;
        this.className = className;
        this.displayName = displayName;
        this.productVersionConstraint = productVersionConstraint;
        this.tags = tags;
        this.attributes = attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginDesc that = (PluginDesc) o;
        return Objects.equals(type, that.type)
                && Objects.equals(name, that.name)
                && Objects.equals(version, that.version)
                && Objects.equals(productVersionConstraint, that.productVersionConstraint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, version, productVersionConstraint);
    }

    public PluginArtifact toArtifact() {
        return PluginArtifact.builder().type(type).name(name).build();
    }
}

