package com.wpb.spky.persistence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public final class SecretCipher {

    private SecretCipher() {}

    public static String fingerprint(String secret) {
        if (secret == null || secret.isBlank()) return null;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Arrays.copyOf(sha256(secret), 12));
    }

    private static byte[] sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }
}
