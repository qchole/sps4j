package io.github.sps4j.springboot2.web;


import io.github.sps4j.springboot2.context.HostApplicationContextHolder;
import org.apache.catalina.*;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import javax.servlet.ServletContainerInitializer;
import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A custom {@link TomcatServletWebServerFactory} for creating a {@link WebServer} for SPS4J plugins.
 * <p>
 * This factory is responsible for creating a web server that attaches a new Tomcat {@link Context}
 * to the existing Tomcat instance of the base application. This allows a plugin to function as a
 * self-contained web application (with its own servlets, filters, and listeners) within the
 * base application's server process.
 *
 * @author Allan-QLB
 */
public class Sps4jTomcatWebServerFactory extends TomcatServletWebServerFactory {
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private final ClassLoader pluginClassLoader;

    /**
     * Constructs a new factory.
     *
     * @param pluginClassLoader The class loader of the plugin for which the web server is being created.
     */
    public Sps4jTomcatWebServerFactory(ClassLoader pluginClassLoader) {
        this.pluginClassLoader = pluginClassLoader;
    }

    @Override
    public WebServer getWebServer(ServletContextInitializer... initializers) {
        ApplicationContext baseApplicationContext = HostApplicationContextHolder.getBaseAppContext();
        Assert.isInstanceOf(ServletWebServerApplicationContext.class, baseApplicationContext);
        ServletWebServerApplicationContext webServerApplicationContext = (ServletWebServerApplicationContext) baseApplicationContext;
        Tomcat embedTomcat = ((TomcatWebServer) webServerApplicationContext.getWebServer()).getTomcat();
        prepareContext(embedTomcat.getHost(), initializers);
        return getWebServer(embedTomcat);
    }


    @Override
    protected void prepareContext(Host host, ServletContextInitializer[] initializers) {
        if (host.getState() == LifecycleState.NEW) {
            super.prepareContext(host, initializers);
        } else {
            File documentRoot = getValidDocumentRoot();
            StandardContext context = new StandardContext();
            if (documentRoot != null) {
                context.setResources(new StandardRoot(context));
            }
            context.setName(getContextPath());
            context.setDisplayName(getDisplayName());
            context.setPath(getContextPath());
            File docBase = (documentRoot != null) ? documentRoot : createTempDir("tomcat-docbase");
            context.setDocBase(docBase.getAbsolutePath());
            context.addLifecycleListener(new Tomcat.FixContextListener());
            context.setParentClassLoader(Thread.currentThread().getContextClassLoader());
            resetDefaultLocaleMapping(context);
            addLocaleMappings(context);
            context.setUseRelativeRedirects(false);
            configureTldSkipPatterns(context);
            WebappLoader loader = new WebappLoader();
            loader.setDelegate(true);
            context.setLoader(loader);
            context.setParentClassLoader(this.pluginClassLoader);
            if (isRegisterDefaultServlet()) {
                addDefaultServlet(context);
            }
            if (shouldRegisterJspServlet()) {
                addJspServlet(context);
                addJasperInitializer(context);
            }
            context.addLifecycleListener(new StaticResourceConfigurer(context));
            ServletContextInitializer[] initializersToUse = mergeInitializers(initializers);
            context.setParent(host);
            configureContext(context, initializersToUse);
            host.addChild(context);
        }
    }

    /**
     * Override Tomcat's default locale mappings to align with other servers. See
     * {@code org.apache.catalina.util.CharsetMapperDefault.properties}.
     *
     * @param context the context to reset
     */
    private void resetDefaultLocaleMapping(StandardContext context) {
        context.addLocaleEncodingMappingParameter(Locale.ENGLISH.toString(),
                DEFAULT_CHARSET.displayName());
        context.addLocaleEncodingMappingParameter(Locale.FRENCH.toString(),
                DEFAULT_CHARSET.displayName());
    }

    private void addLocaleMappings(StandardContext context) {
        for (Map.Entry<Locale, Charset> entry : getLocaleCharsetMappings().entrySet()) {
            context.addLocaleEncodingMappingParameter(entry.getKey().toString(), entry.getValue()
                    .toString());
        }
    }

