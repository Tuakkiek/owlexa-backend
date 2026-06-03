package com.owlexa.owlexabackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentResponse {
    private Long userId;
    private String phoneNumber;
    private String fullName;
    private Long centerId;
    private String temporaryPassword;
}
