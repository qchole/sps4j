package io.github.sps4j.springboot2.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HostApplicationContextHolderTest {

    @BeforeEach
    public void setUp() throws Exception {
        // Reset the singleton instance before each test
        Field instance = HostApplicationContextHolder.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    @Test
    public void getInstance_whenNotInitialized_throwsIllegalStateException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, HostApplicationContextHolder::getInstance);
        assertEquals("HostApplicationContextHolder is not initialized", exception.getMessage());
    }

    @Test
    public void create_initializesInstance() {
        ApplicationContext mockContext = mock(ApplicationContext.class);
        HostApplicationContextHolder holder = HostApplicationContextHolder.create(mockContext);
        assertNotNull(holder);
        assertSame(holder, HostApplicationContextHolder.getInstance());
    }

    @Test
    public void create_doesNotReinitialize() {
        ApplicationContext mockContext1 = mock(ApplicationContext.class);
        ApplicationContext mockContext2 = mock(ApplicationContext.class);

        HostApplicationContextHolder holder1 = HostApplicationContextHolder.create(mockContext1);
        HostApplicationContextHolder holder2 = HostApplicationContextHolder.create(mockContext2);

        assertSame(holder1, holder2);
        assertSame(mockContext1, HostApplicationContextHolder.getHostAppContext());
    }

    @Test
    public void getHostAppContext_returnsCorrectContext() {
        ApplicationContext mockContext = mock(ApplicationContext.class);
        HostApplicationContextHolder.create(mockContext);
        assertSame(mockContext, HostApplicationContextHolder.getHostAppContext());
    }

    @Test
    public void autowireFromHost_autowiresBean() {
        ApplicationContext mockContext = mock(ApplicationContext.class);
        AutowireCapableBeanFactory mockBeanFactory = mock(AutowireCapableBeanFactory.class);
        when(mockContext.getAutowireCapableBeanFactory()).thenReturn(mockBeanFactory);

        HostApplicationContextHolder.create(mockContext);

        Object obj = new Object();
        HostApplicationContextHolder.autowireFromHost(obj);

        verify(mockBeanFactory).autowireBean(obj);
    }
}
