package com.owlexa.owlexabackend.modules.enrollment.dto.request;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EnrollmentRequest {

    @NotNull(message = "studentId is required")
    private Long studentId;
}
