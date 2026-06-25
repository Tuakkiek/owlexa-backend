package com.owlexa.owlexabackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    private String password;

    private String deviceName;

    private String deviceType;
}
