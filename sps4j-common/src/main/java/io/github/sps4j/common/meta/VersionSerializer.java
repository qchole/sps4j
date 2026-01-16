package io.github.sps4j.common.meta;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.zafarkhaja.semver.Version;

import java.io.IOException;

/**
 * Custom Jackson serializer for {@link com.github.zafarkhaja.semver.Version} objects.
 * It serializes a {@link Version} object into its string representation.
 *
 * @author Allan-QLB
 */
public class VersionSerializer extends JsonSerializer<Version> {
    /**
     * Serializes a {@link Version} object into a JSON string.
     *
     * @param value The {@link Version} object to serialize.
     * @param gen The JSON generator.
     * @param serializers The serializer provider.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void serialize(Version value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
    }
}
