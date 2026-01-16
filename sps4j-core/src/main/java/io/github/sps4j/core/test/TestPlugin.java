package io.github.sps4j.core.test;

import io.github.sps4j.annotation.Sps4jPluginInterface;
import io.github.sps4j.core.Sps4jPlugin;

@Sps4jPluginInterface("test")
public interface TestPlugin extends Sps4jPlugin {
    String test();
}
