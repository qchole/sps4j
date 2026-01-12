package com.github.sps4j.example.api;


import com.github.sps4j.annotation.Sps4jPluginInterface;
import com.github.sps4j.core.Sps4jPlugin;

@Sps4jPluginInterface("greeter")
public interface GreeterPlugin extends Sps4jPlugin {
    String greet(String name);
}
