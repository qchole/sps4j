package com.github.sps4j.springboot2.misc;

import com.github.sps4j.core.load.Sps4jPluginClassLoader;
import com.github.sps4j.springboot2.context.HostApplicationContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * A {@link SpringApplicationRunListener} for SPS4J.
 * <p>
 * This listener distinguishes between the base application and a plugin application.
 * For plugin applications, it disables the registration of the shutdown hook, as the plugin's lifecycle
 * is managed by the SPS4J {@link com.github.sps4j.core.PluginManager}.</li>
 * </ul>
 *
 * @author Allan-QLB
 */
@Slf4j
public class Sps4jRunListener implements SpringApplicationRunListener {

    private final SpringApplication application;

    public Sps4jRunListener(SpringApplication application, String[] args) {
        this.application = application;
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        boolean baseApp = !(Thread.currentThread().getContextClassLoader() instanceof Sps4jPluginClassLoader);
        if (baseApp) {
            HostApplicationContextHolder.create((ServletWebServerApplicationContext) context);
            log.info("Application context prepared for base application");
        } else {
            log.info("Application context prepared for plugin application, not register shutdown hook");
            application.setRegisterShutdownHook(false);
        }
    }
}