    private void configureTldSkipPatterns(StandardContext context) {
        StandardJarScanFilter filter = new StandardJarScanFilter();
        filter.setTldSkip(StringUtils.collectionToCommaDelimitedString(getTldSkipPatterns()));
        context.getJarScanner().setJarScanFilter(filter);
    }

    private void addDefaultServlet(Context context) {
        Wrapper defaultServlet = context.createWrapper();
        defaultServlet.setName("default");
        defaultServlet.setServletClass("org.apache.catalina.servlets.DefaultServlet");
        defaultServlet.addInitParameter("debug", "0");
        defaultServlet.addInitParameter("listings", "false");
        defaultServlet.setLoadOnStartup(1);
        // Otherwise the default location of a Spring DispatcherServlet cannot be set
        defaultServlet.setOverridable(true);
        context.addChild(defaultServlet);
        context.addServletMappingDecoded("/", "default");
    }

    private void addJspServlet(Context context) {
        Wrapper jspServlet = context.createWrapper();
        jspServlet.setName("jsp");
        jspServlet.setServletClass(getJsp().getClassName());
        jspServlet.addInitParameter("fork", "false");
        for (Map.Entry<String, String> entry : getJsp().getInitParameters().entrySet()) {
            jspServlet.addInitParameter(entry.getKey(), entry.getValue());
        }
        jspServlet.setLoadOnStartup(3);
        context.addChild(jspServlet);
        context.addServletMappingDecoded("*.jsp", "jsp");
        context.addServletMappingDecoded("*.jspx", "jsp");
    }

    private void addJasperInitializer(StandardContext context) {
        try {
            ServletContainerInitializer initializer = (ServletContainerInitializer) ClassUtils
                    .forName("org.apache.jasper.servlet.JasperInitializer", null).newInstance();
            context.addServletContainerInitializer(initializer, null);
        } catch (Exception ex) {
            // Probably not Tomcat 8
        }
    }

    final class StaticResourceConfigurer implements LifecycleListener {

        private final Context context;

        private StaticResourceConfigurer(Context context) {
            this.context = context;
        }

        @Override
        public void lifecycleEvent(LifecycleEvent event) {
            if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
                addResourceJars(getUrlsOfJarsWithMetaInfResources());
            }
        }

        private void addResourceJars(List<URL> resourceJarUrls) {
            for (URL url : resourceJarUrls) {
                String path = url.getPath();
                if (path.endsWith(".jar") || path.endsWith(".jar!/")) {
                    String jar = url.toString();
                    if (!jar.startsWith("jar:")) {
                        // A jar file in the file system. Convert to Jar URL.
                        jar = "jar:" + jar + "!/";
                    }
                    addResourceSet(jar);
                } else {
                    addResourceSet(url.toString());
                }
            }
        }

        private void addResourceSet(String resource) {
            try {
                if (isInsideNestedJar(resource)) {
                    // It's a nested jar but we now don't want the suffix because Tomcat
                    // is going to try and locate it as a root URL (not the resource
                    // inside it)
                    resource = resource.substring(0, resource.length() - 2);
                }
                URL url = new URL(resource);
                String path = "/META-INF/resources";
                this.context.getResources().createWebResourceSet(
                        WebResourceRoot.ResourceSetType.RESOURCE_JAR, "/", url, path);
            } catch (Exception ex) {
                // Ignore (probably not a directory)
            }
        }

        private boolean isInsideNestedJar(String dir) {
            return dir.indexOf("!/") < dir.lastIndexOf("!/");
        }
    }

    /**
     * Factory method called to create the {@link TomcatWebServer}. Subclasses can
     * override this method to return a different {@link TomcatWebServer} or apply
     * additional processing to the Tomcat server.
     *
     * @param tomcat the Tomcat server.
     * @return a new {@link TomcatWebServer} instance
     */
    protected WebServer getWebServer(Tomcat tomcat) {
        return new Sps4jTomcatWebServer(tomcat);
    }
}
