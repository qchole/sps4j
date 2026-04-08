package io.github.sps4j.example.webflux.host.route;


import io.github.sps4j.example.webflux.host.service.PluginLoadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;


@Configuration
public class HostRouter {


    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public RouterFunction<ServerResponse> hello() {
        return RouterFunctions.route()
                .path("/hello", builder -> builder
                        .GET("", request ->
                                     ServerResponse.ok().bodyValue("hello"))
                )
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> load() {
        return RouterFunctions.route()
                .path("/load", builder -> builder
                        .GET("", request ->
                                    ServerResponse.ok().bodyValue(
                                            applicationContext.getBean(PluginLoadService.class).load()
                                    )
                                )
                )
                .path("/reset", builder -> builder
                        .GET("", request ->
                                ServerResponse.ok().bodyValue(
                                        applicationContext.getBean(PluginLoadService.class).reset()
                                )
                        )
                )
                .build();
    }

}

