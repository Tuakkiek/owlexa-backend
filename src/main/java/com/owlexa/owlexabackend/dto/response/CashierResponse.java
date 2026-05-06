package com.owlexa.owlexabackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashierResponse {
    private Long userId;
    private String fullName;
    private String phoneNumber;
    private Long centerId;
    private String temporaryPassword;
}
