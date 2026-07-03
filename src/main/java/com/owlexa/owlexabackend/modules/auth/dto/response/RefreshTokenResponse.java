package com.owlexa.owlexabackend.modules.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefreshTokenResponse {
    private String refreshToken;
    private AuthResponse auth;
}
