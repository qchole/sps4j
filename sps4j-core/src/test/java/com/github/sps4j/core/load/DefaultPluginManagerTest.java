package com.github.sps4j.core.load;

import com.github.sps4j.annotation.PluginProcessor;
import com.github.sps4j.common.meta.PluginArtifact;
import com.github.sps4j.core.DefaultPluginManager;
import com.github.sps4j.core.PluginManager;
import com.github.sps4j.core.test.TestPlugin;
import com.github.zafarkhaja.semver.Version;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultPluginManagerTest {

    @Test
    void testScan() {
        Map<String, String>
                stringStringMap = DefaultPluginManager.doScan();
        assertFalse(stringStringMap.isEmpty());
    }

    @Test
    void testLoad() {
        URL url = ClassLoader.getSystemClassLoader().getResource("plugins");
        assertNotNull(url);
        DefaultPluginManager pluginManager = new DefaultPluginManager(url.toString(), () -> Version.parse("0.0.1"));
        PluginWrapper test = pluginManager.getPlugin("test", "MyTest");
        assertNotNull(test.getPluginAs(TestPlugin.class).test());
        pluginManager.unload(TestPlugin.class);
    }

    @Test
    void testLoadUnwrapped() {
        URL url = ClassLoader.getSystemClassLoader().getResource("plugins");
        assertNotNull(url);
        DefaultPluginManager pluginManager = new DefaultPluginManager(url.toString(), () -> Version.parse("0.0.1"));
        TestPlugin test = pluginManager.getPluginUnwrapped(TestPlugin.class, "MyTest", Collections.emptyMap());
        assertNotNull(test.test());
        pluginManager.unload(PluginArtifact.builder()
                        .type("test")
                        .name("MyTest")
                .build());
    }

    @Test
    void testLoadAll() {
        URL url = ClassLoader.getSystemClassLoader().getResource("plugins");
        assertNotNull(url);
        DefaultPluginManager pluginManager = new DefaultPluginManager(url.toString(), () -> Version.parse("0.0.1"));
        List<PluginWrapper> test = pluginManager.getPlugins(TestPlugin.class, new HashMap<>());
        assertFalse(test.isEmpty());
        pluginManager.unload("test");
    }

    @Test
    void testLoadAllUnwrapped() {
        URL url = ClassLoader.getSystemClassLoader().getResource("plugins");
        assertNotNull(url);
        DefaultPluginManager pluginManager = new DefaultPluginManager(url.toString(), () -> Version.parse("0.0.1"));
        List<TestPlugin> test = pluginManager.getPluginsUnwrapped(TestPlugin.class, new HashMap<>());
        assertFalse(test.isEmpty());
        pluginManager.unloadAll();
    }


}