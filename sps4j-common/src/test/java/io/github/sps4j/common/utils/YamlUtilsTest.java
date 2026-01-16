package io.github.sps4j.common.utils;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class YamlUtilsTest {

    @Test
    void testGetYamlMapper() {
        YAMLMapper yamlMapper = YamlUtils.getYamlMapper();
        assertSame(yamlMapper, YamlUtils.getYamlMapper());
    }



}