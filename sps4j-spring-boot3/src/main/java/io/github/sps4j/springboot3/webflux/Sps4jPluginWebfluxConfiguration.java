package io.github.sps4j.springboot3.webflux;

import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxProperties;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorResourceFactory;

@AutoConfiguration(before = ReactiveWebServerFactoryAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnExpression("T(java.lang.Thread).currentThread().contextClassLoader instanceof T(io.github.sps4j.core.load.Sps4jPluginClassLoader)")
@AllArgsConstructor
public class Sps4jPluginWebfluxConfiguration {

    private final WebFluxProperties webFluxProperties;
    private final ApplicationContext applicationContext;

    @Bean
    public ReactiveWebServerFactory nettyWebServerFactory() {
        return new Sps4jNettyWebServerFactory(webFluxProperties, applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactorResourceFactory reactorResourceFactory() {
        ReactorResourceFactory factory = new ReactorResourceFactory() {
            @Override
            public void destroy() {
            }
        };
        factory.setUseGlobalResources(false);
        return factory;
    }
}
