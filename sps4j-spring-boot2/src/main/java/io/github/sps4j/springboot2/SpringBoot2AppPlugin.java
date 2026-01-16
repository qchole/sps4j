package io.github.sps4j.springboot2;


import io.github.sps4j.common.meta.MetaInfo;
import io.github.sps4j.core.Sps4jPlugin;
import io.github.sps4j.core.load.Sps4jPluginClassLoader;
import io.github.sps4j.springboot2.context.PluginSpringbootBootstrapContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

/**
 * An abstract base class for plugins that are themselves Spring Boot applications.
 * <p>
 * This class handles the lifecycle of the embedded Spring Boot application,
 * starting it when the plugin is loaded and stopping it when the plugin is unloaded.
 * It configures the application context based on the plugin's metadata.
 *
 * @author Allan-QLB
 */
@Slf4j
@Getter
public abstract class SpringBoot2AppPlugin implements Sps4jPlugin {

    /**
     * A tag indicating that the plugin is a Spring Web MVC application.
     */
    public static final String TAG_SPRING_MVC = "spring-webmvc";
    /**
     * A regex pattern to identify Spring Boot's configuration files (application.yml, bootstrap.properties, etc.).
     * This is used to ensure these files are loaded from the plugin's classloader.
     */
    public static final String SPRING_CONFIG_FILE_NAME_PATTERN = "^(application|bootstrap)(-[a-zA-Z0-9_]+)?\\.(properties|ya?ml|xml)$";

    private ConfigurableApplicationContext applicationContext;

    /**
     * Starts the embedded Spring Boot application when the plugin is loaded.
     * <p>
     * It configures the parent-last class loading for Spring configuration files,
     * determines the {@link WebApplicationType} based on plugin tags, and starts the application.
     * It also registers a shutdown hook to stop the application context when the plugin's classloader is closed.
     *
     * @param conf a {@link Map} containing configuration for the plugin.
     * @param metadata the {@link MetaInfo} object containing metadata about the plugin.
     */
    @Override
    public void onLoad(Map<String, Object> conf, MetaInfo metadata) {
        Sps4jPluginClassLoader classLoader = (Sps4jPluginClassLoader) getClass().getClassLoader();
        classLoader.addIgnoreParentResourceNamePattern(SPRING_CONFIG_FILE_NAME_PATTERN);
        final SpringApplication springApplication = new SpringApplication(this.getClass());
        if (metadata.getDescriptor().getTags().contains(TAG_SPRING_MVC)) {
            springApplication.setWebApplicationType(WebApplicationType.SERVLET);
        } else {
            springApplication.setWebApplicationType(WebApplicationType.NONE);
        }
        PluginSpringbootBootstrapContext.setCurrentPluginMetaInfo(metadata);
        try {
            applicationContext = springApplication.run();
            ((Sps4jPluginClassLoader) this.getClass().getClassLoader()).addOnCloseAction(() -> {
                log.info("Stop plugin application context {}", applicationContext.getApplicationName());
                applicationContext.close();
            });
        } finally {
            PluginSpringbootBootstrapContext.removeCurrentPluginMetaInfo();
        }

    }

}



