package io.github.sps4j.springboot2.web;

import io.github.sps4j.common.meta.MetaInfo;
import io.github.sps4j.core.load.Sps4jPluginClassLoader;
import io.github.sps4j.springboot2.context.PluginSpringbootBootstrapContext;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.core.annotation.Order;

import java.util.Optional;

@AllArgsConstructor
@Order
public class AddPluginServletContextPathCustomizer implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {
    private ServerProperties serverProperties;
    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        boolean host = !(Thread.currentThread().getContextClassLoader() instanceof Sps4jPluginClassLoader);
        if (!host) {
            String contextPath = serverProperties.getServlet().getContextPath();
            MetaInfo metaInfo = PluginSpringbootBootstrapContext.getCurrentPluginMetaInfo();
            factory.setContextPath("/"
                    + metaInfo.getDescriptor().getType()
                    + "/"
                    + metaInfo.getDescriptor().getName()
                    + Optional.ofNullable(contextPath).orElse(StringUtils.EMPTY)
            );
        }
    }
}
