package com.owlexa.owlexabackend.modules.ai_grading.dto.response;

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
public class AIGradingResultResponse {

    private Long id;
    private Long jobId;
    private Long submissionAttemptId;
    private String summary;
    private String overallFeedback;
    private BigDecimal aiScore;
    private BigDecimal maxScore;
    private BigDecimal confidence;
    private List<AIGradingItemResultResponse> itemResults;
    private Instant createdAt;
    private Instant updatedAt;
}
