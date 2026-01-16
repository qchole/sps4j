package io.github.sps4j.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A key-value pair attribute for a plugin.
 * Can be used to store arbitrary metadata for a plugin.
 *
 * @author Allan-QLB
 */
@Retention(RetentionPolicy.SOURCE)
public @interface Attribute {
    /**
     * The name of the attribute.
     *
     * @return The name of the attribute.
     */
    String name();

    /**
     * The value of the attribute.
     *
     * @return The value of the attribute.
     */
    String value();
}

