package com.owlexa.owlexabackend.dto.admin;

public record AdminUserResponse(
        Long id,
        String fullName,
        String phoneNumber,
        String email,
        String role,
        Long centerId,
        String centerName,
        boolean active
) {
}
