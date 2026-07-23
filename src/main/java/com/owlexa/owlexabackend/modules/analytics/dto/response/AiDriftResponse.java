package com.owlexa.owlexabackend.modules.analytics.dto.response;

import lombok.Data;

/**
 * AI vs Teacher scoring drift for a single rubric criterion.
 * Returned by GET /teacher/analytics/classes/{classId}/ai-drift.
 */
@Data
public class AiDriftResponse {
    private Long criterionId;
    private String criterionName;
    /** Rolling average of AI-assigned scores for this criterion. */
    private Double aiAvg;
    /** Rolling average of teacher override scores for this criterion. */
    private Double teacherAvg;
    /**
     * Drift rate = |aiAvg - teacherAvg| / maxScore.
     * Null if AI and teacher averages are not both available yet.
     */
    private Double driftRate;
    private Double maxScore;
}
