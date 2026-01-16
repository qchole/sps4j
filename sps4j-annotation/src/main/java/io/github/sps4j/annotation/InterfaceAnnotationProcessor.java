package io.github.sps4j.annotation;


import com.fasterxml.jackson.core.type.TypeReference;
import io.github.sps4j.common.Const;
import io.github.sps4j.common.utils.YamlUtils;
import com.google.auto.service.AutoService;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.util.*;

/**
 * Annotation processor for {@link Sps4jPluginInterface}.
 * <p>
 * This processor collects all interfaces annotated with {@link Sps4jPluginInterface} and generates a file
 * {@code META-INF/sps4j/interfaces} that contains a map of plugin interface names to their fully qualified class names.
 * The plugin interface name is either the value of the annotation or the simple name of the interface.
 *
 * @author Allan-QLB
 * @see Sps4jPluginInterface
 */
@AutoService(Processor.class)
public class InterfaceAnnotationProcessor extends AbstractProcessor {
    static final String PLUGIN_BASE_INTERFACE = "io.github.sps4j.core.Sps4jPlugin";

    private final Map<String, String> providers = new HashMap<>();

    @SneakyThrows
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver() && !providers.isEmpty()) {
            FileObject resource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT,""
                    , Const.INTERFACE_FILE);
            YamlUtils.getYamlMapper().writerFor(new TypeReference<Map<String, String>>() {
            }).writeValue(resource.openWriter(), providers);
            return false;
        }

        final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Sps4jPluginInterface.class);
        if (!elements.isEmpty()) {
            for (Element element : elements) {
                TypeElement typeElement = (TypeElement) element;
                final Set<String> ifNames = interfaces(typeElement);
                if (!ifNames.contains(PLUGIN_BASE_INTERFACE)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "class " + typeElement.getQualifiedName().toString() +
                                    " with @PluginInterface must extends " +  PLUGIN_BASE_INTERFACE);
                }
                Sps4jPluginInterface annotation = element.getAnnotation(Sps4jPluginInterface.class);
                //todo fail on dup
                providers.put(StringUtils.isNotBlank(annotation.value()) ? annotation.value() : element.getSimpleName().toString(),
                        typeElement.getQualifiedName().toString());
            }
        }
        return false;
    }

    /**
     * Recursively finds all interfaces implemented by a given {@link TypeElement}.
     *
     * @param e The {@link TypeElement} to analyze.
     * @return A set of fully qualified interface names.
     */
    public Set<String> interfaces(TypeElement e) {
        Set<String> result = new HashSet<>();
        final List<? extends TypeMirror> interfaces = e.getInterfaces();
        for (TypeMirror anInterface : interfaces) {
            final String ifName = anInterface.toString();
            final TypeElement element = (TypeElement) processingEnv.getTypeUtils().asElement(anInterface);
            result.addAll(interfaces(element));
            result.add(ifName);
        }
        return result;

    }


    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Sps4jPluginInterface.class.getCanonicalName());
    }
}

