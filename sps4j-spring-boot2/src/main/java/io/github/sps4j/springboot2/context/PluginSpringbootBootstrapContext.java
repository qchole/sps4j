package io.github.sps4j.springboot2.context;

import io.github.sps4j.common.meta.MetaInfo;
import org.springframework.core.NamedThreadLocal;

import javax.annotation.Nonnull;

public class PluginSpringbootBootstrapContext {
    private static ThreadLocal<MetaInfo> PLUGIN_META_INFO = new  NamedThreadLocal<>("Current Plugin MetaInfo");

    @Nonnull
    public static MetaInfo getCurrentPluginMetaInfo() {
        return PLUGIN_META_INFO.get();
    }

    public static void setCurrentPluginMetaInfo(MetaInfo metaInfo) {
        PLUGIN_META_INFO.set(metaInfo);
    }

    public static void removeCurrentPluginMetaInfo() {
        PLUGIN_META_INFO.remove();
    }

}
