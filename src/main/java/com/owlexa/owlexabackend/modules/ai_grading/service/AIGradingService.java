package com.owlexa.owlexabackend.modules.ai_grading.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.ai_grading.config.AIGradingProperties;
import com.owlexa.owlexabackend.modules.ai_grading.dto.response.AIGradingJobSummaryResponse;
import com.owlexa.owlexabackend.modules.ai_grading.dto.response.AIGradingResultResponse;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingJob;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingJobStatus;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingResult;
import com.owlexa.owlexabackend.modules.ai_grading.mapper.AIGradingMapper;
import com.owlexa.owlexabackend.modules.ai_grading.provider.AIGradingProvider;
import com.owlexa.owlexabackend.modules.ai_grading.provider.AIGradingProviderException;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingProviderResponse;
import com.owlexa.owlexabackend.modules.ai_grading.repository.AIGradingJobRepository;
import com.owlexa.owlexabackend.modules.ai_grading.repository.AIGradingResultRepository;
import com.owlexa.owlexabackend.modules.student_submission.repository.SubmissionAttemptRepository;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIGradingService {

    private final AIGradingProperties properties;
    private final List<AIGradingProvider> providers;
    private final AIGradingJobLifecycleService lifecycleService;
    private final AIGradingJobRepository jobRepository;
    private final AIGradingResultRepository resultRepository;
    private final SubmissionAttemptRepository submissionAttemptRepository;
    private final AuthorizationService authorizationService;
    private final MembershipRepository membershipRepository;
    private final AIGradingMapper mapper;

    public AIGradingJobSummaryResponse startGrading(Long submissionAttemptId) {
        User teacher = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        requireTeacherOwnsAttempt(submissionAttemptId, centerId, teacher.getId());
        return startGrading(submissionAttemptId, centerId, teacher.getId());
    }

    /**
     * Automatically grades a freshly submitted attempt (no TEACHER role required).
     * Runs synchronously so essay feedback is stored immediately after submission.
     * Skips silently when AI grading is disabled or the submission has no essay
     * answers. Student visibility of the AI score is controlled elsewhere by the
     * assignment's "show score" flag. Returns {@code true} when a grading job was
     * executed.
     */
    public boolean autoGradeOnSubmit(Long submissionAttemptId, Long centerId, Long requestedByUserId) {
        if (!properties.isEnabled()) {
            log.info("Auto AI grading skipped: attemptId={}, centerId={}, reason=disabled", submissionAttemptId, centerId);
            return false;
        }
        boolean eligible = lifecycleService.isAutoGradeEligible(submissionAttemptId, centerId);
        if (!eligible) {
            log.info("Auto AI grading skipped: attemptId={}, centerId={}, reason=not_eligible", submissionAttemptId, centerId);
            return false;
        }
        log.info(
                "Auto AI grading starting: attemptId={}, centerId={}, requestedByUserId={}, provider={}, model={}",
                submissionAttemptId,
                centerId,
                requestedByUserId,
                properties.getProvider(),
                properties.getModel()
        );
        startGrading(submissionAttemptId, centerId, requestedByUserId);
        return true;
    }

    public AIGradingJobSummaryResponse retryJob(Long jobId) {
        User teacher = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        Long submissionAttemptId = lifecycleService.getRetryAttemptId(jobId, centerId);
        requireTeacherOwnsAttempt(submissionAttemptId, centerId, teacher.getId());
        return startGrading(submissionAttemptId, centerId, teacher.getId());
    }

    public AIGradingJobSummaryResponse getJob(Long jobId) {
        User teacher = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        AIGradingJob job = findTeacherJob(jobId, centerId, teacher.getId());
        return mapper.toJobSummaryResponse(job);
    }

    @Transactional(readOnly = true)
    public List<AIGradingJobSummaryResponse> listJobs(Long submissionAttemptId) {
        User teacher = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        requireTeacherOwnsAttempt(submissionAttemptId, centerId, teacher.getId());

        return jobRepository.findAllBySubmissionAttempt_IdOrderByCreatedAtDesc(submissionAttemptId)
                .stream()
                .map(mapper::toJobSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AIGradingResultResponse getLatestResult(Long submissionAttemptId) {
        User teacher = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        requireTeacherOwnsAttempt(submissionAttemptId, centerId, teacher.getId());

        AIGradingResult result = resultRepository
                .findTopBySubmissionAttempt_IdAndSubmissionAttempt_AssignmentRecipient_Assignment_Center_IdAndSubmissionAttempt_AssignmentRecipient_Assignment_DeletedAtIsNullAndJob_StatusOrderByCreatedAtDesc(
                        submissionAttemptId,
                        centerId,
                        AIGradingJobStatus.COMPLETED
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Completed AI grading result not found for submission attempt: " + submissionAttemptId
                ));
        return mapper.toResultResponse(result);
    }

    @Transactional(readOnly = true)
    public AIGradingResultResponse getResultForJob(Long jobId) {
        User teacher = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        AIGradingJob job = findTeacherJob(jobId, centerId, teacher.getId());

        AIGradingResult result = resultRepository.findByJob_Id(job.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AI grading result not found for job: " + jobId
                ));
        return mapper.toResultResponse(result);
    }

    private AIGradingJobSummaryResponse startGrading(
            Long submissionAttemptId,
            Long centerId,
            Long requestedByUserId
    ) {
        validateConfiguration();
        AIGradingProvider provider = resolveProvider();
        log.info(
                "AI grading requested: attemptId={}, centerId={}, requestedByUserId={}, provider={}, model={}, maxTokens={}, temperature={}",
                submissionAttemptId,
                centerId,
                requestedByUserId,
                properties.getProvider(),
                properties.getModel(),
                properties.getMaxTokens(),
                properties.getTemperature()
        );

        AIGradingExecutionContext context;
        try {
            context = lifecycleService.createPendingJob(
                    submissionAttemptId,
                    centerId,
                    requestedByUserId,
                    properties.getProvider(),
                    properties.getModel().trim(),
                    properties.getTemperature(),
                    properties.getMaxTokens()
            );
        } catch (DataIntegrityViolationException exception) {
            log.warn(
                    "AI grading createPendingJob hit active-job race: attemptId={}, centerId={}, error={}",
                    submissionAttemptId,
                    centerId,
                    exception.getMessage()
            );
            return lifecycleService.findActiveJobSummary(submissionAttemptId)
                    .orElseThrow(() -> exception);
        }

        if (!context.shouldExecute()) {
            log.info(
                    "AI grading reused active job: attemptId={}, centerId={}, jobId={}",
                    submissionAttemptId,
                    centerId,
                    context.jobId()
            );
            return lifecycleService.getJobSummary(context.jobId(), centerId);
        }

        lifecycleService.markRunning(context.jobId());
        log.info(
                "AI grading provider call started: attemptId={}, centerId={}, jobId={}, provider={}, model={}",
                submissionAttemptId,
                centerId,
                context.jobId(),
                provider.provider(),
                context.providerRequest().modelName()
        );
        try {
            AIGradingProviderResponse response = provider.grade(context.providerRequest());
            lifecycleService.completeJob(context.jobId(), response.output(), response.rawResponse());
            log.info(
                    "AI grading provider call completed: attemptId={}, centerId={}, jobId={}, provider={}, itemCount={}",
                    submissionAttemptId,
                    centerId,
                    context.jobId(),
                    provider.provider(),
                    response.output().items() == null ? 0 : response.output().items().size()
            );
            return lifecycleService.getJobSummary(context.jobId(), centerId);
        } catch (AIGradingProviderException exception) {
            log.warn(
                    "AI grading provider call failed: attemptId={}, centerId={}, jobId={}, provider={}, error={}",
                    submissionAttemptId,
                    centerId,
                    context.jobId(),
                    provider.provider(),
                    exception.getMessage(),
                    exception
            );
            return lifecycleService.failJob(context.jobId(), exception.getMessage());
        }
    }

    private AIGradingProvider resolveProvider() {
        return providers.stream()
                .filter(provider -> provider.provider() == properties.getProvider())
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "AI grading provider is not available: " + properties.getProvider()
                ));
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new BadRequestException("AI grading is disabled");
        }
        if (properties.getProvider() == null) {
            throw new BadRequestException("AI grading provider is not configured");
        }
        if (properties.getModel() == null || properties.getModel().isBlank()) {
            throw new BadRequestException("AI grading model is not configured");
        }
        if (properties.getMaxTokens() <= 0) {
            throw new BadRequestException("AI grading max tokens must be greater than zero");
        }
        BigDecimal temperature = properties.getTemperature();
        if (temperature != null && temperature.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("AI grading temperature cannot be negative");
        }
    }

    private void requireTeacherOwnsAttempt(Long submissionAttemptId, Long centerId, Long teacherUserId) {
        boolean ownsAttempt = submissionAttemptRepository
                .existsByIdAndAssignmentRecipient_Assignment_Center_IdAndAssignmentRecipient_Assignment_CreatedBy_IdAndAssignmentRecipient_Assignment_DeletedAtIsNull(
                        submissionAttemptId,
                        centerId,
                        teacherUserId
                );
        if (!ownsAttempt) {
            throw new ResourceNotFoundException(
                    "Submission attempt not found with id: " + submissionAttemptId
            );
        }
    }

    private AIGradingJob findTeacherJob(Long jobId, Long centerId, Long teacherUserId) {
        return jobRepository
                .findByIdAndSubmissionAttempt_AssignmentRecipient_Assignment_Center_IdAndSubmissionAttempt_AssignmentRecipient_Assignment_CreatedBy_IdAndSubmissionAttempt_AssignmentRecipient_Assignment_DeletedAtIsNull(
                        jobId,
                        centerId,
                        teacherUserId
                )
                .orElseThrow(() -> new ResourceNotFoundException("AI grading job not found with id: " + jobId));
    }

    private User requireTeacherInCurrentCenter() {
        User currentUser = authorizationService.getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("Only TEACHER can manage AI grading");
        }
        if (!membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId)) {
            throw new AccessDeniedException("User is not a member of this center");
        }
        return currentUser;
    }

    private Long requiredCurrentCenterId() {
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new BadRequestException("Tenant context not resolved");
        }
        return centerId;
    }
}
