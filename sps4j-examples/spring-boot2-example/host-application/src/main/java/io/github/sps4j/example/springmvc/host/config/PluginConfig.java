package io.github.sps4j.example.springmvc.host.config;

import io.github.sps4j.core.DefaultPluginManager;
import io.github.sps4j.core.PluginManager;
import io.github.sps4j.core.load.ProductPluginLoadService;
import io.github.sps4j.springboot2.loader.SpringAppSupportPluginLoader;
import com.github.zafarkhaja.semver.Version;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.util.Arrays;

@Configuration
public class PluginConfig {
    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public ProductPluginLoadService productPluginLoadService() {
        return () -> Version.parse("1.0.0");
    }

    @Bean
    public PluginManager loader(ProductPluginLoadService productPluginLoadService) throws IOException {
        Resource[] resources = applicationContext.getResources("classpath*:plugin");
        return new DefaultPluginManager(Arrays.stream(resources).filter(Resource::exists).findFirst()
                .map(resource -> {
                    try {
                        return resource.getURL().toString();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseThrow(() -> new IllegalStateException("plugin dir not found")),
                productPluginLoadService, new SpringAppSupportPluginLoader());
    }

}
