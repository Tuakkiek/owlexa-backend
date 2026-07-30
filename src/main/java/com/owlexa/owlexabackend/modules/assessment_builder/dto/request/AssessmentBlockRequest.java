package com.owlexa.owlexabackend.modules.assessment_builder.dto.request;

import com.owlexa.owlexabackend.common.assessment_document.AssessmentBlockType;
import com.owlexa.owlexabackend.common.assessment_document.BlockAlignment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentBlockRequest {

    @Positive(message = "Assessment block ID must be greater than zero")
    private Long id;

    @NotNull(message = "Assessment block type is required")
    private AssessmentBlockType blockType;

    private JsonNode content;

    @Positive(message = "File ID must be greater than zero")
    private Long fileId;

    @Size(max = 1000, message = "Block caption must not exceed 1000 characters")
    private String caption;

    private BlockAlignment alignment;

    @Positive(message = "Question ID must be greater than zero")
    private Long questionId;

    @Positive(message = "Question block points must be greater than zero")
    private BigDecimal points;
}
