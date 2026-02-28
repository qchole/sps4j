package io.github.sps4j.springboot2.config;

import io.github.sps4j.springboot2.web.Sps4jTomcatWebServerFactory;
import org.apache.catalina.startup.Tomcat;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("T(java.lang.Thread).currentThread().contextClassLoader instanceof T(io.github.sps4j.core.load.Sps4jPluginClassLoader)")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(Tomcat.class)
@AutoConfigureBefore({ServletWebServerFactoryAutoConfiguration.class})
public class TomcatAutoConfiguration {

    /**
     * Creates a {@link Sps4jTomcatWebServerFactory} bean.
     * <p>
     * This factory is responsible for creating a {@link org.springframework.boot.web.server.WebServer}
     * that delegates to the base application's Tomcat instance, allowing the plugin
     * to deploy its own servlets and filters within the same server.
     *
     * @return The custom {@link ServletWebServerFactory}.
     */
    @Bean
    public ServletWebServerFactory webServerFactory() {
        return new Sps4jTomcatWebServerFactory(Thread.currentThread().getContextClassLoader());
    }

}
