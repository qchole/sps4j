package io.github.sps4j.annotation;



import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a plugin implementation.
 * <p>
 * The annotation processor will generate a {@code META-INF/sps4j/plugin-desc.yml} file for each class annotated with this.
 * The annotated class must implement an interface that is annotated with {@link Sps4jPluginInterface}.
 *
 * @author Allan-QLB
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Sps4jPlugin {
    /**
     * The unique name of the plugin.
     * @return The plugin name.
     */
    String name();

    /**
     * The version of the plugin, in semantic versioning format (e.g., "1.0.0").
     * If not specified, the project version will be used.
     * @return The plugin version.
     */
    String version() default "";

    /**
     * A short description of the plugin.
     * @return The plugin description.
     */
    String description() default "";

    /**
     * The display name of the plugin.
     * @return The plugin display name.
     */
    String displayName() default "";

    /**
     * The version constraint for the product that this plugin is compatible with.
     * This should be a semantic versioning expression (e.g., ">=2.0.0 &amp; &lt;3.0.0").
     * @return The product version constraint.
     */
    String productVersionConstraint() default "*";

    /**
     * Tags for categorizing the plugin.
     * @return An array of tags.
     */
    String[] tags() default {};

    /**
     * Custom attributes for the plugin.
     * @return An array of {@link Attribute}s.
     */
    Attribute[] attributes() default {};

}
