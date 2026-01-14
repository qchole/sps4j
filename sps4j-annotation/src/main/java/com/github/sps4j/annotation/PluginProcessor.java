package com.github.sps4j.annotation;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.sps4j.common.Const;
import com.github.sps4j.common.meta.PluginDesc;
import com.github.sps4j.common.utils.YamlUtils;
import com.github.zafarkhaja.semver.Version;
import com.github.zafarkhaja.semver.expr.ExpressionParser;
import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * Annotation processor for {@link Sps4jPlugin}.
 * <p>
 * This processor scans for types annotated with {@link Sps4jPlugin}, validates them,
 * and generates a {@code META-INF/sps4j/plugin-desc.yml} file containing the metadata for each plugin.
 *
 * @author Allan-QLB
 * @see Sps4jPlugin
 */
@AutoService(Processor.class)
@SupportedOptions({"pluginVersion", "pluginArtifact"})
public class PluginProcessor extends AbstractProcessor {
    private static final String ERROR_MSG_FORMAT = "@Plugin annotation error on class '%s', parameter '%s', msg: %s";
    private String projectVersion = null;
    private int round = 0;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        projectVersion = processingEnv.getOptions().get("pluginVersion");
    }

    private static void printErrorMessage(Messager messager, String clazz, String param, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR,String.format(ERROR_MSG_FORMAT, clazz, param, message));
    }



    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        round ++;
        if (round > 1) {
            return false;
        }
        List<AnnotatedPluginType> annotatedPluginTypes = new ArrayList<>();
        final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Sps4jPlugin.class);
        for (Element el : elements) {
            annotatedPluginTypes.add(
                    new AnnotatedPluginType(el.getAnnotation(Sps4jPlugin.class), (TypeElement) el)
                    );
        }
        List<PluginDesc> descs = new ArrayList<>();
        final Messager messager = processingEnv.getMessager();
        boolean hasErrors = false;
        loopAnnotatedPluginTypes:
        for (AnnotatedPluginType annotatedPluginType : annotatedPluginTypes) {
            TypeElement element =  annotatedPluginType.getTypeElement();
            final String className = element.getQualifiedName().toString();
            final Sps4jPlugin annotation = annotatedPluginType.getAnnotation();
            if (element.getModifiers().contains(Modifier.ABSTRACT)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "@Plugin on abstract type " + className);
                hasErrors = true;
                break;
            }
            final Pair<String, String> pluginTypeWithIfName;
            try {
                pluginTypeWithIfName = detectPluginInterfaceType(element);
            } catch (Exception e) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Plugin class " + className + " interface error: " + e.getMessage());
                hasErrors = true;
                break;
            }
            final String name = annotation.name();
            final String versionStr = StringUtils.isNotBlank(annotation.version()) ? annotation.version() : projectVersion;
            String versionSupport = annotation.productVersionConstraint();

            final Attribute[] attributes = annotation.attributes();
            if (!notBlank(className, name, PluginDesc.Fields.name, messager)
                    || !notBlank(className, versionStr, PluginDesc.Fields.version, messager)
                    || !notBlank(className, versionSupport, PluginDesc.Fields.productVersionConstraint, messager)
            ) {
                hasErrors = true;
                break;
            }
            Optional<Version> versionOpt = checkAndGetVersion(versionStr);
            if (!versionOpt.isPresent()) {
                printErrorMessage(messager, className, PluginDesc.Fields.version, "version format error");
                hasErrors = true;
                break;
            }

            if (!isValidVersionConstraint(versionSupport)) {
                printErrorMessage(messager, className, PluginDesc.Fields.productVersionConstraint, "not valid");
                hasErrors = true;
                break;
            }

            Map<String, String> attributeMap = new HashMap<>();
            for (Attribute attribute : attributes) {
                if (attributeMap.containsKey(attribute.name())) {
                    printErrorMessage(messager, className, PluginDesc.Fields.attributes, String.format("Attribute conflict '%s'", attribute.name()));
                    hasErrors = true;
                    break loopAnnotatedPluginTypes;
                } else {
                    attributeMap.put(attribute.name(), attribute.value());
                }
            }

            final PluginDesc desc = PluginDesc
                    .builder()
                    .type(pluginTypeWithIfName.getKey())
                    .name(name)
                    .version(versionOpt.get())
                    .description(annotation.description())
                    .className(className)
                    .displayName(annotation.displayName())
                    .productVersionConstraint(versionSupport)
                    .tags(Arrays.asList(annotation.tags()))
                    .attributes(attributeMap)
                    .build();
            descs.add(desc);
        }


        if (!descs.isEmpty() && !hasErrors) {
            try {
                genFile(descs);
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "write spec file error: " + e.getMessage());
            }
        }

        return false;
    }

    private void genFile(List<PluginDesc> descriptors) throws IOException {
        final boolean singlePluginPackage = descriptors.size() == 1;
        final ObjectWriter writer = singlePluginPackage ? YamlUtils.getYamlMapper().writerFor(PluginDesc.class) : YamlUtils.getYamlMapper().writerFor(new TypeReference<List<PluginDesc>>() {});
        final Filer filer = processingEnv.getFiler();
        final FileObject fileObject = filer.createResource(StandardLocation.CLASS_OUTPUT, "", Const.DESC_FILE);
        try (OutputStream stream = fileObject.openOutputStream()) {
            if (singlePluginPackage) {
                writer.writeValue(stream, descriptors.get(0));
            } else {
                writer.writeValue(stream, descriptors);
            }
        }
    }

    private static boolean notBlank(String clazz, String value, String fieldName, Messager messager) {
        final boolean blank = StringUtils.isBlank(value);
        if (blank) {
            printErrorMessage(messager, clazz, fieldName, "Parameter is blank");
        }
        return !blank;
    }

    /**
     * Detects the plugin interface type for a given plugin implementation class.
     * <p>
     * It traverses the class hierarchy and its interfaces to find an interface that is
     * annotated with {@link Sps4jPluginInterface}.
     * It also ensures that the plugin class implements {@link com.github.sps4j.core.Sps4jPlugin}.
     *
     * @param element The {@link TypeElement} of the plugin implementation class.
     * @return A {@link Pair} containing the plugin type name and the fully qualified name of the plugin interface.
     * @throws IllegalStateException if the plugin interface cannot be detected, if there are multiple plugin interfaces,
     *                               or if the plugin does not implement {@link com.github.sps4j.core.Sps4jPlugin}.
     */
    @VisibleForTesting
     Pair<String, String> detectPluginInterfaceType(@Nonnull TypeElement element) {
        List<TypeElement> classCandidates = new ArrayList<>();
        TypeElement current = element;
        while (current!= null && !current.getQualifiedName().toString().startsWith("java")
                && !current.getQualifiedName().toString().startsWith("sun")) {
            classCandidates.add(current);
            final TypeMirror superclass = current.getSuperclass();
            if (superclass != null) {
                current = (TypeElement) processingEnv.getTypeUtils().asElement(superclass);
            } else {
                current = null;
            }
        }
        Map<String, TypeElement> allInterfaces = new HashMap<>();
        classCandidates.forEach(c -> allInterfaces.putAll(findAllInterfacesOfType(c)));
        if (!allInterfaces.containsKey(InterfaceAnnotationProcessor.PLUGIN_BASE_INTERFACE)) {
            throw new IllegalStateException("Plugin must implements "
                    + InterfaceAnnotationProcessor.PLUGIN_BASE_INTERFACE);
        }
        return Optional.ofNullable(findPluginInterfaceFrom(allInterfaces))
                .orElseThrow(() -> new IllegalStateException("can not detect plugin type"));

    }

    /**
     * Finds the plugin interface from a map of interfaces.
     *
     * @param interfaces A map of interface names to their {@link TypeElement}s.
     * @return A {@link Pair} containing the plugin type and the interface name, or {@code null} if not found.
     * @throws IllegalStateException if multiple plugin interfaces are found.
     */
    @VisibleForTesting
    static Pair<String, String> findPluginInterfaceFrom(Map<String, TypeElement> interfaces) {
        Pair<String, String> found = null;
        for (Map.Entry<String, TypeElement> ent : interfaces.entrySet()) {
            final List<? extends AnnotationMirror> annotationMirrors = ent.getValue().getAnnotationMirrors();
            if (CollectionUtils.isNotEmpty(annotationMirrors)) {
                for (AnnotationMirror annotation : annotationMirrors) {
                    final DeclaredType annotationType = annotation.getAnnotationType();
                    final String name = annotationType.asElement().asType().toString();
                    if (Sps4jPluginInterface.class.getCanonicalName().equals(name)) {
                        final Map<? extends ExecutableElement, ? extends AnnotationValue> values = annotation.getElementValues();
                        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
                            if ("value".equals(entry.getKey().getSimpleName().toString())) {
                                String pluginType = StringUtils.isBlank((String) entry.getValue().getValue()) ? ent.getKey() : (String) entry.getValue().getValue();
                                if (found != null) {
                                    throw new IllegalStateException("Plugin type " + pluginType + "conflict with" + found.getKey());
                                }
                                found = Pair.of(pluginType, ent.getKey());
                            }
                        }
                    }
                }
            }
        }
        return found;

    }

    /**
     * Recursively finds all interfaces implemented by a given {@link TypeElement}.
     *
     * @param typeElement The {@link TypeElement} to analyze.
     * @return A map of fully qualified interface names to their corresponding {@link TypeElement}s.
     */
    @VisibleForTesting
    Map<String, TypeElement> findAllInterfacesOfType(TypeElement typeElement) {
        Map<String, TypeElement> all = new HashMap<>();
        final List<? extends TypeMirror> interfaces = typeElement.getInterfaces();
        for (TypeMirror i : interfaces) {
            final String ifName = i.toString();
            if (!all.containsKey(ifName)) {
                TypeElement ifTe = (TypeElement) processingEnv.getTypeUtils().asElement(i);
                all.putAll(findAllInterfacesOfType(ifTe));
                all.put(ifName, ifTe);
            }
        }
        return all;

    }


    /**
     * Validates if a string is a valid semantic versioning expression.
     *
     * @param expr The expression to validate.
     * @return {@code true} if the expression is valid, {@code false} otherwise.
     */
    @VisibleForTesting
     static boolean isValidVersionConstraint(String expr) {
        try {
            ExpressionParser.newInstance().parse(expr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parses a version string into a {@link Version} object.
     *
     * @param version The version string.
     * @return An {@link Optional} containing the {@link Version} object if parsing is successful, otherwise an empty {@link Optional}.
     */
    private static Optional<Version> checkAndGetVersion(String version) {
        try {
            return Optional.of(Version.parse(version));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Sps4jPlugin.class.getCanonicalName());
    }
}
