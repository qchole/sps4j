package com.github.sps4j.annotation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PluginProcessorTest {

    @Test
    void isValidVersionConstraint() {
        assertTrue(PluginProcessor.isValidVersionConstraint("=1.0.0"));
        assertFalse(PluginProcessor.isValidVersionConstraint("==1.0.0"));
        assertTrue(PluginProcessor.isValidVersionConstraint(">1.0"));
        assertTrue(PluginProcessor.isValidVersionConstraint(">1.0 && < 1.2"));
    }
}