package com.owlexa.owlexabackend.modules.analytics.dto.response;

import lombok.Data;

@Data
public class AnalyticsClassPerformanceResponse {
    private Long id;
    private Long classId;
    private Long homeworkId;
    private Integer submittedCount;
    private Integer gradedCount;
    private Integer lateSubmissionCount;
    private Integer missingSubmissionCount;
    private Double averageScore;
    private Double highestScore;
    private Double lowestScore;
    private Double passRate;
    private String updatedAt;
}
