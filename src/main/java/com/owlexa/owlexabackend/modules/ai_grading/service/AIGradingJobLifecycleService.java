package com.owlexa.owlexabackend.modules.ai_grading.service;

import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.ai_grading.dto.response.AIGradingJobSummaryResponse;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingItemResult;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingJob;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingJobStatus;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingResult;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIModelProvider;
import com.owlexa.owlexabackend.modules.ai_grading.mapper.AIGradingMapper;
import com.owlexa.owlexabackend.modules.ai_grading.prompt.AIGradingPromptBuilder;
import com.owlexa.owlexabackend.modules.ai_grading.prompt.AIGradingPromptSnapshot;
import com.owlexa.owlexabackend.modules.ai_grading.provider.AIGradingProviderException;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingCriterionOutput;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingItemOutput;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingImprovementOutput;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingOutput;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingProviderRequest;
import com.owlexa.owlexabackend.modules.ai_grading.repository.AIGradingJobRepository;
import com.owlexa.owlexabackend.modules.ai_grading.repository.AIGradingResultRepository;
import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAnswer;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttempt;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttemptStatus;
import com.owlexa.owlexabackend.modules.student_submission.repository.SubmissionAttemptRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
class AIGradingJobLifecycleService {

    private static final int MAX_STORED_ERROR_LENGTH = 2000;

    private final AIGradingJobRepository jobRepository;
    private final AIGradingResultRepository resultRepository;
    private final SubmissionAttemptRepository submissionAttemptRepository;
    private final UserRepository userRepository;
    private final AIGradingPromptBuilder promptBuilder;
    private final AIGradingMapper mapper;

    @Transactional
    public AIGradingExecutionContext createPendingJob(
            Long submissionAttemptId,
            Long centerId,
            Long requestedByUserId,
            AIModelProvider provider,
            String modelName,
            BigDecimal temperature,
            Integer maxTokens
    ) {
        SubmissionAttempt attempt = findAttempt(submissionAttemptId, centerId);

        Optional<AIGradingJob> activeJob = jobRepository.findByActiveJobKey(submissionAttemptId);
        if (activeJob.isPresent()) {
            log.info(
                    "AI grading pending job reused: attemptId={}, centerId={}, jobId={}, status={}",
                    submissionAttemptId,
                    centerId,
                    activeJob.get().getId(),
                    activeJob.get().getStatus()
            );
            return new AIGradingExecutionContext(activeJob.get().getId(), false, provider, null);
        }

        validateAttempt(attempt);
        List<SubmissionAnswer> essayAnswers = essayAnswers(attempt);
        if (essayAnswers.isEmpty()) {
            throw new BadRequestException("Submission attempt has no essay answers to grade");
        }
        log.info(
                "AI grading pending job creation: attemptId={}, centerId={}, requestedByUserId={}, essayAnswerCount={}, submissionStatus={}, provider={}, model={}",
                submissionAttemptId,
                centerId,
                requestedByUserId,
                essayAnswers.size(),
                attempt.getStatus(),
                provider,
                modelName
        );

        AIGradingPromptSnapshot prompt = promptBuilder.build(essayAnswers);
        AIGradingJob job = AIGradingJob.builder()
                .submissionAttempt(attempt)
                .status(AIGradingJobStatus.PENDING)
                .requestedBy(userRepository.getReferenceById(requestedByUserId))
                .promptTemplateVersion(prompt.promptTemplateVersion())
                .promptBuilderVersion(prompt.promptBuilderVersion())
                .modelProvider(provider)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .systemPrompt(prompt.systemPrompt())
                .userPrompt(prompt.userPrompt())
                .activeJobKey(submissionAttemptId)
                .build();

        AIGradingJob savedJob = jobRepository.saveAndFlush(job);
        log.info(
                "AI grading job saved: attemptId={}, centerId={}, jobId={}, essayAnswerCount={}, systemPromptLength={}, userPromptLength={}",
                submissionAttemptId,
                centerId,
                savedJob.getId(),
                essayAnswers.size(),
                prompt.systemPrompt().length(),
                prompt.userPrompt().length()
        );
        AIGradingProviderRequest providerRequest = new AIGradingProviderRequest(
                modelName,
                temperature,
                maxTokens,
                prompt.systemPrompt(),
                prompt.userPrompt()
        );
        return new AIGradingExecutionContext(savedJob.getId(), true, provider, providerRequest);
    }

