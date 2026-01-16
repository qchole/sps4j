package io.github.sps4j.common.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Utility class for providing YAML mappers, specifically handling different mappers for different
 * {@link ClassLoader} contexts, especially for plugin class loaders.
 *
 * @author Allan-QLB
 */
@Slf4j
public class YamlUtils {
    private static final YAMLMapper YAML = createYamlMapper();
    private static final Map<ClassLoader, YAMLMapper> CLASS_LOADER_YAML_MAPPER_MAP = new WeakHashMap<>();
    private static Class<?> PLUGIN_CLASSLOADER_CLASS;

    static {
        try {
            PLUGIN_CLASSLOADER_CLASS =
                    Class.forName("io.github.sps4j.core.load.Sps4jPluginClassLoader");
        } catch (ClassNotFoundException e) {
            log.warn("Unable to load SPS4J plugin classloader", e);
            PLUGIN_CLASSLOADER_CLASS = null;
        }
    }

    /**
     * Returns a {@link YAMLMapper} instance. If the current thread's context class loader is an
     * {@code Sps4jPluginClassLoader}, a specific mapper instance for that class loader is returned (or created).
     * Otherwise, a shared default mapper is returned.
     *
     * @return A {@link YAMLMapper} instance suitable for the current class loading context.
     */
    public static YAMLMapper getYamlMapper() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (PLUGIN_CLASSLOADER_CLASS == null || !PLUGIN_CLASSLOADER_CLASS.isInstance(contextClassLoader)) {
            return YAML;
        }
        synchronized (CLASS_LOADER_YAML_MAPPER_MAP) {
            return CLASS_LOADER_YAML_MAPPER_MAP.computeIfAbsent(contextClassLoader,
                    cl -> createYamlMapper());
        }
    }

    /**
     * Creates and configures a new {@link YAMLMapper} instance.
     *
     * @return A new, configured {@link YAMLMapper}.
     */
    private static YAMLMapper createYamlMapper() {
        YAMLMapper mapper = new YAMLMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setConfig(mapper.getDeserializationConfig().without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        return mapper;
    }

}