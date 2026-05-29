package com.owlexa.owlexabackend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRequest {

    @NotBlank(message="fullName is required")
    private String fullName;

    @Email(message="email must be valid")
    private String email;

    @NotBlank(message = "phoneNumber is required")
    @Pattern(regexp = "^0\\d{9}$", message = "phoneNumber must be a valid VN number (10 digits)")
    private String phoneNumber;
}