    @Transactional
    public void markRunning(Long jobId) {
        AIGradingJob job = findJob(jobId);
        if (job.getStatus() != AIGradingJobStatus.PENDING) {
            throw new BadRequestException("Only pending AI grading jobs can start");
        }

        job.setStatus(AIGradingJobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);
        log.info(
                "AI grading job running: jobId={}, attemptId={}, startedAt={}",
                jobId,
                job.getSubmissionAttempt().getId(),
                job.getStartedAt()
        );
    }

    @Transactional
    public void completeJob(Long jobId, AIGradingOutput output, String rawResponse) {
        AIGradingJob job = findJob(jobId);
        if (job.getStatus() != AIGradingJobStatus.RUNNING) {
            throw new BadRequestException("Only running AI grading jobs can complete");
        }

        List<SubmissionAnswer> essayAnswers = essayAnswers(job.getSubmissionAttempt());
        Map<Integer, AIGradingItemOutput> outputByItemNumber = validateOutput(output, essayAnswers);
        log.info(
                "AI grading completion started: jobId={}, attemptId={}, essayAnswerCount={}, outputItemCount={}",
                jobId,
                job.getSubmissionAttempt().getId(),
                essayAnswers.size(),
                output.items() == null ? 0 : output.items().size()
        );

        AIGradingResult result = AIGradingResult.builder()
                .job(job)
                .submissionAttempt(job.getSubmissionAttempt())
                .summary(output.summary())
                .overallFeedback(output.overallFeedback())
                .confidence(confidence(output.confidence()))
                .rawResponse(rawResponse)
                .build();

        BigDecimal totalScore = score(BigDecimal.ZERO);
        BigDecimal totalMaxScore = score(BigDecimal.ZERO);

        for (int index = 0; index < essayAnswers.size(); index++) {
            SubmissionAnswer answer = essayAnswers.get(index);
            AIGradingItemOutput itemOutput = outputByItemNumber.get(index + 1);
            BigDecimal maxScore = maxScore(answer);
            BigDecimal aiScore = score(itemOutput.aiScore());

            if (aiScore.compareTo(maxScore) > 0) {
                throw new AIGradingProviderException("AI score exceeds the assignment item maximum score");
            }

            AIGradingItemResult itemResult = AIGradingItemResult.builder()
                    .result(result)
                    .submissionAnswer(answer)
                    .assignmentItem(answer.getAssignmentItem())
                    .aiScore(aiScore)
                    .maxScore(maxScore)
                    .feedback(itemOutput.feedback())
                    .rubricAnalysis(itemOutput.rubricAnalysis())
                    .confidence(confidence(itemOutput.confidence()))
                    .build();
            result.getItemResults().add(itemResult);
            totalScore = totalScore.add(aiScore);
            totalMaxScore = totalMaxScore.add(maxScore);
        }

        result.setAiScore(score(totalScore));
        result.setMaxScore(score(totalMaxScore));
        resultRepository.save(result);
        log.info(
                "AI grading result persisted: jobId={}, attemptId={}, aiScore={}, maxScore={}, itemResultCount={}",
                jobId,
                job.getSubmissionAttempt().getId(),
                result.getAiScore(),
                result.getMaxScore(),
                result.getItemResults().size()
        );

        job.setResult(result);
        job.setStatus(AIGradingJobStatus.COMPLETED);
        job.setCompletedAt(Instant.now());
        job.setActiveJobKey(null);
        jobRepository.save(job);
        log.info(
                "AI grading job completed: jobId={}, attemptId={}, completedAt={}",
                jobId,
                job.getSubmissionAttempt().getId(),
                job.getCompletedAt()
        );
    }

