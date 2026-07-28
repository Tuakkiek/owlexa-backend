package com.owlexa.owlexabackend.modules.assessment_builder.dto.request;

import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentType;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.PlaybackMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentRequest {

    @NotBlank(message = "Assessment title is required")
    @Size(max = 255, message = "Assessment title must not exceed 255 characters")
    private String title;

    private String description;

    private JsonNode content;

    @NotNull(message = "Assessment type is required")
    private AssessmentType type;

    private Long audioFileId;

    private PlaybackMode playbackMode;

    @Valid
    private List<AssessmentItemRequest> items;
}
