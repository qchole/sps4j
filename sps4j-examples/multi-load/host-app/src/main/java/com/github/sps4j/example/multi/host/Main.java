package com.github.sps4j.example.multi.host;

import com.github.sps4j.core.DefaultPluginManager;
import com.github.sps4j.core.PluginManager;
import com.github.sps4j.core.load.DefaultPluginLoader;
import com.github.sps4j.core.load.ProductPluginLoadService;
import com.github.sps4j.example.api.GreeterPlugin;
import com.github.zafarkhaja.semver.Version;

import java.net.URL;
import java.util.Collections;
import java.util.List;

public class Main {

    private static final String DIR = "plugin";

    public static void main(String[] args) throws Exception {
        ProductPluginLoadService productService = () -> Version.parse("1.0.0");

        URL resource = Main.class.getClassLoader().getResource(DIR);
        if (resource == null) {
            throw new IllegalStateException("Plugin dir not found: " + DIR);
        }
        PluginManager pluginManager = new DefaultPluginManager(
                resource.toString(),
                productService,
                new DefaultPluginLoader()
        );

        // 使用 getPluginsUnwrapped 获取所有 "greeter" 类型的插件
        List<GreeterPlugin> allGreeters = pluginManager.getPluginsUnwrapped(
                GreeterPlugin.class,
                Collections.emptyMap()
        );

        // 遍历并调用所有插件
        System.out.println("Found " + allGreeters.size() + " greeter plugins.");
        for (GreeterPlugin plugin : allGreeters) {
            System.out.println(plugin.greet("World"));
        }
    }
}