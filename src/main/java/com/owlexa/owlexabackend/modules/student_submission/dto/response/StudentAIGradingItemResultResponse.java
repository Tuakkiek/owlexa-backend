package com.owlexa.owlexabackend.modules.student_submission.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * AI grading feedback for a single question, exposed to students only when
 * the assignment has "show score" enabled.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAIGradingItemResultResponse {

    private Long id;
    private Long assignmentItemId;
    private BigDecimal aiScore;
    private BigDecimal maxScore;
    private String feedback;
    private String rubricAnalysis;
    private BigDecimal confidence;
}
