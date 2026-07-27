package com.owlexa.owlexabackend.modules.ai_grading.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIGradingItemResultResponse {

    private Long id;
    private Long submissionAnswerId;
    private Long assignmentItemId;
    private BigDecimal aiScore;
    private BigDecimal maxScore;
    private String feedback;
    private String rubricAnalysis;
    private BigDecimal confidence;
    private Instant createdAt;
    private Instant updatedAt;
}
