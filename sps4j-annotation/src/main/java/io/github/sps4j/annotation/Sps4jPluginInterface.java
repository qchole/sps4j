package io.github.sps4j.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface as a plugin interface.
 * <p>
 * Plugin implementations should implement an interface marked with this annotation.
 * The annotation processor uses this to identify plugin extension points.
 * All plugin interfaces must extend {@code io.github.sps4j.core.Sps4jPlugin}.
 *
 * @author Allan-QLB
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Sps4jPluginInterface {
    /**
     * The name of the plugin type. If not specified, the simple name of the interface will be used.
     * @return The plugin type name.
     */
    String value() default "";
}
