package com.owlexa.owlexabackend.modules.ai_scoring.listener;

import com.owlexa.owlexabackend.common.config.AiProperties;
import com.owlexa.owlexabackend.modules.ai_scoring.service.AiScoringService;
import com.owlexa.owlexabackend.modules.analytics.event.HomeworkSubmittedEvent;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkQuestionSubmission;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkQuestionType;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkQuestionSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Async listener that triggers AI scoring when a homework submission is finalized.
 * <p>
 * Only processes ESSAY-type question submissions. Returns immediately if:
 * <ul>
 *   <li>AI scoring is disabled in config</li>
 *   <li>The submission has no ESSAY questions</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiScoringEventListener {

    private final AiScoringService aiScoringService;
    private final HomeworkQuestionSubmissionRepository questionSubmissionRepository;
    private final AiProperties aiProperties;

    @Async("asyncTaskExecutor")
    @EventListener
    public void handleSubmitted(HomeworkSubmittedEvent event) {
        if (!aiProperties.isEnabled()) {
            log.debug("[AiScoringListener] AI scoring disabled. Skipping submission {}.", event.getSubmissionId());
            return;
        }

        log.info("[AiScoringListener] Received HomeworkSubmittedEvent for submissionId={}", event.getSubmissionId());

        List<HomeworkQuestionSubmission> essaySubmissions = questionSubmissionRepository
                .findBySubmission_IdAndQuestion_Type(event.getSubmissionId(), HomeworkQuestionType.ESSAY);

        if (essaySubmissions.isEmpty()) {
            log.debug("[AiScoringListener] No ESSAY questions in submissionId={}. Nothing to score.", event.getSubmissionId());
            return;
        }

        log.info("[AiScoringListener] Found {} ESSAY question(s) to score in submissionId={}.",
                essaySubmissions.size(), event.getSubmissionId());

        for (HomeworkQuestionSubmission qs : essaySubmissions) {
            try {
                aiScoringService.scoreEssaySubmission(qs.getId());
            } catch (Exception e) {
                // scoreEssaySubmission handles its own exception logging and status update;
                // catch here to ensure one failure doesn't block remaining questions.
                log.error("[AiScoringListener] Unexpected error scoring questionSubId={}: {}",
                        qs.getId(), e.getMessage());
            }
        }

        log.info("[AiScoringListener] Completed AI scoring round for submissionId={}", event.getSubmissionId());
    }
}
