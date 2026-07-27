package com.owlexa.owlexabackend.modules.teacher_review.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
public class TeacherReviewItemRequest {

    @NotNull(message = "Assignment item id is required")
    private Long assignmentItemId;

    @DecimalMin(value = "0.00", message = "Final score must be greater than or equal to 0")
    @Digits(integer = 6, fraction = 2, message = "Final score must have at most 6 integer and 2 decimal digits")
    private BigDecimal finalScore;

    private String itemComment;
}
