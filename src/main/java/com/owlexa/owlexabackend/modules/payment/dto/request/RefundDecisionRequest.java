package com.owlexa.owlexabackend.modules.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundDecisionRequest {
    @NotNull
    private Boolean approve;
    private String rejectedReason;
}
