package io.github.sps4j.springboot2.webflux;

import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxProperties;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorResourceFactory;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnExpression("T(java.lang.Thread).currentThread().contextClassLoader instanceof T(io.github.sps4j.core.load.Sps4jPluginClassLoader)")
@AutoConfigureBefore(ReactiveWebServerFactoryAutoConfiguration.class)
@AllArgsConstructor
public class Sps4jPluginWebfluxConfiguration {

    private final WebFluxProperties webFluxProperties;

    @Bean
    public ReactiveWebServerFactory nettyWebServerFactory() {
        return new Sps4jNettyWebServerFactory(webFluxProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactorResourceFactory reactorResourceFactory() {
        // This factory is created to prevent the default one that uses global resources.
        // The Sps4j architecture reuses the host's web server, so the plugin
        // does not need its own loop resources.
        // We return a factory that does not use global resources and does nothing on destroy.
        ReactorResourceFactory factory = new ReactorResourceFactory() {
            @Override
            public void destroy()  {
                // Do nothing. The resources are managed by the host.
            }
        };
        factory.setUseGlobalResources(false);
        return factory;
    }
}
