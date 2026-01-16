package io.github.sps4j.example.springmvc.plugin.config;

import io.github.sps4j.example.springmvc.plugin.service.TestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExampleConfig {

    @Bean
    public TestService testService() {
        return new TestService();
    }
}