    @Transactional
    public AIGradingJobSummaryResponse failJob(Long jobId, String errorMessage) {
        AIGradingJob job = findJob(jobId);
        if (job.getStatus() == AIGradingJobStatus.COMPLETED
                || job.getStatus() == AIGradingJobStatus.FAILED) {
            return mapper.toJobSummaryResponse(job);
        }

        job.setStatus(AIGradingJobStatus.FAILED);
        job.setFailedAt(Instant.now());
        job.setErrorMessage(truncate(errorMessage));
        job.setActiveJobKey(null);
        log.warn(
                "AI grading job failed: jobId={}, attemptId={}, statusBeforeSave={}, error={}",
                jobId,
                job.getSubmissionAttempt().getId(),
                job.getStatus(),
                job.getErrorMessage()
        );
        return mapper.toJobSummaryResponse(jobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public AIGradingJobSummaryResponse getJobSummary(Long jobId, Long centerId) {
        AIGradingJob job = jobRepository
                .findByIdAndSubmissionAttempt_AssignmentRecipient_Assignment_Center_IdAndSubmissionAttempt_AssignmentRecipient_Assignment_DeletedAtIsNull(
                        jobId,
                        centerId
                )
                .orElseThrow(() -> new ResourceNotFoundException("AI grading job not found with id: " + jobId));
        return mapper.toJobSummaryResponse(job);
    }

    @Transactional(readOnly = true)
    public Optional<AIGradingJobSummaryResponse> findActiveJobSummary(Long submissionAttemptId) {
        return jobRepository.findByActiveJobKey(submissionAttemptId)
                .map(mapper::toJobSummaryResponse);
    }

    /**
     * Determines whether a submitted attempt qualifies for automatic AI grading:
     * the assignment contains at least one essay question. Student visibility of
     * the AI score remains controlled separately by the assignment's "show score"
     * flag.
     */
    @Transactional(readOnly = true)
    public boolean isAutoGradeEligible(Long submissionAttemptId, Long centerId) {
        SubmissionAttempt attempt = findAttempt(submissionAttemptId, centerId);
        Assignment assignment = attempt.getAssignmentRecipient().getAssignment();
        boolean eligible = assignment.getItems().stream()
                .anyMatch(item -> item.getQuestionType() == QuestionType.ESSAY);
        log.info(
                "AI grading eligibility checked: attemptId={}, centerId={}, eligible={}, submissionStatus={}, itemCount={}",
                submissionAttemptId,
                centerId,
                eligible,
                attempt.getStatus(),
                assignment.getItems().size()
        );
        return eligible;
    }

    @Transactional(readOnly = true)
    public Long getRetryAttemptId(Long jobId, Long centerId) {
        AIGradingJob job = jobRepository
                .findByIdAndSubmissionAttempt_AssignmentRecipient_Assignment_Center_IdAndSubmissionAttempt_AssignmentRecipient_Assignment_DeletedAtIsNull(
                        jobId,
                        centerId
                )
                .orElseThrow(() -> new ResourceNotFoundException("AI grading job not found with id: " + jobId));

        if (job.getStatus() != AIGradingJobStatus.FAILED) {
            throw new BadRequestException("Only failed AI grading jobs can be retried");
        }
        return job.getSubmissionAttempt().getId();
    }

    private SubmissionAttempt findAttempt(Long submissionAttemptId, Long centerId) {
        return submissionAttemptRepository
                .findByIdAndAssignmentRecipient_Assignment_Center_IdAndAssignmentRecipient_Assignment_DeletedAtIsNull(
                        submissionAttemptId,
                        centerId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Submission attempt not found with id: " + submissionAttemptId
                ));
    }

    private AIGradingJob findJob(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("AI grading job not found with id: " + jobId));
    }

    private void validateAttempt(SubmissionAttempt attempt) {
        if (attempt.getStatus() != SubmissionAttemptStatus.SUBMITTED
                && attempt.getStatus() != SubmissionAttemptStatus.AUTO_SUBMITTED) {
            throw new BadRequestException("Only submitted attempts can be AI graded");
        }
    }

    private List<SubmissionAnswer> essayAnswers(SubmissionAttempt attempt) {
        return attempt.getAnswers().stream()
                .filter(answer -> answer.getAssignmentItem().getQuestionType() == QuestionType.ESSAY)
                .sorted(Comparator.comparing(answer -> answer.getAssignmentItem().getDisplayOrder()))
                .toList();
    }

