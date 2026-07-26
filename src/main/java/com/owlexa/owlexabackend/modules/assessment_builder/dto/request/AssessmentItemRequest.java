package com.owlexa.owlexabackend.modules.assessment_builder.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentItemRequest {

    @NotNull(message = "Question id is required")
    private Long questionId;

    @DecimalMin(value = "0.01", message = "Item points must be greater than 0")
    private BigDecimal points;

    @NotNull(message = "Display order is required")
    @Min(value = 1, message = "Display order must be greater than or equal to 1")
    private Integer displayOrder;
}
