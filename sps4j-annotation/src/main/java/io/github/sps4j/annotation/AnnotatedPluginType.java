package io.github.sps4j.annotation;


import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.lang.model.element.TypeElement;

/**
 * A container for a {@link TypeElement} that is annotated with {@link Sps4jPlugin}.
 *
 * @author Allan-QLB
 */
@Getter
@AllArgsConstructor
public class AnnotatedPluginType {
    /**
     * The {@link Sps4jPlugin} annotation instance.
     */
    private Sps4jPlugin annotation;
    /**
     * The annotated type element.
     */
    private TypeElement typeElement;
}

