package io.github.sps4j.example.api;


import io.github.sps4j.annotation.Sps4jPluginInterface;
import io.github.sps4j.core.Sps4jPlugin;

@Sps4jPluginInterface("greeter")
public interface GreeterPlugin extends Sps4jPlugin {
    String greet(String name);
}
