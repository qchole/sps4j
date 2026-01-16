package io.github.sps4j.example.springmvc.host.controller;

import io.github.sps4j.common.meta.PluginArtifact;
import io.github.sps4j.core.PluginManager;
import io.github.sps4j.example.api.GreeterPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
public class HostController {
    @Autowired
    private PluginManager pluginManager;
    private GreeterPlugin greeter;

    @GetMapping("/hello")
    public String hello() {
        return "hello host " + Thread.currentThread().getContextClassLoader().toString();
    }

    @GetMapping("/load")
    public String load() {
        greeter = pluginManager.getPluginUnwrapped(GreeterPlugin.class,
                PluginArtifact.builder()
                .type("greeter").name("spring-hello").build(),
                Collections.emptyMap());
        return "load ok " + greeter.getClass().getClassLoader().toString();
    }

    @GetMapping("/reset")
    public String reset() {
        pluginManager.resetAll();
        greeter = null;
        return "reset ok";
    }

    @GetMapping("/greet/{msg}")
    public String greet(@PathVariable String msg) {
        return greeter.greet(msg);
    }
}
