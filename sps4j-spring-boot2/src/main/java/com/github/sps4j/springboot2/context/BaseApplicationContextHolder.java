package com.github.sps4j.springboot2.context;

import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;

/**
 * A singleton holder for the base (host) application's {@link ApplicationContext}.
 * <p>
 * This allows plugins to access the base application's context, for instance, to autowire beans
 * from the base application into plugin components.
 *
 * @author Allan-QLB
 */
public final class BaseApplicationContextHolder {

    private static BaseApplicationContextHolder instance;
    private final  ApplicationContext baseApplicationContext;


    private BaseApplicationContextHolder(ApplicationContext baseCtx) {
        this.baseApplicationContext = baseCtx;
    }

    /**
     * Creates and initializes the singleton instance.
     * This method is synchronized to ensure thread safety.
     *
     * @param applicationContext The base application's context.
     * @return The singleton instance.
     */
    public static synchronized BaseApplicationContextHolder create(ApplicationContext applicationContext) {
        if (instance == null) {
            instance = new BaseApplicationContextHolder(applicationContext);
        }
        return instance;
    }

    /**
     * Gets the singleton instance.
     *
     * @return The singleton instance.
     * @throws IllegalStateException if the holder has not been initialized via {@link #create(ApplicationContext)}.
     */
    public static BaseApplicationContextHolder getInstance() {
        if (instance == null) {
            throw new IllegalStateException("BaseApplicationContextHolder is not initialized");
        }
        return instance;
    }

    /**
     * Gets the base application's {@link ServletWebServerApplicationContext}.
     *
     * @return The base application context.
     */
    public static ApplicationContext getBaseAppContext() {
        return getInstance().baseApplicationContext;
    }

    /**
     * Autowires dependencies into the given object from the base application's bean factory.
     *
     * @param obj The object to autowire.
     */
    public static void autowireFromBase(Object obj) {
        getInstance().baseApplicationContext.getAutowireCapableBeanFactory().autowireBean(obj);
    }


}

