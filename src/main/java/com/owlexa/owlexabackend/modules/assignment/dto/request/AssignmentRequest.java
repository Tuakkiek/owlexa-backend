package com.owlexa.owlexabackend.modules.assignment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentRequest {

    @NotNull(message = "Assessment id is required")
    private Long assessmentId;

    @NotBlank(message = "Assignment title is required")
    @Size(max = 255, message = "Assignment title must not exceed 255 characters")
    private String title;

    private String description;

    private Instant openAt;

    private Instant dueAt;

    @Min(value = 1, message = "Attempt limit must be greater than or equal to 1")
    private Integer attemptLimit;

    @Min(value = 1, message = "Time limit must be greater than or equal to 1")
    private Integer timeLimitMinutes;

    private Boolean showScore;

    private Boolean allowReview;

    private String accessPassword;

    @Valid
    private List<AssignmentTargetRequest> targets;
}
