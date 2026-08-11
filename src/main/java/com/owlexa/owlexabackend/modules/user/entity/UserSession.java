package com.owlexa.owlexabackend.modules.user.entity;

import com.owlexa.owlexabackend.common.context.TenantAware;
import com.owlexa.owlexabackend.modules.user.entity.DeviceType;
import com.owlexa.owlexabackend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import com.owlexa.owlexabackend.common.listener.TenantEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions",
        indexes = {
                @Index(name = "idx_sessions_user_id", columnList = "user_id"),
                @Index(name = "idx_sessions_id_active", columnList = "id, is_active"),
                @Index(name = "idx_sessions_user_device", columnList = "user_id, device_key"),
                @Index(name = "idx_sessions_cleanup", columnList = "is_active, last_used_at")
        }
)
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "center_id = :tenantId")
@EntityListeners(TenantEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession implements TenantAware {

    @Id
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(name = "refresh_token_hash", nullable = false, length = 88)
    private String refreshTokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id")
    private Center center;

    @Column(name = "device_name", length = 120)
    private String deviceName;

    @Convert(converter = DeviceTypeConverter.class)
    @Column(name = "device_type", length = 20)
    private DeviceType deviceType;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Lob
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * Stable device identifier = SHA-256(userId + "|" + userAgent).
     * Used to deduplicate sessions: same device → reuse, new device → create.
     */
    @Column(name = "device_key", length = 64)
    private String deviceKey;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * Sliding expiration: updated to now + inactiveTimeout on every successful refresh.
     * Maps to existing {@code expired_at} column for backward compatibility.
     */
    @Column(name = "expired_at", nullable = false)
    private LocalDateTime inactiveExpireAt;

    /**
     * Absolute expiration: set to createdAt + absoluteTimeout on login.
     * Never extended. User MUST re-login after this date.
     */
    @Column(name = "absolute_expire_at", nullable = false)
    private LocalDateTime absoluteExpireAt;

    /**
     * Number of times the refresh token has been rotated for this session.
     * Useful for anomaly detection (e.g. 1000 rotations/hour = suspicious).
     */
    @Column(name = "rotation_count", nullable = false)
    @Builder.Default
    private int rotationCount = 0;

    /**
     * Why this session was revoked.
     * Values: USER_LOGOUT, MANUAL_REVOKE, MANUAL_REVOKE_ALL,
     *         REUSE_DETECTED, LIMIT_EXCEEDED, PASSWORD_CHANGED
     */
    @Column(name = "revoked_reason", length = 50)
    private String revokedReason;

    /**
     * When this session was revoked. Set alongside revokedReason.
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Override
    public Long getCenterId() {
        return center != null ? center.getId() : null;
    }
}
