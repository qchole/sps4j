package io.github.sps4j.springboot2.loader;

import io.github.sps4j.common.meta.MetaInfo;
import io.github.sps4j.common.meta.PluginDesc;
import io.github.sps4j.common.utils.CallUtils;
import io.github.sps4j.core.Sps4jPlugin;
import io.github.sps4j.springboot2.SpringBoot2AppPlugin;
import io.github.sps4j.springboot2.context.HostApplicationContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SpringAppSupportPluginLoader}.
 */
@ExtendWith(MockitoExtension.class)
class SpringAppSupportPluginLoaderTest {

    @InjectMocks
    private SpringAppSupportPluginLoader pluginLoader;

    @Mock
    private Sps4jPlugin regularPlugin;

    @Mock
    private SpringBoot2AppPlugin springBootAppPlugin;

    @Mock
    private MetaInfo metaInfo;

    @Mock
    private PluginDesc descriptor;

    private MockedStatic<CallUtils> callUtilsMockedStatic;
    private MockedStatic<HostApplicationContextHolder> hostApplicationContextHolderMockedStatic;

    @BeforeEach
    void setUp() {
        // mock static methods
        callUtilsMockedStatic = mockStatic(CallUtils.class);
        hostApplicationContextHolderMockedStatic = mockStatic(HostApplicationContextHolder.class);
    }

    @AfterEach
    void tearDown() {
        callUtilsMockedStatic.close();
        hostApplicationContextHolderMockedStatic.close();
    }


    @Test
    void pluginCreated_withRegularPlugin_shouldAutowire() {
        callUtilsMockedStatic.when(() -> CallUtils.runWithContextLoader(any(), any(Runnable.class)))
                .thenAnswer(invocation -> {
                    invocation.getArgument(1, Runnable.class).run();
                    return null;
                });

        Sps4jPlugin result = pluginLoader.pluginCreated(regularPlugin, metaInfo);

        hostApplicationContextHolderMockedStatic.verify(() -> HostApplicationContextHolder.autowireFromHost(regularPlugin));
        assertSame(regularPlugin, result);
    }

    @Test
    void pluginCreated_withSpringBoot2AppPlugin_shouldNotAutowire() {
        Sps4jPlugin result = pluginLoader.pluginCreated(springBootAppPlugin, metaInfo);

        hostApplicationContextHolderMockedStatic.verifyNoInteractions();
        assertSame(springBootAppPlugin, result);
    }

    @Test
    void postLoadPlugin_withSpringBoot2AppPlugin_shouldReturnBean() {
        org.springframework.context.ConfigurableApplicationContext applicationContext = mock(org.springframework.context.ConfigurableApplicationContext.class);
        Sps4jPlugin beanInstance = mock(Sps4jPlugin.class);
        String className = "com.example.TestPlugin";
        String beanName = "testPlugin";

        when(springBootAppPlugin.getApplicationContext()).thenReturn(applicationContext);
        when(descriptor.getClassName()).thenReturn(className);
        when(metaInfo.getDescriptor()).thenReturn(descriptor);
        when(applicationContext.getBean(beanName)).thenReturn(beanInstance);

        Sps4jPlugin result = pluginLoader.postLoadPlugin(springBootAppPlugin, metaInfo);

        assertSame(beanInstance, result);
        verify(applicationContext).getBean(beanName);
    }

    @Test
    void postLoadPlugin_withRegularPlugin_shouldReturnOriginalInstance() {
        Sps4jPlugin result = pluginLoader.postLoadPlugin(regularPlugin, metaInfo);

        assertSame(regularPlugin, result);
        verifyNoInteractions(metaInfo);
    }
}