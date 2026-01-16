package io.github.sps4j.springboot2.context;

import org.springframework.context.ApplicationContext;

/**
 * A singleton holder for the host application's {@link ApplicationContext}.
 * <p>
 * This allows plugins to access the host application's context, for instance, to autowire beans
 * from the host application into plugin components.
 *
 * @author Allan-QLB
 */
public final class HostApplicationContextHolder {

    private static HostApplicationContextHolder instance;
    private final  ApplicationContext hostApplicationContext;


    private HostApplicationContextHolder(ApplicationContext hostApplicationContext) {
        this.hostApplicationContext = hostApplicationContext;
    }

    /**
     * Creates and initializes the singleton instance.
     * This method is synchronized to ensure thread safety.
     *
     * @param applicationContext The host application's context.
     * @return The singleton instance.
     */
    public static synchronized HostApplicationContextHolder create(ApplicationContext applicationContext) {
        if (instance == null) {
            instance = new HostApplicationContextHolder(applicationContext);
        }
        return instance;
    }

    /**
     * Gets the singleton instance.
     *
     * @return The singleton instance.
     * @throws IllegalStateException if the holder has not been initialized via {@link #create(ApplicationContext)}.
     */
    public static HostApplicationContextHolder getInstance() {
        if (instance == null) {
            throw new IllegalStateException("HostApplicationContextHolder is not initialized");
        }
        return instance;
    }

    /**
     * Gets the host application's {@link ApplicationContext}.
     *
     * @return The host application context.
     */
    public static ApplicationContext getBaseAppContext() {
        return getInstance().hostApplicationContext;
    }

    /**
     * Autowires dependencies into the given object from the host application's bean factory.
     *
     * @param obj The object to autowire.
     */
    public static void autowireFromHost(Object obj) {
        getInstance().hostApplicationContext.getAutowireCapableBeanFactory().autowireBean(obj);
    }


}

