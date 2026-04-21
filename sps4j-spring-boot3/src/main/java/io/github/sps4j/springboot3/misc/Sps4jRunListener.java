package io.github.sps4j.springboot3.misc;

import io.github.sps4j.common.meta.MetaInfo;
import io.github.sps4j.core.load.Sps4jPluginClassLoader;
import io.github.sps4j.springboot3.context.HostApplicationContextHolder;
import io.github.sps4j.springboot3.context.PluginSpringbootBootstrapContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class Sps4jRunListener implements SpringApplicationRunListener {


    private static final String SPRING_BOOT_PROP_ADMIN_JMX_NAME = "spring.application.admin.jmx-name";
    private static final String SPRING_BOOT_ADMIN_JMX_NAME_KEY = "org.springframework.boot:type=Admin,name=";
    private static final String SLASH = "/";
    public static final String PROPERTY_SOURCE_NAME_SPS4J_PLUGIN_OVERWRITE = "sps4j-plugin-overwrite";


    public Sps4jRunListener(SpringApplication ignored, String[] args) {
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        boolean host = !(Thread.currentThread().getContextClassLoader() instanceof Sps4jPluginClassLoader);
        if (host) {
            HostApplicationContextHolder.create(context);
            log.info("Application context prepared for host application");
        } else {
            MetaInfo metaInfo = PluginSpringbootBootstrapContext.getCurrentPluginMetaInfo();
            log.info("Application context prepared for plugin application");
            Map<String, Object> source = new HashMap<>();
            source.put(SPRING_BOOT_PROP_ADMIN_JMX_NAME,
                SPRING_BOOT_ADMIN_JMX_NAME_KEY
                    + metaInfo.getDescriptor().getType() + SLASH + metaInfo.getDescriptor().getName());
            context.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME_SPS4J_PLUGIN_OVERWRITE, source));
        }
    }
}
