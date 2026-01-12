package com.github.sps4j.example.plugin.hello;

import com.github.sps4j.annotation.Sps4jPlugin;
import com.github.sps4j.example.api.GreeterPlugin;

@Sps4jPlugin(name = "hello", version = "1.0.0", productVersionConstraint = ">=1.0")
public class HelloPlugin implements GreeterPlugin {
    @Override
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}
