package io.github.sps4j.example.plugin.bye;

import io.github.sps4j.example.api.GreeterPlugin;

import io.github.sps4j.annotation.Sps4jPlugin;

@Sps4jPlugin(name = "bye", version = "1.0.0", productVersionConstraint = ">=1.0")
public class ByePlugin implements GreeterPlugin {
    @Override
    public String greet(String name) {
        return "Bye, " + name + "!";
    }
}