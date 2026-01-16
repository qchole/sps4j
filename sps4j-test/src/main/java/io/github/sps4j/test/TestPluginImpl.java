package io.github.sps4j.test;

import cn.hutool.Hutool;
import io.github.sps4j.annotation.Attribute;
import io.github.sps4j.annotation.Sps4jPlugin;
import io.github.sps4j.common.meta.MetaInfo;
import io.github.sps4j.core.test.TestPlugin;

import java.util.Map;

@Sps4jPlugin(name = "MyTest", productVersionConstraint = ">=0.0.1", attributes = {
        @Attribute(name = "a", value = "x"),
        @Attribute(name= "b", value = "y")
})
public class TestPluginImpl implements TestPlugin {
    static {
        Hutool.printAllUtils();
    }

    @Override
    public void onLoad(Map<String, Object> conf, MetaInfo metaInfo) {
        System.out.println("onLoad");
    }

    @Override
    public String test() {
        return "hello My test plugin";
    }

    @Override
    public void onDestroy() {
        System.out.println("onDestroy");
    }
}
