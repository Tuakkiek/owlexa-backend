package com.owlexa.owlexabackend.modules.ai_scoring.service;

import com.owlexa.owlexabackend.common.config.AiProperties;
import com.owlexa.owlexabackend.modules.ai_scoring.dto.AiBulkScoringResult;
import com.owlexa.owlexabackend.modules.ai_scoring.dto.AiScoringResponseDto;
import com.owlexa.owlexabackend.modules.ai_scoring.entity.AiScoringJob;
import com.owlexa.owlexabackend.modules.ai_scoring.gateway.AiScoringGateway;
import com.owlexa.owlexabackend.modules.ai_scoring.gateway.DeepSeekAiScoringGateway;
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
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkTemplate;
import com.owlexa.owlexabackend.modules.homework.entity.GradingCriteria;
import com.owlexa.owlexabackend.modules.homework.repository.GradingCriteriaRepository;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core AI scoring service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiScoringService {

    private final HomeworkQuestionSubmissionRepository questionSubmissionRepository;
    private final AiScoringJobRepository aiScoringJobRepository;
    private final DeepSeekAiScoringGateway aiScoringGateway;
    private final AiProperties aiProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final GradingCriteriaRepository gradingCriteriaRepository;

    @Async("asyncTaskExecutor")
    public void rescoreEssaySubmissionAsync(Long questionSubmissionId) {
        scoreEssaySubmission(questionSubmissionId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scoreEssaySubmission(Long questionSubmissionId) {
        log.info("[AiScoring] Starting AI scoring for questionSubmissionId={}", questionSubmissionId);

        HomeworkQuestionSubmission qs = questionSubmissionRepository
                .findWithRubricDetailsById(questionSubmissionId)
                .orElseThrow(() -> {
                    log.error("[AiScoring] QuestionSubmission not found: {}", questionSubmissionId);
                    return new IllegalArgumentException("QuestionSubmission not found: " + questionSubmissionId);
                });

        if (qs.getQuestion().getType() != HomeworkQuestionType.ESSAY) {
            log.debug("[AiScoring] Skipping non-ESSAY question {}", questionSubmissionId);
            qs.setAiScoringStatus(AiScoringStatus.SKIPPED);
            questionSubmissionRepository.save(qs);
            return;
        }

        if (!aiProperties.isEnabled()) {
            log.info("[AiScoring] AI disabled in config. Skipping questionSubmissionId={}", questionSubmissionId);
            qs.setAiScoringStatus(AiScoringStatus.SKIPPED);
            questionSubmissionRepository.save(qs);
            return;
        }

        HomeworkQuestion question = qs.getQuestion();
        HomeworkRubric rubric = question.getRubric();
        if (rubric == null || rubric.getCriteria() == null || rubric.getCriteria().isEmpty()) {
            log.warn("[AiScoring] No rubric/criteria for question {}. Skipping.", question.getId());
            qs.setAiScoringStatus(AiScoringStatus.SKIPPED);
            questionSubmissionRepository.save(qs);
            return;
        }

        String criteriaContent = null;
        HomeworkTemplate template = question.getHomeworkTemplate();
        if (template != null && template.getGradingCriteriaId() != null) {
            criteriaContent = gradingCriteriaRepository.findById(template.getGradingCriteriaId())
                    .map(GradingCriteria::getContent)
                    .orElse(null);
        }

        AiScoringJob job = AiScoringJob.builder()
                .submissionId(qs.getSubmission().getId())
                .questionSubId(questionSubmissionId)
                .centerId(qs.getSubmission().getCenterId())
                .status(AiScoringStatus.IN_PROGRESS)
                .attemptCount(1)
                .modelUsed(aiProperties.getModel())
                .build();
        job = aiScoringJobRepository.save(job);

        qs.setAiScoringStatus(AiScoringStatus.IN_PROGRESS);
        questionSubmissionRepository.save(qs);

        try {
            String studentAnswer = qs.getTextAnswer() != null ? qs.getTextAnswer() : "(No answer provided)";
            String prompt = buildBulkPrompt(question.getQuestionText(), rubric, studentAnswer, criteriaContent);
            
            AiScoringResponseDto responseDto = aiScoringGateway.scoreEssay(prompt);
            AiBulkScoringResult result = responseDto.getResult();
            
            job.setPromptTokens(responseDto.getPromptTokens());
            job.setResponseTokens(responseDto.getResponseTokens());
            job.setTotalTokens(responseDto.getTotalTokens());
            job.setLatencyMs(responseDto.getLatencyMs());
            job.setModelUsed(responseDto.getModelUsed());
            
            double totalScore = 0.0;
            List<HomeworkRubricCriterionScore> aiScores = new ArrayList<>();
            
            Map<String, HomeworkRubricCriterion> criteriaMap = rubric.getCriteria().stream()
                    .collect(Collectors.toMap(c -> c.getName().toLowerCase(), c -> c));

            if (result != null && result.getCriteria() != null) {
                for (AiBulkScoringResult.CriterionResult cr : result.getCriteria()) {
                    if (cr.getCriterion() == null) continue;
                    HomeworkRubricCriterion criterion = criteriaMap.get(cr.getCriterion().toLowerCase());
                    if (criterion != null) {
                        double clampedScore = Math.min(
                                cr.getScore() != null ? cr.getScore() : 0.0,
                                criterion.getMaxScore() != null ? criterion.getMaxScore() : 0.0
                        );
                        totalScore += clampedScore;
                        
                        HomeworkRubricCriterionScore criterionScore = HomeworkRubricCriterionScore.builder()
                                .questionSubmission(qs)
                                .criterion(criterion)
                                .score(clampedScore)
                                .comment(cr.getFeedback())
                                .graderType(GraderType.AI)
                                .build();
                        aiScores.add(criterionScore);
                    }
                }
            }

            qs.getCriterionScores().removeIf(cs -> cs.getGraderType() == GraderType.AI);
            qs.getCriterionScores().addAll(aiScores);

            qs.setScore(totalScore);
            qs.setAiFeedback(result != null ? result.getOverallFeedback() : null);
            qs.setAiScoringStatus(AiScoringStatus.COMPLETED);
            qs.setAiScoredAt(Instant.now());
            questionSubmissionRepository.save(qs);

            job.setStatus(AiScoringStatus.COMPLETED);
            aiScoringJobRepository.save(job);

            log.info("[AiScoring] Completed scoring for questionSubId={}. totalScore={}", questionSubmissionId, totalScore);

            eventPublisher.publishEvent(new AiScoringCompletedEvent(
                    questionSubmissionId,
                    question.getHomeworkTemplate().getId(),
                    qs.getSubmission().getHomeworkAssignment().getClazz().getId(),
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

    private String buildBulkPrompt(String questionText, HomeworkRubric rubric, String studentAnswer, String criteriaContent) {
        StringBuilder rubricBuilder = new StringBuilder();
        for (HomeworkRubricCriterion criterion : rubric.getCriteria()) {
            rubricBuilder.append("Criterion:\n")
                    .append(criterion.getName()).append("\n")
                    .append("Description: ").append(criterion.getDescription() != null ? criterion.getDescription() : "").append("\n")
                    .append("Max Score: ").append(criterion.getMaxScore() != null ? criterion.getMaxScore() : 0).append("\n\n");
        }
        
        String optionalCriteriaContext = "";
        if (criteriaContent != null && !criteriaContent.isBlank()) {
            optionalCriteriaContext = "\nAdditional Grading Context:\n" + criteriaContent + "\n";
        }

        return """
                Question:
                %s
                %s
                Rubric:
                %s
                Student Answer:
                %s

                Return JSON:
                {
                  "criteria":[
                      {
                          "criterion":"<exact criterion name>",
                          "score":<number>,
                          "feedback":"<short feedback>"
                      }
                  ],
                  "overallFeedback":"<overall feedback on the answer>",
                  "improvementSuggestions":"<suggestions>"
                }
                """.formatted(
                questionText != null ? questionText : "",
                optionalCriteriaContext,
                rubricBuilder.toString(),
                studentAnswer
        );
    }
}
