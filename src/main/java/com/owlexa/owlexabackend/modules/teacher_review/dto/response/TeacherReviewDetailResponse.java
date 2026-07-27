package com.owlexa.owlexabackend.modules.teacher_review.dto.response;

import com.owlexa.owlexabackend.modules.teacher_review.entity.TeacherReviewStatus;
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
public class TeacherReviewDetailResponse {

    private Long id;
    private Long submissionAttemptId;
    private Long selectedAiGradingResultId;
    private TeacherReviewStatus status;
    private String overallComment;
    private BigDecimal autoScore;
    private BigDecimal finalScore;
    private BigDecimal maxScore;
    private Long version;
    private List<TeacherReviewItemResponse> items;
    private Long createdByUserId;
    private String createdByFullName;
    private Long updatedByUserId;
    private String updatedByFullName;
    private Long finalizedByUserId;
    private String finalizedByFullName;
    private Instant finalizedAt;
    private Long releasedByUserId;
    private String releasedByFullName;
    private Instant releasedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
