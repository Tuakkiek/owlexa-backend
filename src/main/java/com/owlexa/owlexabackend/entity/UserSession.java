package com.owlexa.owlexabackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_sessions",
        indexes = {
                @Index(name = "idx_sessions_user_id",     columnList = "user_id"),
                @Index(name = "idx_sessions_id_active",   columnList = "id, is_active")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

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

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;
}
