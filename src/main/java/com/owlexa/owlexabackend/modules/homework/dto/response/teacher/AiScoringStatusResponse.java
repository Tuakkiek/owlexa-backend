package com.owlexa.owlexabackend.modules.homework.dto.response.teacher;

import com.owlexa.owlexabackend.modules.homework.enums.AiScoringStatus;
import lombok.Data;

import java.time.Instant;

/**
 * AI scoring status for a single question submission.
 * Returned as a list from the GET /ai-status endpoint.
 */
@Data
public class AiScoringStatusResponse {
    private Long questionSubmissionId;
    private AiScoringStatus aiScoringStatus;
    private Instant aiScoredAt;
}
