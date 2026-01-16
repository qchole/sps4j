package io.github.sps4j.core.load;


import io.github.sps4j.common.meta.MetaInfo;
import io.github.sps4j.common.utils.CallUtils;
import io.github.sps4j.core.Sps4jPlugin;
import io.github.sps4j.core.exception.PluginException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.pool.TypePool;

import java.net.URL;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;


/**
 * The default implementation of {@link Sps4jPluginLoader}.
 * <p>
 * This loader creates a plugin instance and wraps it in a ByteBuddy proxy.
 * The proxy intercepts method calls to set the correct class loader context,
 * ensuring that the plugin operates within its own isolated environment.
 *
 * @author Allan-QLB
 */
@Getter
@Slf4j
public class DefaultPluginLoader implements Sps4jPluginLoader {

    /**
     * Loads a plugin based on its metadata. It creates a plugin instance,
     * calls the {@link #pluginCreated(Sps4jPlugin, MetaInfo)} hook,
     * invokes the plugin's {@link Sps4jPlugin#onLoad(Map, MetaInfo)} lifecycle method,
     * and finally calls the {@link #postLoadPlugin(Sps4jPlugin, MetaInfo)} hook.
     *
     * @param pluginMetadata The metadata of the plugin to load.
     * @param cl The class loader to use. If null, a new one will be created from the plugin's URL.
     * @param conf The configuration for the plugin.
     * @return The fully loaded and initialized plugin instance.
     * @throws PluginException if an error occurs during loading.
     */
    @Override
    public Sps4jPlugin load(MetaInfo pluginMetadata, Sps4jPluginClassLoader cl, Map<String, Object> conf) {
        try {
            final Sps4jPluginClassLoader classLoader;
            if (cl != null) {
                classLoader = cl;
            } else {
                classLoader = new Sps4jPluginClassLoader(new URL[]{pluginMetadata.getUrl()}, Sps4jPlugin.class.getClassLoader());
            }
            Sps4jPlugin pluginInstance = createPluginInstance(pluginMetadata.getDescriptor().getClassName(), classLoader);
            pluginInstance = pluginCreated(pluginInstance, pluginMetadata);
            pluginInstance.onLoad(conf, pluginMetadata);
            return postLoadPlugin(pluginInstance, pluginMetadata);
        } catch (Exception e) {
            throw new PluginException("Error load sps4j plugin class " + pluginMetadata.getDescriptor().getClassName() + " from " +
                    pluginMetadata.getUrl(), e);
        }
    }


    /**
     * Creates a proxied instance of the plugin class.
     * It uses ByteBuddy to rebase the plugin class, implement the {@link Sps4jProxy} marker interface,
     * and intercept all public, non-static methods with a {@link PluginMethodInvocationInterceptor}.
     *
     * @param clazz The fully qualified name of the plugin class.
     * @param cl The class loader to use for loading the plugin and its proxy.
     * @return The proxied plugin instance.
     * @throws Exception if an error occurs during proxy creation or instantiation.
     */
    @Override
    @SuppressWarnings("java:S112")
    public Sps4jPlugin createPluginInstance(String clazz, Sps4jPluginClassLoader cl) throws Exception {
        try (ClassFileLocator classFileLocator = new ClassFileLocator.ForUrl(cl.getURLs())) {
            final TypePool typePool = TypePool.Default.of(new ClassFileLocator.Compound(classFileLocator,
                    ClassFileLocator.ForClassLoader.of(Thread.currentThread().getContextClassLoader())));
            final TypePool.Resolution describe = typePool.describe(clazz);
            DynamicType.Unloaded<Object> unloaded = new ByteBuddy()
                    .rebase(describe.resolve(), classFileLocator)
                    .implement(Sps4jProxy.class)
                    .method(isPublic()
                            .and(not(isStatic()))
                            .and(not(nameStartsWith("java")))
                    )
                    .intercept(MethodDelegation.to(new PluginMethodInvocationInterceptor()))
                    .make();
            return (Sps4jPlugin) CallUtils.executeWithContextLoader(cl, () -> unloaded
                    .load(cl, ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded()
                    .getDeclaredConstructor()
                    .newInstance());
        }
    }
}
