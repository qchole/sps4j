package com.github.sps4j.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.sps4j.common.Const;
import com.github.sps4j.common.meta.MetaInfo;
import com.github.sps4j.common.meta.PluginArtifact;
import com.github.sps4j.common.meta.PluginDesc;
import com.github.sps4j.common.meta.VersionedPluginArtifact;
import com.github.sps4j.common.utils.YamlUtils;
import com.github.sps4j.core.exception.PluginException;
import com.github.sps4j.core.load.*;
import com.github.sps4j.core.load.storage.LocalDirJarPackageStorage;
import com.github.sps4j.core.load.storage.PluginPackage;
import com.github.sps4j.core.load.storage.PluginStorage;
import com.google.common.annotations.VisibleForTesting;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The default implementation of the {@link PluginManager} interface.
 * <p>
 * This class is responsible for discovering plugins from a storage backend,
 * loading their metadata, handling their lifecycle (loading, unloading),
 * and providing access to plugin instances. It maintains a cache of loaded
 * plugins and their metadata.
 *
 * @author Allan-QLB
 */
@Slf4j
public class DefaultPluginManager implements PluginManager {
    private static final String PLUGIN_DESC_FOUND_MSG_PREF = "Can not found any plugin descriptor of type ";
    private static volatile boolean interfaceDiscovered = false;
    protected static final Map<String, String> SUPPORTED_TYPES = new HashMap<>();
    @Nonnull
    private final String baseUrl;
    private volatile boolean productServiceInitialized = false;
    @Nonnull
    private final ProductPluginLoadService productPluginLoadService;
    private final Map<String, Map<String, MetaInfo>> pluginMetaMap = new ConcurrentHashMap<>();
    private final Map<PluginArtifact, PluginWrapper> loaded = new ConcurrentHashMap<>();
    @Nonnull
    private final PluginStorage storage;
    @Nonnull
    private final Sps4jPluginLoader pluginLoader;

        static {
            discoverInterfaces();
        }
    
        /**
         * Constructs a new DefaultPluginManager with default storage and loader.
         *
         * @param baseUrl                  The base URL where plugins are located.
         * @param productPluginLoadService The service providing product-specific information.
         */
        public DefaultPluginManager(@Nonnull String baseUrl, @Nonnull ProductPluginLoadService productPluginLoadService) {
            this(baseUrl, productPluginLoadService, true, new LocalDirJarPackageStorage(), new DefaultPluginLoader());
        }
    
        /**
         * Constructs a new DefaultPluginManager with a custom plugin loader.
         *
         * @param baseUrl                  The base URL where plugins are located.
         * @param productPluginLoadService The service providing product-specific information.
         * @param pluginLoader             The custom plugin loader to use.
         */
        public DefaultPluginManager(@Nonnull String baseUrl, @Nonnull ProductPluginLoadService productPluginLoadService,
                                    @Nonnull Sps4jPluginLoader pluginLoader) {
            this(baseUrl, productPluginLoadService, true, new LocalDirJarPackageStorage(), pluginLoader);
        }
    
        /**
         * Constructs a new DefaultPluginManager with full custom configuration.
         *
         * @param baseUrl                  The base URL where plugins are located.
         * @param productPluginLoadService The service providing product-specific information.
         * @param init                 Whether to automatically initialize the manager upon construction.
         * @param storage                  The plugin storage implementation.
         * @param pluginLoader             The plugin loader implementation.
         */
        public DefaultPluginManager(@Nonnull String baseUrl,
                                    @Nonnull ProductPluginLoadService productPluginLoadService,
                                    boolean init,
                                    @Nonnull PluginStorage storage,
                                    @Nonnull Sps4jPluginLoader pluginLoader
        ) {
            this.baseUrl = baseUrl;
            this.productPluginLoadService = productPluginLoadService;
            this.storage = storage;
            this.pluginLoader = pluginLoader;
            if (init) {
                init();
            }
    }

    public static boolean isSupportedType(String type) {
        return SUPPORTED_TYPES.containsValue(type);
    }

    void discoverInterfacesIfNecessary() {
        if (!interfaceDiscovered) {
            discoverInterfaces();
        }
    }

    void initializeProductServiceIfNecessary() {
        if (!productServiceInitialized) {
            productPluginLoadService.init();
            productServiceInitialized = true;
        }
    }

