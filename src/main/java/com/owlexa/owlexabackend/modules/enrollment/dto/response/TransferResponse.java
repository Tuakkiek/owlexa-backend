package com.owlexa.owlexabackend.modules.enrollment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponse {
    private EnrollmentResponse oldEnrollment;
    private EnrollmentResponse newEnrollment;
    private BigDecimal feeDifference;
}
