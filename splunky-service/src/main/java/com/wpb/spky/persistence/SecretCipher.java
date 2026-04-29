package com.wpb.spky.persistence;

import com.wpb.spky.config.SplunkyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class SecretCipher {

    private static final Logger log = LoggerFactory.getLogger(SecretCipher.class);
    private static final String PREFIX = "v1";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec key;

    public SecretCipher(SplunkyProperties properties) {
        this.key = new SecretKeySpec(resolveKey(properties.cryptoSecretKey()), "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("Secret plaintext must not be blank.");
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            Base64.Encoder encoder = Base64.getEncoder();
            return PREFIX + ":" + encoder.encodeToString(iv) + ":" + encoder.encodeToString(encrypted);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt secret.", ex);
        }
    }

    public String decrypt(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isBlank()) return null;
        String[] parts = encryptedValue.split(":", 3);
        if (parts.length != 3 || !PREFIX.equals(parts[0])) {
            throw new IllegalArgumentException("Unsupported encrypted secret format.");
        }
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] iv = decoder.decode(parts[1]);
            byte[] encrypted = decoder.decode(parts[2]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt secret.", ex);
        }
    }

    private static byte[] resolveKey(String configured) {
        if (configured != null && !configured.isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(configured);
            if (decoded.length != 32) {
                throw new IllegalArgumentException("SPKY_SECRET_KEY must decode to exactly 32 bytes.");
            }
            return decoded;
        }

        log.warn("SPKY_SECRET_KEY is not configured; using local development credential encryption key.");
        return Arrays.copyOf(sha256("splunky-local-development-secret-key"), 32);
    }

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
