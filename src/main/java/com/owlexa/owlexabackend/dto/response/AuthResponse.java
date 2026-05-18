package com.owlexa.owlexabackend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String phoneNumber;
    private String email;
    private String fullName;
    private String roleName;
    private String centerName;
}

