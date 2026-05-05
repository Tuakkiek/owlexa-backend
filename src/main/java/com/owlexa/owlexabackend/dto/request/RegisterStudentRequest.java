package com.owlexa.owlexabackend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterStudentRequest {
    @NotBlank(message = "phoneNumber is required")
    @Pattern(regexp = "^0\\d{9}$", message = "phoneNumber must be a valid VN number (10 digits)")
    private String phoneNumber;

    @Email(message = "email must be valid")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "password must be at least 8 characters")
    private String password;

    @NotBlank(message = "fullName is required")
    private String fullName;
}
