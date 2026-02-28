package io.github.sps4j.springboot2.config;

import io.github.sps4j.springboot2.web.jetty.Sps4jJettyWebServerFactory;
import org.eclipse.jetty.server.Server;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(Server.class)
@AutoConfigureBefore(ServletWebServerFactoryAutoConfiguration.class)
public class JettyAutoConfiguration {

    @Bean
    public ServletWebServerFactory jettyWebServerFactory() {
        return new Sps4jJettyWebServerFactory();
    }

}
