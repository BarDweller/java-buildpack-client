package dev.snowdrop.buildpack.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class HostAndSocketConfigTest {

    @Test
    void testHostAndSocketConfigBuilderAndGetters() {
        HostAndSocketConfig config = HostAndSocketConfig.builder()
            .withHost("localhost")
            .withSocket("/var/run/docker.sock")
            .build();

        assertNotNull(config);
        assertTrue(config.getHost().isPresent());
        assertEquals("localhost", config.getHost().get());
        assertTrue(config.getSocket().isPresent());
        assertEquals("/var/run/docker.sock", config.getSocket().get());
    }

    @Test
    void testHostAndSocketConfigEmpty() {
        HostAndSocketConfig config = HostAndSocketConfig.builder().build();

        assertNotNull(config);
        assertFalse(config.getHost().isPresent());
        assertFalse(config.getSocket().isPresent());
    }
}
