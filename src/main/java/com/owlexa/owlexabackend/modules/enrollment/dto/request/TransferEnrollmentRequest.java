package com.owlexa.owlexabackend.modules.enrollment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferEnrollmentRequest {
    @NotNull
    private Long targetClassId;
    private String note;
}
