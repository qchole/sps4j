package io.github.sps4j.springboot2.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sps4j.spring-mvc")
@Getter
@Setter
@ToString
public class Sps4jSpringMvcProperties {
    private boolean addServletContextPrefix = true;
}
