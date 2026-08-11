package com.owlexa.owlexabackend.dto.admin;

import java.time.LocalDateTime;

public record AdminCenterResponse(
        Long id,
        String name,
        String subdomain,
        Long ownerId,
        String ownerName,
        String ownerPhoneNumber,
        long memberCount,
        LocalDateTime createdAt,
        boolean active
) {
}
