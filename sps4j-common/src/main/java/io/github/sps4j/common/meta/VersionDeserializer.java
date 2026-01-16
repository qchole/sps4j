package io.github.sps4j.common.meta;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.github.zafarkhaja.semver.Version;

import java.io.IOException;

/**
 * Custom Jackson deserializer for {@link com.github.zafarkhaja.semver.Version} objects.
 * It deserializes a string representation of a version into a {@link Version} object.
 *
 * @author Allan-QLB
 */
public class VersionDeserializer extends JsonDeserializer<Version> {
    /**
     * Deserializes a JSON string into a {@link Version} object.
     *
     * @param p The JSON parser.
     * @param ctxt The deserialization context.
     * @return The deserialized {@link Version} object.
     * @throws IOException If an I/O error occurs or the JSON token is not a string.
     */
    @Override
    public Version deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            return Version.parse(p.getText());
        }
        throw new JsonParseException(p, "Expected a string value");
    }
}
