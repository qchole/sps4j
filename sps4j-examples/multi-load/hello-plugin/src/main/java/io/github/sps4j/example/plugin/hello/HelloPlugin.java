package io.github.sps4j.example.plugin.hello;

import io.github.sps4j.annotation.Sps4jPlugin;
import io.github.sps4j.example.api.GreeterPlugin;

@Sps4jPlugin(name = "hello")
public class HelloPlugin implements GreeterPlugin {
    @Override
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}
