package com.owlexa.owlexabackend.modules.assessment_builder.dto.response;

import com.owlexa.owlexabackend.common.assessment_document.AssessmentBlockType;
import com.owlexa.owlexabackend.common.assessment_document.BlockAlignment;
import com.owlexa.owlexabackend.modules.file.dto.FileResponse;
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
public class AssessmentBlockResponse {

    private Long id;
    private AssessmentBlockType blockType;
    private Integer position;
    private JsonNode content;
    private FileResponse file;
    private String caption;
    private BlockAlignment alignment;
    private Long questionId;
    private BigDecimal points;
    private AssessmentQuestionPreviewResponse questionPreview;
}
