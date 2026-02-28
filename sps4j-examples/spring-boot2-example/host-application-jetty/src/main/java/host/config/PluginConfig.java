package host.config;

import com.github.zafarkhaja.semver.Version;
import io.github.sps4j.core.DefaultPluginManager;
import io.github.sps4j.core.PluginManager;
import io.github.sps4j.core.load.ProductPluginLoadService;
import io.github.sps4j.springboot2.loader.SpringAppSupportPluginLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

@Configuration
public class PluginConfig {
    @Autowired
    private ResourceLoader resourceLoader;

    @Bean
    public ProductPluginLoadService productPluginLoadService() {
        return () -> Version.parse("1.0.0");
    }

    @Bean
    public PluginManager loader(ProductPluginLoadService productPluginLoadService) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:plugin");
        return new DefaultPluginManager(resource.getURL().toString(),
                productPluginLoadService, new SpringAppSupportPluginLoader());
    }

}
