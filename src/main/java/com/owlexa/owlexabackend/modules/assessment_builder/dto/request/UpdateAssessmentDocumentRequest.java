package com.owlexa.owlexabackend.modules.assessment_builder.dto.request;

import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAssessmentDocumentRequest {

    @NotBlank(message = "Assessment title is required")
    @Size(max = 255, message = "Assessment title must not exceed 255 characters")
    private String title;

    @NotNull(message = "Assessment type is required")
    private AssessmentType type;

    @NotNull(message = "Assessment version is required")
    @PositiveOrZero(message = "Assessment version must not be negative")
    private Long version;

    @NotNull(message = "Assessment document blocks are required")
    @Valid
    private List<AssessmentBlockRequest> blocks;
}
