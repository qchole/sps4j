package com.github.sps4j.example.springmvc.host.controller;

import com.github.sps4j.common.meta.PluginArtifact;
import com.github.sps4j.core.PluginManager;
import com.github.sps4j.core.test.TestPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
public class HostController {
    @Autowired
    private PluginManager pluginManager;
    private TestPlugin testPlugin;

    @GetMapping("/hello")
    public String hello() {
        return "hello base " + Thread.currentThread().getContextClassLoader().toString();
    }

    @GetMapping("/load")
    public String load() {
        testPlugin = pluginManager.getPluginUnwrapped(TestPlugin.class,
                PluginArtifact.builder()
                .type("test").name("example").build(),
                Collections.emptyMap());
        return "load ok " + testPlugin.getClass().getClassLoader().toString();
    }

    @GetMapping("/reset")
    public String reset() {
        pluginManager.resetAll();
        testPlugin = null;
        return "reset ok";
    }

    @GetMapping("/test")
    public String test() {
        testPlugin.test();
        return "test ok ";
    }
}
