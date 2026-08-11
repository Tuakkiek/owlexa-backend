package com.owlexa.owlexabackend.modules.auth.dto.response;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String accessToken;

    private String phoneNumber;
    private String email;
    private String fullName;
    private String roleName;
    private String centerName;
    private Long centerId;
    private java.util.List<String> permissions;
}