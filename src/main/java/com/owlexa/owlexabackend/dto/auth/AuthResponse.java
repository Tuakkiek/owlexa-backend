package com.owlexa.owlexabackend.dto.auth;

import java.util.List;

public record AuthResponse(
        String accessToken,
        Long userId,
        String phoneNumber,
        String email,
        String fullName,
        String roleName,
        String centerName,
        Long centerId,
        List<String> permissions
) {
}
