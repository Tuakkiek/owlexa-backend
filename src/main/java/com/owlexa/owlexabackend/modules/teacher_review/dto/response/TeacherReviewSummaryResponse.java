package com.owlexa.owlexabackend.modules.teacher_review.dto.response;

import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttemptStatus;
import com.owlexa.owlexabackend.modules.teacher_review.entity.TeacherReviewStatus;
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
public class TeacherReviewSummaryResponse {

    private Long submissionAttemptId;
    private Long assignmentId;
    private Long assignmentRecipientId;
    private Long studentUserId;
    private String studentFullName;
    private Long classId;
    private String className;
    private Integer attemptNumber;
    private SubmissionAttemptStatus submissionStatus;
    private Instant submittedAt;
    private Long reviewId;
    private TeacherReviewStatus reviewStatus;
    private boolean hasEssay;
    private boolean hasAiResult;
    private Long selectedAiGradingResultId;
    private BigDecimal autoScore;
    private BigDecimal finalScore;
    private BigDecimal maxScore;
}
