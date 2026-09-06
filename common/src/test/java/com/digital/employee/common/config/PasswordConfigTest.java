package com.digital.employee.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordConfigTest {

    private final PasswordConfig config = new PasswordConfig();

    @Test
    void passwordEncoderShouldBeBCrypt() {
        PasswordEncoder encoder = config.passwordEncoder();
        assertInstanceOf(BCryptPasswordEncoder.class, encoder);
    }

    @Test
    void encodeAndVerifyPassword() {
        PasswordEncoder encoder = config.passwordEncoder();
        String raw = "Test@12345";
        String hashed = encoder.encode(raw);

        assertNotNull(hashed);
        assertTrue(encoder.matches(raw, hashed));
        assertFalse(encoder.matches("wrong", hashed));
    }

    @Test
    void samePasswordDifferentHashes() {
        PasswordEncoder encoder = config.passwordEncoder();
        String raw = "Test@12345";
        String hash1 = encoder.encode(raw);
        String hash2 = encoder.encode(raw);

        assertNotEquals(hash1, hash2);
        assertTrue(encoder.matches(raw, hash1));
        assertTrue(encoder.matches(raw, hash2));
    }
}
