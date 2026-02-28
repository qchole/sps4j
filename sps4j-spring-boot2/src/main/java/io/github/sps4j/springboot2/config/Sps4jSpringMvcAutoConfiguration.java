package io.github.sps4j.springboot2.config;

import io.github.sps4j.springboot2.web.AddPluginServletContextPathCustomizer;
import io.github.sps4j.springboot2.web.Sps4jTomcatWebServerFactory;
import org.apache.catalina.startup.Tomcat;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto configuration for SPS4J Spring MVC plugins.
 * <p>
 * This configuration is activated only when running inside an {@code Sps4jPluginClassLoader}
 * for a servlet web application. It provides a custom {@link ServletWebServerFactory}
 * to integrate the plugin's web server with the main application's Tomcat instance.
 *
 * @author Allan-QLB
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("T(java.lang.Thread).currentThread().contextClassLoader instanceof T(io.github.sps4j.core.load.Sps4jPluginClassLoader)")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@AutoConfigureBefore({ServletWebServerFactoryAutoConfiguration.class, JettyAutoConfiguration.class,
        TomcatAutoConfiguration.class})
@EnableConfigurationProperties(Sps4jSpringMvcProperties.class)
public class Sps4jSpringMvcAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "sps4j.spring-mvc", name = "add-servlet-context-prefix", havingValue = "true", matchIfMissing = true)
    public AddPluginServletContextPathCustomizer addPluginServletContextPathCustomizer(ServerProperties serverProperties) {
        return new AddPluginServletContextPathCustomizer(serverProperties);
    }

}
