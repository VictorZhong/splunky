package com.wpb.spky.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "spky_user_account")
public class SpkyUserAccountEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_by_staff_id")
    private String createdByStaffId;

    @Column(name = "updated_by_staff_id")
    private String updatedByStaffId;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public String getCreatedByStaffId() { return createdByStaffId; }
    public void setCreatedByStaffId(String createdByStaffId) { this.createdByStaffId = createdByStaffId; }
    public String getUpdatedByStaffId() { return updatedByStaffId; }
    public void setUpdatedByStaffId(String updatedByStaffId) { this.updatedByStaffId = updatedByStaffId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
