package com.owlexa.owlexabackend.modules.student_submission.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * AI grading result exposed to students. Only populated when the assignment
 * has "show score" enabled and a completed AI grading result exists, so
 * students can immediately see the AI feedback based on the teacher's
 * attached grading criteria. Teachers can always review the full result.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAIGradingResultResponse {

    private Long resultId;
    private Long jobId;
    private String summary;
    private String overallFeedback;
    private String focusArea;
    private BigDecimal aiScore;
    private BigDecimal maxScore;
    private BigDecimal confidence;
    private Instant createdAt;
    private List<StudentAIGradingCriterionResultResponse> criteria;
    private List<StudentAIGradingImprovementResponse> improvements;
    private List<StudentAIGradingItemResultResponse> itemResults;
}
