package com.github.sps4j.example.springmvc.plugin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PluginController {
    @GetMapping("/hello")
    public String hello() {
        return "This response comes from a controller inside the plugin!";
    }
}
