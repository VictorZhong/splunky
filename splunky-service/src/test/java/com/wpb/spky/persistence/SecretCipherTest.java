package com.wpb.spky.persistence;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretCipherTest {

    @Test
    void fingerprintDoesNotExposeSecret() {
        String fingerprint = SecretCipher.fingerprint("secret-value");

        assertThat(fingerprint).isNotBlank();
        assertThat(fingerprint).doesNotContain("secret-value");
    }
}