    private static synchronized void discoverInterfaces() {
        if (interfaceDiscovered) {
            return;
        }
        try {
            SUPPORTED_TYPES.putAll(doScan());
            interfaceDiscovered = true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @SneakyThrows
    @VisibleForTesting
    public static Map<String, String> doScan() {
        Map<String, String> result = new HashMap<>();
        Enumeration<URL> resources = ClassLoader.getSystemClassLoader().getResources(Const.INTERFACE_FILE);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            Map<String, String> content = YamlUtils.getYamlMapper().readerFor(new TypeReference<Map<String, String>>() {
            }).readValue(url);
            content.forEach((type, clazz) -> result.put(clazz, type));
        }
        return result;
    }

    void loadMetadata(@Nullable PluginArtifact artifact) {
        final List<PluginPackage> containers = storage.listPackages(baseUrl);
        for (PluginPackage c : containers) {
            try (final PluginPackage container = c) {
                if (!c.contains(Const.DESC_FILE)) {
                    continue;
                }
                List<PluginDesc> descriptors = loadDescriptors(container.getResource(Const.DESC_FILE));
                for (PluginDesc descriptor : descriptors) {
                    if (artifact != null && (!Objects.equals(artifact.getType(), descriptor.getType()) || !Objects.equals(artifact.getName(), descriptor.getName()))) {
                        continue;
                    }
                    final Map<String, MetaInfo> typeMetaMap =
                            pluginMetaMap.computeIfAbsent(descriptor.getType(), t -> new HashMap<>());
                    final MetaInfo existMeta = typeMetaMap.get(descriptor.getName());
                    if (canLoad(productPluginLoadService, descriptor)) {
                        MetaInfo newMetaInfo = new MetaInfo(descriptor, URI.create(container.getBaseUrl()).toURL());
                        if (existMeta == null || existMeta.getDescriptor().getVersion().compareTo(newMetaInfo.getDescriptor().getVersion()) < 0) {
                            typeMetaMap.put(descriptor.getName(), newMetaInfo);
                            if (existMeta != null) {
                                log.info("replace plugin metaInfo {} with {}", existMeta, newMetaInfo);
                            } else {
                                log.info("add plugin metaInfo {}", newMetaInfo);
                            }
                        }

                    } else {
                        log.info("Plugin {}:{}:{} is not supported by current product",
                                descriptor.getType(), descriptor.getName(), descriptor.getVersion());
                    }
                }
            } catch (Exception e) {
                throw new PluginException(e.getMessage(), e);
            }
        }
    }

    private List<PluginDesc> loadDescriptors(@Nonnull InputStream stream) throws IOException {
        final String content = IOUtils.toString(stream, StandardCharsets.UTF_8);
        final JsonNode jsonNode = YamlUtils.getYamlMapper().readTree(content);
        if (jsonNode.isArray()) {
            return YamlUtils.getYamlMapper().convertValue(jsonNode, new TypeReference<List<PluginDesc>>() {});
        } else {
            return Collections.singletonList(YamlUtils.getYamlMapper().convertValue(jsonNode, PluginDesc.class));
        }
    }

    boolean canLoad(ProductPluginLoadService pluginService, PluginDesc pd) {
        return pluginService.productVersion().satisfies(pd.getProductVersionConstraint());
    }

    @Override
    public synchronized void init() {
        discoverInterfacesIfNecessary();
        initializeProductServiceIfNecessary();
        loadMetadata(null);
    }

    @Override
    public synchronized void resetAll() {
        unloadAll();
        loadMetadata(null);
    }

    @Override
    public synchronized void reset(@Nonnull PluginArtifact artifact) {
        unload(artifact);
        loadMetadata(artifact);
    }

    @Override
    public void unloadAll() {
        for (Map.Entry<PluginArtifact, PluginWrapper> entry : loaded.entrySet()) {
            final Sps4jPlugin plugin = entry.getValue().getPlugin();
            final PluginDesc descriptor = entry.getValue().getMetaInfo().getDescriptor();
            Sps4jPluginClassLoader cl = (Sps4jPluginClassLoader) plugin.getClass().getClassLoader();
            try {
                plugin.onDestroy();
                cl.close();
            } catch (Exception e) {
                throw new PluginException("Error remove plugin " +
                        VersionedPluginArtifact.builder().artifact(
                                        PluginArtifact.builder()
                                                .type(descriptor.getType())
                                                .name(descriptor.getName())
                                                .build())
                                .version(descriptor.getVersion())
                                .build(), e);
            }
        }
        if (!loaded.isEmpty()) {
            loaded.clear();
        }
        if (!pluginMetaMap.isEmpty()) {
            pluginMetaMap.clear();
        }
    }

    @Override
    public void unload(@Nonnull String type) {
        if (StringUtils.isEmpty(type)) {
            throw new IllegalArgumentException("plugin type cannot be empty");
        }
        if (!loaded.isEmpty()) {
            List<PluginArtifact> toRemove = new ArrayList<>();
            loaded.forEach((art, pw) -> {
                if (Objects.equals(art.getType(), type)) {
                    toRemove.add(art);
                    try {
                        Sps4jPlugin plugin = pw.getPlugin();
                        plugin.onDestroy();
                        ((Sps4jPluginClassLoader) plugin.getClass().getClassLoader()).close();
                    } catch (Exception e) {
                        throw new PluginException("Error remove plugin " + VersionedPluginArtifact.builder()
                                .artifact(art)
                                .version(pw.getMetaInfo().getDescriptor().getVersion())
                                .build(), e);
                    }
                }
            });
            for (PluginArtifact pluginArtifact : toRemove) {
                loaded.remove(pluginArtifact);
            }
        }
        if (!pluginMetaMap.isEmpty()) {
            pluginMetaMap.remove(type);
        }
    }

    @Override
    public void unload(@Nonnull Class<?> pluginInterface) {
        unload(checkInterfaceSupported(pluginInterface));
    }

    @Override
    public void unload(@Nonnull PluginArtifact artifact) {
        if (!loaded.isEmpty()) {
            final PluginWrapper pluginWithMetadata = loaded.remove(artifact);
            if (pluginWithMetadata != null) {
                try {
                    pluginWithMetadata.getPlugin().onDestroy();
                    ((Sps4jPluginClassLoader) pluginWithMetadata.getPlugin().getClass().getClassLoader()).close();
                } catch (Exception e) {
                    throw new PluginException("Error remove plugin " + VersionedPluginArtifact.builder()
                            .artifact(artifact)
                            .version(pluginWithMetadata.getMetaInfo().getDescriptor().getVersion())
                            .build(), e);
                }
            }
        }
        if (!pluginMetaMap.isEmpty()) {
            final Map<String, MetaInfo> nameMeta = pluginMetaMap.get(artifact.getType());
            if (MapUtils.isNotEmpty(nameMeta)) {
                nameMeta.remove(artifact.getName());
            }
        }
    }

    private String checkInterfaceSupported(@Nonnull Class<?> pluginInterface) {
        String type = SUPPORTED_TYPES.get(pluginInterface.getName());
        if (type == null) {
            throw new IllegalArgumentException("Plugin interface " + pluginInterface.getName() + " is not supported");
        }
        return type;
    }


    PluginWrapper getLoadedPlugin(PluginArtifact pluginArtifact) {
        return loaded.get(pluginArtifact);
    }

    @Override
    public MetaInfo getPluginMetaInfo(@Nonnull PluginArtifact artifact) {
        if (!isSupportedType(artifact.getType())) {
            throw new PluginException("Unsupported plugin type: " + artifact.getType() + " supported types:" + SUPPORTED_TYPES.values());
        }
        final Map<String, MetaInfo> nameMeta = pluginMetaMap.get(artifact.getType());
        if (MapUtils.isEmpty(nameMeta)) {
            return null;
        }
        return nameMeta.get(artifact.getName());
    }

    public synchronized PluginWrapper getPlugin(String type, String name, Sps4jPluginClassLoader classLoader, Map<String, Object> config) {
        final PluginArtifact artifact = new PluginArtifact(type, name);
        PluginWrapper loadedPlugin = getLoadedPlugin(artifact);
        if (loadedPlugin != null) {
            return loadedPlugin;
        }
        MetaInfo metaInfo = Optional.ofNullable(getPluginMetaInfo(artifact)).orElseThrow(() -> new PluginException(PLUGIN_DESC_FOUND_MSG_PREF + artifact));
        final PluginWrapper pluginWrapper = PluginWrapper.builder()
                .plugin(pluginLoader.load(metaInfo, classLoader, config)).metaInfo(metaInfo).build();
        loaded.put(artifact, pluginWrapper);
        log.info("load sps4j plugin {}", VersionedPluginArtifact.builder()
                .artifact(PluginArtifact.builder().type(type).name(name).build())
                .version(metaInfo.getDescriptor().getVersion())
                .build()
        );
        return pluginWrapper;
    }

    @Override
    public PluginWrapper getPlugin(@Nonnull String type, @Nonnull String name) {
        return getPlugin(type, name, null, Collections.emptyMap());
    }

    @Override
    public PluginWrapper getPlugin(@Nonnull PluginArtifact artifact) {
        return getPlugin(artifact.getType(), artifact.getName());
    }

    @Override
    public <T extends Sps4jPlugin> T getPluginUnwrapped(@Nonnull Class<T> pluginInterface, @Nonnull PluginArtifact artifact) {
        return getPluginUnwrapped(pluginInterface, artifact, Collections.emptyMap());
    }

    @Override
    public <T extends Sps4jPlugin> T getPluginUnwrapped(@Nonnull Class<T> pluginInterface, @Nonnull PluginArtifact artifact, @Nonnull Map<String, Object> config) {
        if (!Objects.equals(artifact.getType(), checkInterfaceSupported(pluginInterface))) {
            throw new IllegalArgumentException("Plugin type of artifact " + artifact.getType() + " is not same as plugin interface " + pluginInterface);
        }
        return getPlugin(pluginInterface, artifact.getName(), config).getPluginAs(pluginInterface);
    }

    @Override
    public PluginWrapper getPlugin(@Nonnull String type, @Nonnull String name, @Nonnull Map<String, Object> conf) {
        return getPlugin(type, name, null,  conf);
    }

    @Override
    public PluginWrapper getPlugin(@Nonnull Class<?> pluginInterface, @Nonnull String name,  @Nonnull Map<String, Object> conf) {
        return getPlugin(checkInterfaceSupported(pluginInterface), name, conf);
    }

    @Override
    public <T extends Sps4jPlugin> T getPluginUnwrapped(@Nonnull Class<T> pluginInterface, @Nonnull String name, @Nonnull Map<String, Object> config) {
        return getPlugin(pluginInterface, name, config).getPluginAs(pluginInterface);
    }

    @Override
    public synchronized List<PluginWrapper> getPlugins(@Nonnull String type) {
        return getPlugins(type, Collections.emptyMap());
    }

    @Override
    public List<PluginWrapper> getPlugins(Class<? extends Sps4jPlugin> pluginInterface, Map<String, Object> conf) {
        String type = checkInterfaceSupported(pluginInterface);
        return getPlugins(type, conf);
    }

    @Override
    public <T extends Sps4jPlugin> List<T> getPluginsUnwrapped(Class<T> pluginInterface, Map<String, Object> conf) {
        return getPlugins(pluginInterface, conf).stream().map(pw -> pw.getPluginAs(pluginInterface))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<PluginWrapper> getPlugins(@Nonnull String type, Map<String, Object> conf) {
        final Map<String, MetaInfo> nameMeta = pluginMetaMap.get(type);
        if (MapUtils.isEmpty(nameMeta)) {
            throw new PluginException(PLUGIN_DESC_FOUND_MSG_PREF + type);
        }
        final Set<String> names = nameMeta.keySet();
        if (CollectionUtils.isEmpty(names)) {
            throw new PluginException(PLUGIN_DESC_FOUND_MSG_PREF + type);
        }
        return names.stream().map(name -> getPlugin(type, name, conf)).collect(Collectors.toList());
    }


    @SuppressWarnings("java:S2095")
    @Override
    public synchronized List<PluginWrapper> getPluginsSharingClassLoader(@Nonnull String first, String... rest) {
        List<MetaInfo> metas = new ArrayList<>();
        String[] types = ArrayUtils.add(rest, first);
        for (String type : types) {
            final Map<String, MetaInfo> nameMap = pluginMetaMap.get(type);
            if (MapUtils.isNotEmpty(nameMap)) {
                for (Map.Entry<String, MetaInfo> entry : nameMap.entrySet()) {
                    metas.add(entry.getValue());
                }
            }
        }
        if (CollectionUtils.isEmpty(metas)) {
            throw new PluginException(PLUGIN_DESC_FOUND_MSG_PREF + " nothing to load");
        }
        final URL[] urls = metas.stream().map(MetaInfo::getUrl).toArray(URL[]::new);
        final Sps4jPluginClassLoader classLoader = new Sps4jPluginClassLoader(urls, Sps4jPlugin.class.getClassLoader());
        return metas.stream()
                .map(MetaInfo::getDescriptor)
                .map(d -> getPlugin(d.getType(), d.getName(), classLoader, Collections.emptyMap())).collect(Collectors.toList());
    }


}
