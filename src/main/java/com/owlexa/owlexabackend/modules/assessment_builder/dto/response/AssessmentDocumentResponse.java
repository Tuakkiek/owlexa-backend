package com.owlexa.owlexabackend.modules.assessment_builder.dto.response;

import com.owlexa.owlexabackend.common.assessment_document.AssessmentDocumentFormat;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentStatus;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentDocumentResponse {

    private Long id;
    private String title;
    private AssessmentType type;
    private AssessmentStatus status;
    private AssessmentDocumentFormat documentFormat;
    private Long version;
    private List<AssessmentBlockResponse> blocks;
    private long totalQuestions;
    private BigDecimal totalPoints;
    private Instant createdAt;
    private Instant updatedAt;
}
