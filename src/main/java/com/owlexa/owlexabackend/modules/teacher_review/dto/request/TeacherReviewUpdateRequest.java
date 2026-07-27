package com.owlexa.owlexabackend.modules.teacher_review.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherReviewUpdateRequest {

    @NotNull(message = "Version is required")
    @PositiveOrZero(message = "Version must be greater than or equal to 0")
    private Long version;

    private Long selectedAiGradingResultId;

    private String overallComment;

    @NotNull(message = "Review items are required")
    @Valid
    private List<TeacherReviewItemRequest> items;
}
