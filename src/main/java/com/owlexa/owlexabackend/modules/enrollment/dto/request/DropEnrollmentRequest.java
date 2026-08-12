package com.owlexa.owlexabackend.modules.enrollment.dto.request;

import com.owlexa.owlexabackend.modules.enrollment.entity.DropReason;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DropEnrollmentRequest {
    @NotNull
    private DropReason reason;
    private String note;
}
