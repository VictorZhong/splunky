package com.wpb.spky.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "spky_llm_credential")
public class SpkyLlmCredentialEntity {

    @Id
    @Column(name = "credential_id", nullable = false)
    private UUID credentialId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "credential_type", nullable = false)
    private String credentialType;

    @Column(name = "encrypted_secret", nullable = false)
    private String encryptedSecret;

    @Column(name = "secret_fingerprint")
    private String secretFingerprint;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_refreshed_at")
    private Instant lastRefreshedAt;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public UUID getCredentialId() { return credentialId; }
    public void setCredentialId(UUID credentialId) { this.credentialId = credentialId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getCredentialType() { return credentialType; }
    public void setCredentialType(String credentialType) { this.credentialType = credentialType; }
    public String getEncryptedSecret() { return encryptedSecret; }
    public void setEncryptedSecret(String encryptedSecret) { this.encryptedSecret = encryptedSecret; }
    public String getSecretFingerprint() { return secretFingerprint; }
    public void setSecretFingerprint(String secretFingerprint) { this.secretFingerprint = secretFingerprint; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getLastRefreshedAt() { return lastRefreshedAt; }
    public void setLastRefreshedAt(Instant lastRefreshedAt) { this.lastRefreshedAt = lastRefreshedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
