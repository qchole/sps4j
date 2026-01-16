package io.github.sps4j.core.load;

import io.github.sps4j.common.meta.MetaInfo;
import io.github.sps4j.common.meta.PluginDesc;
import io.github.sps4j.core.Sps4jPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URL;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultPluginLoaderTest {

    @Spy
    private DefaultPluginLoader pluginLoader = new DefaultPluginLoader();

    @Test
    void load_shouldCallLifecycleMethods() throws Exception {
        // --- SETUP ---
        Sps4jPlugin mockPlugin = mock(Sps4jPlugin.class);
        MetaInfo metaInfo = mock(MetaInfo.class);
        PluginDesc desc = mock(PluginDesc.class);
        Map<String, Object> conf = Collections.singletonMap("key", "value");

        when(metaInfo.getDescriptor()).thenReturn(desc);
        when(desc.getClassName()).thenReturn("com.example.FakePlugin");

        // Stub the complex createPluginInstance method to return our mock plugin
        doReturn(mockPlugin).when(pluginLoader).createPluginInstance(any(), any());

        // --- EXECUTION ---
        // Pass a dummy classloader to ensure the `if (cl != null)` branch is taken, making the test more robust.
        Sps4jPlugin loadedPlugin = pluginLoader.load(metaInfo, new Sps4jPluginClassLoader(new URL[]{}, null), conf);

        // --- VERIFICATION ---
        // Verify that createPluginInstance was called
        verify(pluginLoader, times(1)).createPluginInstance(eq("com.example.FakePlugin"), any(Sps4jPluginClassLoader.class));

        // Verify that the onLoad method was called on the plugin instance
        verify(mockPlugin, times(1)).onLoad(conf, metaInfo);

        // Verify that the lifecycle hooks on the loader were called
        verify(pluginLoader, times(1)).pluginCreated(mockPlugin, metaInfo);
        verify(pluginLoader, times(1)).postLoadPlugin(mockPlugin, metaInfo);

        // The final returned plugin should be the one from postLoadPlugin, which is the mockPlugin in the spy's default implementation.
        assertSame(mockPlugin, loadedPlugin);
    }
}