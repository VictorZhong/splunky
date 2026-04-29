package com.wpb.spky.persistence;

import com.wpb.spky.config.SplunkyProperties;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class SecretCipherTest {

    @Test
    void encryptsAndDecryptsSecret() {
        SecretCipher cipher = new SecretCipher(properties());

        String encrypted = cipher.encrypt("secret-value");

        assertThat(encrypted).doesNotContain("secret-value");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("secret-value");
    }

    @Test
    void fingerprintDoesNotExposeSecret() {
        String fingerprint = SecretCipher.fingerprint("secret-value");

        assertThat(fingerprint).isNotBlank();
        assertThat(fingerprint).doesNotContain("secret-value");
    }

    private static SplunkyProperties properties() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return new SplunkyProperties(
                new SplunkyProperties.PersistenceProperties(true),
                new SplunkyProperties.CryptoProperties(Base64.getEncoder().encodeToString(key)),
                null,
                null
        );
    }
}
