package com.owlexa.owlexabackend.dto.admin;

import java.time.LocalDateTime;

public record AdminAuditLogResponse(
        Long id,
        Long adminUserId,
        String adminName,
        String adminPhoneNumber,
        String action,
        String targetType,
        Long targetId,
        String targetName,
        String previousStatus,
        String newStatus,
        String reason,
        LocalDateTime createdAt
) {
}
