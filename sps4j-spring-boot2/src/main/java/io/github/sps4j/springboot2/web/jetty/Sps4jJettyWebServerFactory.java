package io.github.sps4j.springboot2.web.jetty;

import io.github.sps4j.core.load.Sps4jPluginClassLoader;
import io.github.sps4j.springboot2.context.HostApplicationContextHolder;
import lombok.SneakyThrows;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.FileSessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.boot.web.embedded.jetty.*;
import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Sps4jJettyWebServerFactory extends JettyServletWebServerFactory {

    @Override
    @SneakyThrows
    public WebServer getWebServer(ServletContextInitializer... initializers) {
        if (Thread.currentThread().getContextClassLoader() instanceof Sps4jPluginClassLoader) {
            ApplicationContext baseApplicationContext = HostApplicationContextHolder.getHostAppContext();
            Assert.isInstanceOf(ServletWebServerApplicationContext.class, baseApplicationContext);
            ServletWebServerApplicationContext webServerApplicationContext = (ServletWebServerApplicationContext) baseApplicationContext;
            JettyWebServer webServer = (JettyWebServer) webServerApplicationContext.getWebServer();
            Server jetty = webServer.getServer();
            WebAppContext context = new WebAppContext();
            configurePluginWebAppContext(context, initializers);
            ContextHandlerCollection handler = (ContextHandlerCollection) jetty.getHandler();
            Handler newHandler = addHandlerWrappers(context);
            handler.addHandler(newHandler);
            this.logger.info("Server initialized");
            return new Sps4jJettyWebServer(jetty, newHandler, handler);
        }
        else {
            return super.getWebServer(initializers);
        }
    }

    private void configureDocumentRoot(WebAppContext handler) {
        File root = getValidDocumentRoot();
        File docBase = (root != null) ? root : createTempDir("jetty-docbase");
        try {
            List<Resource> resources = new ArrayList<>();
            Resource rootResource = (docBase.isDirectory() ? Resource.newResource(docBase.getCanonicalFile())
                    : JarResource.newJarResource(Resource.newResource(docBase)));
            resources.add((root != null) ? new LoaderHidingResource(rootResource) : rootResource);
            for (URL resourceJarUrl : getUrlsOfJarsWithMetaInfResources()) {
                Resource resource = createResource(resourceJarUrl);
                if (resource.exists() && resource.isDirectory()) {
                    resources.add(resource);
                }
            }
            handler.setBaseResource(new ResourceCollection(resources.toArray(new Resource[0])));
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Resource createResource(URL url) throws Exception {
        if ("file".equals(url.getProtocol())) {
            File file = new File(url.toURI());
            if (file.isFile()) {
                return Resource.newResource("jar:" + url + "!/META-INF/resources");
            }
        }
        return Resource.newResource(url + "META-INF/resources");
    }

    private void configurePluginWebAppContext(WebAppContext context, ServletContextInitializer... initializers) {
        Assert.notNull(context, "Context must not be null");
        context.clearAliasChecks();
        context.setTempDirectory(new File(System.getProperty("java.io.tmpdir")));
        context.setClassLoader(Thread.currentThread().getContextClassLoader());
        String contextPath = getContextPath();
        context.setContextPath(StringUtils.hasLength(contextPath) ? contextPath : "/");
        context.setDisplayName(getDisplayName());
        configureDocumentRoot(context);
        if (isRegisterDefaultServlet()) {
            addDefaultServlet(context);
        }
        addLocaleMappings(context);
        ServletContextInitializer[] initializersToUse = mergeInitializers(initializers);
        Configuration[] configurations = getWebAppContextConfigurations(context, initializersToUse);
        context.setConfigurations(configurations);
        context.setThrowUnavailableOnStartupException(true);
        configureSession(context);
        postProcessWebAppContext(context);
    }

    private void configureSession(WebAppContext context) {
        SessionHandler handler = context.getSessionHandler();
        org.springframework.boot.web.server.Cookie.SameSite sessionSameSite = getSession().getCookie().getSameSite();
        if (sessionSameSite != null) {
            handler.setSameSite(HttpCookie.SameSite.valueOf(sessionSameSite.name()));
        }
        Duration sessionTimeout = getSession().getTimeout();
        handler.setMaxInactiveInterval((sessionTimeout == null || sessionTimeout.isNegative())? -1
                : (int) sessionTimeout.getSeconds());
        if (getSession().isPersistent()) {
            DefaultSessionCache cache = new DefaultSessionCache(handler);
            FileSessionDataStore store = new FileSessionDataStore();
            store.setStoreDir(getValidSessionStoreDir());
            cache.setSessionDataStore(store);
            handler.setSessionCache(cache);
        }
    }

    private void addLocaleMappings(WebAppContext context) {
        getLocaleCharsetMappings()
                .forEach((locale, charset) -> context.addLocaleEncoding(locale.toString(), charset.toString()));
    }


    @Override
    protected JettyWebServer getJettyWebServer(Server server) {
        Handler handler = server.getHandler();
        if (!(handler instanceof ContextHandlerCollection)) {
            ContextHandlerCollection hc = new ContextHandlerCollection();
            hc.addHandler(handler);
            server.setHandler(hc);
        }
        return super.getJettyWebServer(server);
    }

    private Handler addHandlerWrappers(Handler handler) {
        if (getCompression() != null && getCompression().getEnabled()) {
            handler = applyWrapper(handler, createGzipHandlerWrapper(getCompression()));
        }
        if (StringUtils.hasText(getServerHeader())) {
            handler = applyWrapper(handler, createServerHeaderHandlerWrapper(getServerHeader()));
        }
        if (!CollectionUtils.isEmpty(getCookieSameSiteSuppliers())) {
            handler = applyWrapper(handler, new SuppliedSameSiteCookieHandlerWrapper(getCookieSameSiteSuppliers()));
        }
        return handler;
    }

    private Handler applyWrapper(Handler handler, HandlerWrapper wrapper) {
        wrapper.setHandler(handler);
        return wrapper;
    }

    static HandlerWrapper createGzipHandlerWrapper(Compression compression) {
        GzipHandler handler = new GzipHandler();
        handler.setMinGzipSize((int) compression.getMinResponseSize().toBytes());
        handler.setIncludedMimeTypes(compression.getMimeTypes());
        for (HttpMethod httpMethod : HttpMethod.values()) {
            handler.addIncludedMethods(httpMethod.name());
        }
        if (compression.getExcludedUserAgents() != null) {
            try {
                handler.setExcludedAgentPatterns(compression.getExcludedUserAgents());
            }
            catch (NoSuchMethodError ex) {
                // Jetty 10 does not support User-Agent-based exclusions
            }
        }
        return handler;
    }

    static HandlerWrapper createServerHeaderHandlerWrapper(String header) {
        return new ServerHeaderHandler(header);
    }

    /**
     * {@link HandlerWrapper} to add a custom {@code server} header.
     */
    private static class ServerHeaderHandler extends HandlerWrapper {

        private static final String SERVER_HEADER = "server";

        private final String value;

        ServerHeaderHandler(String value) {
            this.value = value;
        }

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            if (!response.getHeaderNames().contains(SERVER_HEADER)) {
                response.setHeader(SERVER_HEADER, this.value);
            }
            super.handle(target, baseRequest, request, response);
        }
    }

    private static class SuppliedSameSiteCookieHandlerWrapper extends HandlerWrapper {

        private final List<CookieSameSiteSupplier> suppliers;

        SuppliedSameSiteCookieHandlerWrapper(List<CookieSameSiteSupplier> suppliers) {
            this.suppliers = suppliers;
        }

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            HttpServletResponse wrappedResponse = new SuppliedSameSiteCookieHandlerWrapper.ResponseWrapper(response);
            super.handle(target, baseRequest, request, wrappedResponse);
        }

        class ResponseWrapper extends HttpServletResponseWrapper {

            ResponseWrapper(HttpServletResponse response) {
                super(response);
            }

            @Override
            public void addCookie(Cookie cookie) {
                org.springframework.boot.web.server.Cookie.SameSite sameSite = getSameSite(cookie);
                if (sameSite != null) {
                    String comment = HttpCookie.getCommentWithoutAttributes(cookie.getComment());
                    String sameSiteComment = getSameSiteComment(sameSite);
                    cookie.setComment((comment != null) ? comment + sameSiteComment : sameSiteComment);
                }
                super.addCookie(cookie);
            }

            private String getSameSiteComment(org.springframework.boot.web.server.Cookie.SameSite sameSite) {
                switch (sameSite) {
                    case NONE:
                        return HttpCookie.SAME_SITE_NONE_COMMENT;
                    case LAX:
                        return HttpCookie.SAME_SITE_LAX_COMMENT;
                    case STRICT:
                        return HttpCookie.SAME_SITE_STRICT_COMMENT;
                }
                throw new IllegalStateException("Unsupported SameSite value " + sameSite);
            }

            private org.springframework.boot.web.server.Cookie.SameSite getSameSite(Cookie cookie) {
                for (CookieSameSiteSupplier supplier : SuppliedSameSiteCookieHandlerWrapper.this.suppliers) {
                    org.springframework.boot.web.server.Cookie.SameSite sameSite = supplier.getSameSite(cookie);
                    if (sameSite != null) {
                        return sameSite;
                    }
                }
                return null;
            }

        }

    }

    private static final class LoaderHidingResource extends Resource {

        private final Resource delegate;

        private LoaderHidingResource(Resource delegate) {
            this.delegate = delegate;
        }

        @Override
        public Resource addPath(String path) throws IOException {
            if (path.startsWith("/org/springframework/boot")) {
                return null;
            }
            return this.delegate.addPath(path);
        }

        @Override
        public boolean isContainedIn(Resource resource) throws MalformedURLException {
            return this.delegate.isContainedIn(resource);
        }

        @Override
        public void close() {
            this.delegate.close();
        }

        @Override
        public boolean exists() {
            return this.delegate.exists();
        }

        @Override
        public boolean isDirectory() {
            return this.delegate.isDirectory();
        }

        @Override
        public long lastModified() {
            return this.delegate.lastModified();
        }

        @Override
        public long length() {
            return this.delegate.length();
        }

        @Override
        @Deprecated
        public URL getURL() {
            return this.delegate.getURL();
        }

        @Override
        public File getFile() throws IOException {
            return this.delegate.getFile();
        }

        @Override
        public String getName() {
            return this.delegate.getName();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return this.delegate.getInputStream();
        }

        @Override
        public ReadableByteChannel getReadableByteChannel() throws IOException {
            return this.delegate.getReadableByteChannel();
        }

        @Override
        public boolean delete() throws SecurityException {
            return this.delegate.delete();
        }

        @Override
        public boolean renameTo(Resource dest) throws SecurityException {
            return this.delegate.renameTo(dest);
        }

        @Override
        public String[] list() {
            return this.delegate.list();
        }

    }
}
