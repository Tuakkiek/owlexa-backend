package com.owlexa.owlexabackend.modules.ai_grading.dto.response;

import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingJobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIGradingJobSummaryResponse {

    private Long id;
    private Long submissionAttemptId;
    private AIGradingJobStatus status;
    private Long requestedByUserId;
    private String requestedByFullName;
    private Long resultId;
    private Instant startedAt;
    private Instant completedAt;
    private Instant failedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
