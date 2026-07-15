package dev.snowdrop.buildpack.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class RegistryAuthConfigTest {

    @Test
    void testRegistryAuthConfigBuilderAndGetters() {
        RegistryAuthConfig config = RegistryAuthConfig.builder()
            .withRegistryAddress("https://registry.example.com")
            .withRegistryToken("token-xyz")
            .withUsername("test-user")
            .withAuth("auth-token")
            .withEmail("user@example.com")
            .withIdentityToken("identity-token-123")
            .withPassword("secret-pass")
            .build();

        assertNotNull(config);
        assertEquals("https://registry.example.com", config.getRegistryAddress());
        assertEquals("token-xyz", config.getRegistryToken());
        assertEquals("test-user", config.getUsername());
        assertEquals("auth-token", config.getAuth());
        assertEquals("user@example.com", config.getEmail());
        assertEquals("identity-token-123", config.getIdentityToken());
        assertEquals("secret-pass", config.getPassword());
    }
}
