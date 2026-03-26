package io.github.sps4j.example.spring.webflux.plugin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/welcome")
public class WelcomeController {

    @GetMapping
    public Mono<String> welcome() {
        return Mono.just("welcome");
    }

}
