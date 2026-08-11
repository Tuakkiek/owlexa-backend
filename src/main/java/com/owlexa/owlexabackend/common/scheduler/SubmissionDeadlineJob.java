package com.owlexa.owlexabackend.common.scheduler;

import com.owlexa.owlexabackend.modules.student_submission.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Safety-net job that finalizes IN_PROGRESS submission attempts whose
 * assignment deadline has passed. Handles the case where a student closes
 * their browser without submitting.
 *
 * <p>The primary deadline enforcement happens in the API layer
 * ({@link SubmissionService#saveAnswers}, {@link SubmissionService#submitAttempt},
 * etc.) — this scheduler is a secondary guarantee.
 *
 * <p>Idempotent: safe to run multiple times or overlap.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionDeadlineJob {

    private final SubmissionService submissionService;

    @Scheduled(fixedRateString = "${app.submission.deadline-check-interval-ms:300000}")
    public void finalizeExpiredAttempts() {
        Instant now = Instant.now();
        try {
            int count = submissionService.finalizeExpiredAttempts(now);
            if (count > 0) {
                log.info("SubmissionDeadlineJob: finalized {} expired attempts at {}", count, now);
            }
        } catch (Exception e) {
            log.error("SubmissionDeadlineJob: failed to finalize expired attempts", e);
        }
    }
}
