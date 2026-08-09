package com.owlexa.owlexabackend.modules.student_submission.dto.response;

import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttemptStatus;
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
public class StudentAttemptSummaryResponse {

    private Long id;
    private Integer attemptNumber;
    private SubmissionAttemptStatus status;
    private Instant startedAt;
    private Instant lastSavedAt;
    private Instant submittedAt;
    private Instant expiresAt;
    private BigDecimal autoScore;
    private BigDecimal aiScore;
    private BigDecimal displayedScore;
    private BigDecimal maxScore;
}
