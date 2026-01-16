package io.github.sps4j.springboot2.loader;


import io.github.sps4j.common.meta.MetaInfo;
import io.github.sps4j.common.utils.CallUtils;
import io.github.sps4j.core.Sps4jPlugin;
import io.github.sps4j.core.load.DefaultPluginLoader;
import io.github.sps4j.springboot2.SpringBoot2AppPlugin;
import io.github.sps4j.springboot2.context.HostApplicationContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ClassUtils;

import javax.annotation.Nonnull;
import java.beans.Introspector;

/**
 * A custom {@link DefaultPluginLoader} with added support for Spring applications.
 * <p>
 * This loader enhances the default behavior by:
 * 1. Autowiring dependencies from the base application into regular plugin instances.
 * 2. For {@link SpringBoot2AppPlugin} instances, it retrieves the actual plugin bean from the plugin's
 *    own application context, allowing it to be a fully managed Spring bean.
 *
 * @author Allan-QLB
 */
@Slf4j
public class SpringAppSupportPluginLoader extends DefaultPluginLoader {


    /**
     * Hook called after a plugin instance is created.
     * <p>
     * If the plugin is not a full-fledged {@link SpringBoot2AppPlugin}, it attempts to autowire
     * dependencies from the base application context into the plugin instance.
     *
     * @param pluginInstance The newly created plugin instance.
     * @param metadata       The metadata of the plugin.
     * @return The plugin instance, possibly with dependencies injected.
     */
    @Override
    public Sps4jPlugin pluginCreated(@Nonnull Sps4jPlugin pluginInstance, @Nonnull MetaInfo metadata) {
        if (!(pluginInstance instanceof SpringBoot2AppPlugin)) {
            try {
                CallUtils.runWithContextLoader(pluginInstance.getClass().getClassLoader(), () -> HostApplicationContextHolder.autowireFromHost(pluginInstance));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return pluginInstance;
    }

    /**
     * Hook called after the plugin is fully loaded.
     * <p>
     * If the plugin is a {@link SpringBoot2AppPlugin}, this method replaces the initial plugin instance
     * with the actual bean instance retrieved from the plugin's own Spring application context.
     * This ensures the returned object is the fully initialized Spring bean.
     *
     * @param pluginInstance The loaded plugin instance (which is the application class itself).
     * @param metadata       The metadata of the plugin.
     * @return The actual plugin bean from the Spring context, or the original instance if not a Spring app plugin.
     */
    @Override
    public Sps4jPlugin postLoadPlugin(@Nonnull Sps4jPlugin pluginInstance, @Nonnull MetaInfo metadata) {
        if (pluginInstance instanceof SpringBoot2AppPlugin) {
            final String shortName = ClassUtils.getShortName(metadata.getDescriptor().getClassName());
            return (Sps4jPlugin) ((SpringBoot2AppPlugin) pluginInstance).getApplicationContext()
                    .getBean(Introspector.decapitalize(shortName));
        }
        return pluginInstance;
    }
}

