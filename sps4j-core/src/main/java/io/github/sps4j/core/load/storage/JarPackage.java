package io.github.sps4j.core.load.storage;

import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * An implementation of {@link PluginPackage} that wraps a JAR file.
 *
 * @author Allan-QLB
 */
public class JarPackage implements PluginPackage {
    private final File file;
    private final JarFile jarFile;

    /**
     * Constructs a new JarPackage from a {@link File}.
     *
     * @param file The JAR file.
     */
    @SneakyThrows
    public JarPackage(File file) {
        this.file = file;
        this.jarFile = new JarFile(file);
    }


    @Override
    public InputStream getResource(String resource) throws IOException {
        final JarEntry ent = jarFile.getJarEntry(resource);
        if (ent == null) {
            throw new IOException("jar file " + file + " missing item " + resource);
        }
        return jarFile.getInputStream(ent);
    }

    @Override
    public boolean contains(String resource) {
        return jarFile.getJarEntry(resource) != null;
    }


    @Override
    @SneakyThrows
    public String getBaseUrl() {
        return new URL("file:///"+ file.getAbsolutePath()).toString();
    }

    @Override
    public void close() throws Exception {
        jarFile.close();
    }
}
