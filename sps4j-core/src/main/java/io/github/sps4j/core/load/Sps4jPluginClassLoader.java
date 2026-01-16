package io.github.sps4j.core.load;


import io.github.sps4j.common.utils.CallUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.regex.Pattern;

/**
 * A custom {@link URLClassLoader} for loading plugins.
 * <p>
 * This class loader implements a mixed parent-first/child-first delegation model.
 * It enforces parent-first loading for framework classes (under {@code io.github.sps4j}) and other
 * explicitly configured packages. For all other classes, it attempts to load from the plugin's URLs first
 * (child-first), falling back to the parent loader if the class is not found.
 * This allows plugins to bundle their own dependencies while sharing the core framework.
 *
 * @author Allan-QLB
 */
@Getter
@Slf4j
public class Sps4jPluginClassLoader extends URLClassLoader {
    private static final String FRAMEWORK_PACKAGE = "io.github.sps4j";
    private final Set<String> parentFirstPackages =  new HashSet<>();
    private final List<Runnable> onCloseActions = new ArrayList<>();
    private final Map<String,Pattern> ignoreParentResourceNamePatterns = new HashMap<>();

    /**
     * Constructs a new plugin class loader.
     *
     * @param urls   The URLs from which to load classes and resources.
     * @param parent The parent class loader.
     */
    public Sps4jPluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    /**
     * Adds resource name patterns to be ignored by the parent class loader.
     * When a resource matching one of these patterns is requested, the parent class loader will not be queried.
     *
     * @param first The first pattern to add.
     * @param rest  Additional patterns to add.
     */
    public void addIgnoreParentResourceNamePattern(String first, String ... rest) {
        ignoreParentResourceNamePatterns.put(first, Pattern.compile(first));
        for (String pattern : rest) {
            ignoreParentResourceNamePatterns.put(pattern, Pattern.compile(pattern));
        }
    }

    /**
     * Adds a cleanup action to be executed when this class loader is closed.
     *
     * @param action The action to run on close.
     */
    public void addOnCloseAction(Runnable action) {
        synchronized (onCloseActions) {
            onCloseActions.add(action);
        }
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // Parent-first for framework classes and configured packages.
        if (name.startsWith(FRAMEWORK_PACKAGE)) {
            return super.loadClass(name, resolve);
        }
        for (String pkg : parentFirstPackages) {
            if (name.startsWith(pkg)) {
                return super.loadClass(name, resolve);
            }
        }

        // Child-first for all other classes.
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                try {
                    // 1. Try to find in this classloader's URLs.
                    c = findClass(name);
                } catch (ClassNotFoundException e) {
                    // 2. If not found, delegate to the parent.
                    return super.loadClass(name, resolve);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    @Override
    public URL getResource(String name) {
        // 1. Try to find it in this classloader's URLs (child-first).
        URL url = findResource(name);

        if (url != null) {
            return url;
        }

        // 2. Check ignore patterns before delegating to parent.
        String fname = FilenameUtils.getName(name);
        for (Pattern pattern : ignoreParentResourceNamePatterns.values()) {
            if (pattern.matcher(fname).find()) {
                return null; // Ignore parent resource
            }
        }

        // 3. Delegate to parent.
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        final List<URL> result = new ArrayList<>();

        // 1. Get resources from this classloader's URLs (child-first).
        Enumeration<URL> childResources = findResources(name);
        while (childResources.hasMoreElements()) {
            result.add(childResources.nextElement());
        }

        // 2. Check if parent should be queried.
        boolean addParent = true;
        String fname = FilenameUtils.getName(name);
        for (Pattern pattern : ignoreParentResourceNamePatterns.values()) {
            if (pattern.matcher(fname).find()) {
                addParent = false;
                break;
            }
        }

        // 3. Add resources from parent if not ignored.
        if (addParent) {
            Enumeration<URL> parentResources = getParent().getResources(name);
            while (parentResources.hasMoreElements()) {
                result.add(parentResources.nextElement());
            }
        }

        return Collections.enumeration(result);
    }

    /**
     * Closes this class loader and runs all registered close actions.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        synchronized (onCloseActions) {
            onCloseActions.forEach(action -> {
                try {
                    CallUtils.runWithContextLoader(this, action);
                } catch (Exception e) {
                    log.error("Error run close action {}", action, e);
                }
            });
            onCloseActions.clear();
        }
        super.close();
    }
}

