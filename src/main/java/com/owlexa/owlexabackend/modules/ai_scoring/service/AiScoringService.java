package com.owlexa.owlexabackend.modules.ai_scoring.service;

import com.owlexa.owlexabackend.common.config.AiProperties;
import com.owlexa.owlexabackend.modules.ai_scoring.dto.AiCriterionResult;
import com.owlexa.owlexabackend.modules.ai_scoring.entity.AiScoringJob;
import com.owlexa.owlexabackend.modules.ai_scoring.gateway.AiScoringException;
import com.owlexa.owlexabackend.modules.ai_scoring.gateway.AiScoringGateway;
import com.owlexa.owlexabackend.modules.ai_scoring.repository.AiScoringJobRepository;
import com.owlexa.owlexabackend.modules.analytics.event.AiScoringCompletedEvent;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkQuestion;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkQuestionSubmission;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkRubric;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkRubricCriterion;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkRubricCriterionScore;
import com.owlexa.owlexabackend.modules.homework.enums.AiScoringStatus;
import com.owlexa.owlexabackend.modules.homework.enums.GraderType;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkQuestionType;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkQuestionSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Core AI scoring service.
 * <p>
 * For each ESSAY question submission:
 * <ol>
 *   <li>Validates that the question has a rubric with criteria.</li>
 *   <li>Marks status as IN_PROGRESS.</li>
 *   <li>Calls {@link AiScoringGateway} once per criterion with a structured prompt.</li>
 *   <li>Persists {@link HomeworkRubricCriterionScore} entries with {@code graderType = AI}.</li>
 *   <li>Sums scores → sets {@code questionSubmission.score} and concatenates feedback → {@code aiFeedback}.</li>
 *   <li>Logs an {@link AiScoringJob} record for auditability.</li>
 *   <li>Publishes {@link AiScoringCompletedEvent} for drift analytics.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiScoringService {

    private final HomeworkQuestionSubmissionRepository questionSubmissionRepository;
    private final AiScoringJobRepository aiScoringJobRepository;
    private final AiScoringGateway aiScoringGateway;
    private final AiProperties aiProperties;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Async re-score entry point for teacher-triggered re-scoring.
     * Runs on the shared async executor so the HTTP response is returned immediately (202).
     */
    @Async("asyncTaskExecutor")
    public void rescoreEssaySubmissionAsync(Long questionSubmissionId) {
        scoreEssaySubmission(questionSubmissionId);
    }

    /**
     * Scores a single essay question submission using AI.
     * Runs in its own transaction so failures don't roll back the parent submission transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scoreEssaySubmission(Long questionSubmissionId) {
        log.info("[AiScoring] Starting AI scoring for questionSubmissionId={}", questionSubmissionId);

        HomeworkQuestionSubmission qs = questionSubmissionRepository
                .findWithRubricDetailsById(questionSubmissionId)
                .orElseThrow(() -> {
                    log.error("[AiScoring] QuestionSubmission not found: {}", questionSubmissionId);
                    return new IllegalArgumentException("QuestionSubmission not found: " + questionSubmissionId);
                });

        // ── Guard: only ESSAY questions are AI-scored ──
        if (qs.getQuestion().getType() != HomeworkQuestionType.ESSAY) {
            log.debug("[AiScoring] Skipping non-ESSAY question {}", questionSubmissionId);
            qs.setAiScoringStatus(AiScoringStatus.SKIPPED);
            questionSubmissionRepository.save(qs);
            return;
        }

        // ── Guard: AI disabled in config ──
        if (!aiProperties.isEnabled()) {
            log.info("[AiScoring] AI disabled in config. Skipping questionSubmissionId={}", questionSubmissionId);
            qs.setAiScoringStatus(AiScoringStatus.SKIPPED);
            questionSubmissionRepository.save(qs);
            return;
        }

        // ── Guard: rubric must exist ──
        HomeworkQuestion question = qs.getQuestion();
        HomeworkRubric rubric = question.getRubric();
        if (rubric == null || rubric.getCriteria() == null || rubric.getCriteria().isEmpty()) {
            log.warn("[AiScoring] No rubric/criteria for question {}. Skipping.", question.getId());
            qs.setAiScoringStatus(AiScoringStatus.SKIPPED);
            questionSubmissionRepository.save(qs);
            return;
        }

        // ── Create/update job record ──
        AiScoringJob job = AiScoringJob.builder()
                .submissionId(qs.getSubmission().getId())
                .questionSubId(questionSubmissionId)
                .centerId(qs.getSubmission().getCenterId())
                .status(AiScoringStatus.IN_PROGRESS)
                .attemptCount(1)
                .modelUsed(aiProperties.getModel())
                .build();
        job = aiScoringJobRepository.save(job);

        // ── Mark IN_PROGRESS ──
        qs.setAiScoringStatus(AiScoringStatus.IN_PROGRESS);
        questionSubmissionRepository.save(qs);

        try {
            String studentAnswer = qs.getTextAnswer() != null ? qs.getTextAnswer() : "(No answer provided)";
            double totalScore = 0.0;
            StringJoiner feedbackJoiner = new StringJoiner(" | ");
            List<HomeworkRubricCriterionScore> aiScores = new ArrayList<>();

            for (HomeworkRubricCriterion criterion : rubric.getCriteria()) {
                String prompt = buildPrompt(question.getQuestionText(), criterion, studentAnswer);
                log.debug("[AiScoring] Scoring criterion '{}' for questionSubId={}", criterion.getName(), questionSubmissionId);

                AiCriterionResult result = aiScoringGateway.scoreCriterion(prompt);

                // Cap score at criterion maximum
                double clampedScore = Math.min(
                        result.score() != null ? result.score() : 0.0,
                        criterion.getMaxScore() != null ? criterion.getMaxScore() : 0.0
                );

                totalScore += clampedScore;

                HomeworkRubricCriterionScore criterionScore = HomeworkRubricCriterionScore.builder()
                        .questionSubmission(qs)
                        .criterion(criterion)
                        .score(clampedScore)
                        .comment(result.feedback())
                        .graderType(GraderType.AI)
                        .build();
                aiScores.add(criterionScore);

                if (result.feedback() != null && !result.feedback().isBlank()) {
                    feedbackJoiner.add("[" + criterion.getName() + "]: " + result.feedback());
                }
            }

            // ── Persist AI criterion scores ──
            // Remove any existing AI scores (e.g. from a previous failed attempt/rescore) before adding new ones
            qs.getCriterionScores().removeIf(cs -> cs.getGraderType() == GraderType.AI);
            qs.getCriterionScores().addAll(aiScores);

            // ── Update question submission ──
            qs.setScore(totalScore);
            qs.setAiFeedback(feedbackJoiner.toString());
            qs.setAiScoringStatus(AiScoringStatus.COMPLETED);
            qs.setAiScoredAt(Instant.now());
            questionSubmissionRepository.save(qs);

            // ── Update job ──
            job.setStatus(AiScoringStatus.COMPLETED);
            aiScoringJobRepository.save(job);

            log.info("[AiScoring] Completed scoring for questionSubId={}. totalScore={}", questionSubmissionId, totalScore);

            // ── Publish analytics event ──
            eventPublisher.publishEvent(new AiScoringCompletedEvent(
                    questionSubmissionId,
                    question.getHomework().getId(),
                    question.getHomework().getClazz().getId(),
                    qs.getSubmission().getCenterId()
            ));

        } catch (Exception e) {
            log.error("[AiScoring] Failed to score questionSubId={}: {}", questionSubmissionId, e.getMessage());

            qs.setAiScoringStatus(AiScoringStatus.FAILED);
            questionSubmissionRepository.save(qs);

            job.setStatus(AiScoringStatus.FAILED);
            job.setErrorMessage(e.getMessage() != null
                    ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 1000))
                    : "Unknown error");
            aiScoringJobRepository.save(job);
        }
    }

    /**
     * Constructs the structured scoring prompt for a single rubric criterion.
     * The prompt instructs the AI to respond with JSON only, and to use the same
     * language as the student's answer.
     */
    private String buildPrompt(String questionText, HomeworkRubricCriterion criterion, String studentAnswer) {
        return """
                You are an educational grading assistant.

                Question: %s

                Rubric Criterion: %s
                Description: %s
                Maximum Score: %s

                Student Answer:
                %s

                Evaluate the student's answer based ONLY on this criterion.
                Respond with valid JSON only, with no markdown or extra text:
                {
                  "score": <number between 0 and %s>,
                  "feedback": "<one sentence in the same language as the student's answer>"
                }
                """.formatted(
                questionText,
                criterion.getName(),
                criterion.getDescription() != null ? criterion.getDescription() : "",
                criterion.getMaxScore() != null ? criterion.getMaxScore() : 0,
                studentAnswer,
                criterion.getMaxScore() != null ? criterion.getMaxScore() : 0
        );
    }
}
