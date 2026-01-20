package io.github.sps4j.core;

import io.github.sps4j.common.Const;
import io.github.sps4j.common.meta.MetaInfo;
import io.github.sps4j.common.meta.PluginArtifact;
import io.github.sps4j.core.exception.PluginException;
import io.github.sps4j.core.load.ProductPluginLoadService;
import io.github.sps4j.core.load.Sps4jPluginLoader;
import io.github.sps4j.core.load.PluginWrapper;
import io.github.sps4j.core.load.storage.PluginPackage;
import io.github.sps4j.core.load.storage.PluginRepository;
import io.github.sps4j.core.test.TestPlugin;
import com.github.zafarkhaja.semver.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultPluginManagerTest {

    @Mock
    private ProductPluginLoadService productPluginLoadService;
    @Mock
    private PluginRepository pluginStorage;
    @Mock
    private Sps4jPluginLoader sps4jPluginLoader;
    @Mock
    private PluginPackage pkgWithValidPlugin;
    @Mock
    private PluginPackage pkgWithIncompatiblePlugin;
    @Mock
    private PluginPackage pkgWithOldVersion;
    @Mock
    private PluginPackage pkgWithNewVersion;
    @Mock
    private PluginPackage pkgWithoutDescriptor;
    @Mock
    private PluginPackage pkgWithMultipleDescriptors;

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
        DefaultPluginManager pluginManager = new DefaultPluginManager(url.getPath(), () -> Version.parse("0.0.1"));
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

    @Test
    void unload_artifact_shouldRemovePluginAndMetadata() {
        // Given: a plugin manager with a loaded plugin
        URL url = ClassLoader.getSystemClassLoader().getResource("plugins");
        assertNotNull(url);
        DefaultPluginManager pluginManager = new DefaultPluginManager(url.toString(), () -> Version.parse("0.0.1"));
        PluginArtifact artifact = PluginArtifact.builder().type("test").name("MyTest").build();

        // When: the plugin is loaded
        PluginWrapper wrapper = pluginManager.getPlugin(artifact);

        // Then: assert the plugin is actually loaded
        assertNotNull(wrapper);
        assertNotNull(pluginManager.getPluginMetaInfo(artifact), "Plugin metadata should be loaded.");
        assertNotNull(pluginManager.getLoadedPlugin(artifact), "Plugin instance should be loaded.");

        // When: the plugin is unloaded
        pluginManager.unload(artifact);

        // Then: assert the plugin is fully removed
        assertNull(pluginManager.getPluginMetaInfo(artifact), "Plugin metadata should be removed after unload.");
        assertNull(pluginManager.getLoadedPlugin(artifact), "Plugin instance should be removed after unload.");

        // And: attempting to get the plugin again should fail
        assertThrows(PluginException.class, () -> pluginManager.getPlugin(artifact),
                "Attempting to get an unloaded plugin should throw PluginException.");
    }

    @Test
    void unload_byInterface_shouldRemovePluginsAndMetadata() {
        // Given: a plugin manager with a loaded plugin
        URL url = ClassLoader.getSystemClassLoader().getResource("plugins");
        assertNotNull(url);
        DefaultPluginManager pluginManager = new DefaultPluginManager(url.toString(), () -> Version.parse("0.0.1"));
        PluginArtifact artifact = PluginArtifact.builder().type("test").name("MyTest").build();

        // When: the plugin is loaded
        pluginManager.getPlugin(artifact);

        // Then: assert the plugin is actually loaded
        assertNotNull(pluginManager.getPluginMetaInfo(artifact), "Plugin metadata should be loaded.");
        assertNotNull(pluginManager.getLoadedPlugin(artifact), "Plugin instance should be loaded.");

        // When: the plugin is unloaded by its interface
        pluginManager.unload(TestPlugin.class);

        // Then: assert the plugin is fully removed
        assertNull(pluginManager.getPluginMetaInfo(artifact), "Plugin metadata should be removed after unload.");
        assertNull(pluginManager.getLoadedPlugin(artifact), "Plugin instance should be removed after unload.");

        // And: attempting to get the plugin again should fail
        assertThrows(PluginException.class, () -> pluginManager.getPlugin(artifact),
                "Attempting to get an unloaded plugin should throw PluginException because its type metadata is removed.");
    }

    @Test
    void init_shouldLoadMetadataCorrectly() {
        try {
            // Given: Various plugin packages representing different scenarios
            when(productPluginLoadService.productVersion()).thenReturn(Version.parse("0.0.1"));
            doNothing().when(productPluginLoadService).init();

            // Scenario 1: Valid plugin
            String validPluginYaml = readYaml("yaml/valid-plugin.yaml");
            setupMockPackage(pkgWithValidPlugin, "file:/repo/valid.jar", validPluginYaml);

            // Scenario 2: Incompatible plugin
            String incompatiblePluginYaml = readYaml("yaml/incompatible-plugin.yaml");
            setupMockPackage(pkgWithIncompatiblePlugin, "file:/repo/incompatible.jar", incompatiblePluginYaml);

            // Scenario 3: Newer version of a plugin
            String oldVersionYaml = readYaml("yaml/old-version.yaml");
            setupMockPackage(pkgWithOldVersion, "file:/repo/versioned-1.0.0.jar", oldVersionYaml);

            String newVersionYaml = readYaml("yaml/new-version.yaml");
            setupMockPackage(pkgWithNewVersion, "file:/repo/versioned-1.1.0.jar", newVersionYaml);

            // Scenario 4: No descriptor
            when(pkgWithoutDescriptor.contains(Const.DESC_FILE)).thenReturn(false);
            doNothing().when(pkgWithoutDescriptor).close();

            // Scenario 5: Multi-plugin descriptor
            String multiPluginYaml = readYaml("yaml/multi-plugin.yaml");
            setupMockPackage(pkgWithMultipleDescriptors, "file:/repo/multi.jar", multiPluginYaml);

            List<PluginPackage> packages = Arrays.asList(pkgWithValidPlugin, pkgWithIncompatiblePlugin, pkgWithOldVersion, pkgWithNewVersion, pkgWithoutDescriptor, pkgWithMultipleDescriptors);
            when(pluginStorage.listPackages()).thenReturn(packages);

            // When: The DefaultPluginManager is initialized
            DefaultPluginManager pluginManager = new DefaultPluginManager(productPluginLoadService, true, pluginStorage, sps4jPluginLoader);

            // Then: The metadata should be loaded according to the rules
            // 1. Valid plugin is loaded
            MetaInfo validMeta = pluginManager.getPluginMetaInfo(new PluginArtifact("test", "my-plugin-ok"));
            assertNotNull(validMeta);
            assertEquals("1.0.0", validMeta.getDescriptor().getVersion().toString());

            // 2. Incompatible plugin is not loaded
            assertNull(pluginManager.getPluginMetaInfo(new PluginArtifact("test", "my-plugin-incompatible")));

            // 3. Only the newest version of a plugin is loaded
            MetaInfo versionedMeta = pluginManager.getPluginMetaInfo(new PluginArtifact("test", "my-plugin-versioned"));
            assertNotNull(versionedMeta);
            assertEquals("1.1.0", versionedMeta.getDescriptor().getVersion().toString());
            URL expectedUrl = new URL("file:/repo/versioned-1.1.0.jar");
            assertEquals(expectedUrl, versionedMeta.getUrl());

            // 4. Multi-plugins are loaded
            MetaInfo multi1Meta = pluginManager.getPluginMetaInfo(new PluginArtifact("test", "multi-plugin-1"));
            assertNotNull(multi1Meta);
            assertEquals("1.0.0", multi1Meta.getDescriptor().getVersion().toString());

            MetaInfo multi2Meta = pluginManager.getPluginMetaInfo(new PluginArtifact("test", "multi-plugin-2"));
            assertNotNull(multi2Meta);
            assertEquals("1.0.0", multi2Meta.getDescriptor().getVersion().toString());
        } catch (Exception e) {
            fail("Test threw an unexpected exception", e);
        }
    }

    private String readYaml(String path) throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(path);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + path);
        }
        return new String(Files.readAllBytes(Paths.get(resource.toURI())));
    }

    private void setupMockPackage(PluginPackage mockPackage, String baseUrl, String yamlContent) throws Exception {
        when(mockPackage.contains(Const.DESC_FILE)).thenReturn(true);
        InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8));
        when(mockPackage.getResource(Const.DESC_FILE)).thenReturn(inputStream);
        lenient().when(mockPackage.getBaseUrl()).thenReturn(baseUrl);
        doNothing().when(mockPackage).close();
    }
}