    private Map<Integer, AIGradingItemOutput> validateOutput(
            AIGradingOutput output,
            List<SubmissionAnswer> essayAnswers
    ) {
        if (output == null || output.items() == null || output.items().size() != essayAnswers.size()) {
            throw new AIGradingProviderException("AI grading result does not match the submitted essay answers");
        }
        requireText(output.summary(), "AI grading summary is missing");
        requireText(output.overallFeedback(), "AI overall feedback is missing");
        requireText(output.focusArea(), "AI focus area is missing");
        confidence(output.confidence());
        validateCriteria(output.criteria());
        validateImprovements(output.improvements());

        Map<Integer, AIGradingItemOutput> outputByItemNumber = new HashMap<>();
        for (AIGradingItemOutput item : output.items()) {
            if (item == null || item.itemNumber() == null
                    || item.itemNumber() < 1
                    || item.itemNumber() > essayAnswers.size()) {
                throw new AIGradingProviderException("AI grading result contains an invalid item number");
            }
            if (outputByItemNumber.put(item.itemNumber(), item) != null) {
                throw new AIGradingProviderException("AI grading result contains duplicate items");
            }
            if (item.aiScore() == null || item.aiScore().compareTo(BigDecimal.ZERO) < 0) {
                throw new AIGradingProviderException("AI grading result contains an invalid score");
            }
            requireText(item.feedback(), "AI item feedback is missing");
            requireText(item.rubricAnalysis(), "AI rubric analysis is missing");
            confidence(item.confidence());
        }
        return outputByItemNumber;
    }

    private void validateCriteria(List<AIGradingCriterionOutput> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            throw new AIGradingProviderException("AI criterion breakdown is missing");
        }

        for (AIGradingCriterionOutput criterion : criteria) {
            if (criterion == null) {
                throw new AIGradingProviderException("AI criterion breakdown contains an empty item");
            }
            requireText(criterion.name(), "AI criterion name is missing");
            requireText(criterion.feedback(), "AI criterion feedback is missing");
            if (criterion.score() == null || criterion.score().compareTo(BigDecimal.ZERO) < 0) {
                throw new AIGradingProviderException("AI criterion score is invalid");
            }
            if (criterion.maxScore() == null || criterion.maxScore().compareTo(BigDecimal.ZERO) <= 0) {
                throw new AIGradingProviderException("AI criterion max score is invalid");
            }
            if (criterion.score().compareTo(criterion.maxScore()) > 0) {
                throw new AIGradingProviderException("AI criterion score exceeds its maximum score");
            }
        }
    }

    private void validateImprovements(List<AIGradingImprovementOutput> improvements) {
        if (improvements == null || improvements.isEmpty()) {
            throw new AIGradingProviderException("AI improvement suggestions are missing");
        }

        for (AIGradingImprovementOutput improvement : improvements) {
            if (improvement == null) {
                throw new AIGradingProviderException("AI improvement suggestions contain an empty item");
            }
            requireText(improvement.category(), "AI improvement category is missing");
            requireText(improvement.issue(), "AI improvement issue is missing");
            requireText(improvement.suggestion(), "AI improvement suggestion is missing");
            requireText(improvement.example(), "AI improvement example is missing");
        }
    }

    private BigDecimal maxScore(SubmissionAnswer answer) {
        BigDecimal value = answer.getMaxScore();
        if (value == null) {
            value = answer.getAssignmentItem().getPoints();
        }
        return score(value == null ? BigDecimal.ZERO : value);
    }

    private BigDecimal score(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal confidence(BigDecimal value) {
        if (value == null
                || value.compareTo(BigDecimal.ZERO) < 0
                || value.compareTo(BigDecimal.ONE) > 0) {
            throw new AIGradingProviderException("AI grading result contains an invalid confidence value");
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new AIGradingProviderException(message);
        }
    }

    private String truncate(String message) {
        String safeMessage = message == null || message.isBlank()
                ? "AI grading failed"
                : message;
        return safeMessage.length() <= MAX_STORED_ERROR_LENGTH
                ? safeMessage
                : safeMessage.substring(0, MAX_STORED_ERROR_LENGTH);
    }
}
