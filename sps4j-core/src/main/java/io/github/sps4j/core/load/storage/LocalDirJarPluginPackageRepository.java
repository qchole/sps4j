package io.github.sps4j.core.load.storage;

import lombok.AllArgsConstructor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An implementation of {@link PluginRepository} that scans a local directory for JAR files.
 * Each found JAR file is treated as a {@link PluginPackage}.
 *
 * @author Allan-QLB
 */
@AllArgsConstructor
public class LocalDirJarPluginPackageRepository implements PluginRepository {
    private static final String FILE_PROTOCOL = "file://";
    private static final String FILE_PROTOCOL_ALT = "file:";
    private static final String JAR = "jar";
    private final String baseUrl;

    /**
     * Lists all JAR files in a given directory path and wraps them as {@link JarPackage} objects.
     * The path can be a standard file path or a URL with a "file://" or "file:" scheme.
     *
     * @param baseUrl The file system path or URL to the directory to scan.
     * @return A list of {@link PluginPackage} objects representing the found JAR files.
     */
    @Override
    public List<PluginPackage> listPackages() {
        String url = baseUrl;
        if (baseUrl.startsWith(FILE_PROTOCOL)) {
            url = url.substring(FILE_PROTOCOL.length());
        }
        if (baseUrl.startsWith(FILE_PROTOCOL_ALT)) {
            url = url.substring(FILE_PROTOCOL_ALT.length());
        }
        return FileUtils.listFiles(new File(url), new String[]{JAR}, true)
                .stream().map(JarPackage::new).collect(Collectors.toList());

    }
}