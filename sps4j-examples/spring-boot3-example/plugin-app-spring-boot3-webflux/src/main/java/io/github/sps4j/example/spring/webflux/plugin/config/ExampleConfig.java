package io.github.sps4j.example.spring.webflux.plugin.config;

import io.github.sps4j.example.spring.webflux.plugin.service.TestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class ExampleConfig {

    @Bean
    public TestService testService() {
        return new TestService();
    }

    @Bean
    public RouterFunction<ServerResponse> hello() {
        return RouterFunctions.route()
                .path("/hello", builder -> builder
                        .GET("", request ->
                                ServerResponse.ok().bodyValue("This response comes from a controller inside the plugin!"))
                )
                .build();
    }
}

