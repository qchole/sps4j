package io.github.sps4j.example.spring.webflux.plugin;


import io.github.sps4j.annotation.Sps4jPlugin;
import io.github.sps4j.example.api.GreeterPlugin;
import io.github.sps4j.example.spring.webflux.plugin.service.TestService;
import io.github.sps4j.springboot3.SpringBoot3AppPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Sps4jPlugin(version = "1.2.1", name = "spring-hello",
        productVersionConstraint = ">=1.0.0"
)
@SpringBootApplication
public class SpringHelloPlugin extends SpringBoot3AppPlugin implements GreeterPlugin {

    @Autowired
    private TestService testService;
    @Override
    public String greet(String name) {
        testService.test();
        return "Hello from Spring, " + name + "!";
    }
}
