package io.github.sps4j.example.webflux.host.service;

import io.github.sps4j.common.meta.PluginArtifact;
import io.github.sps4j.core.PluginManager;
import io.github.sps4j.example.api.GreeterPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class PluginLoadService {

    @Autowired
    private PluginManager pluginManager;
    private GreeterPlugin greeter;

    public String load() {

        greeter = pluginManager.getPluginUnwrapped(GreeterPlugin.class,
                PluginArtifact.builder()
                        .type("greeter").name("spring-hello").build(),
                Collections.emptyMap());

        return "load ok " + greeter.getClass().getClassLoader().toString();
    }

    public String reset() {
        pluginManager.resetAll();
        greeter = null;
        return "reset ok";
    }
}
