package com.owlexa.owlexabackend.modules.ai_grading.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.modules.ai_grading.config.AIGradingProperties;
import com.owlexa.owlexabackend.modules.ai_grading.dto.response.AIGradingJobSummaryResponse;
import com.owlexa.owlexabackend.modules.ai_grading.dto.response.AIGradingResultResponse;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingJobStatus;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingResult;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIModelProvider;
import com.owlexa.owlexabackend.modules.ai_grading.mapper.AIGradingMapper;
import com.owlexa.owlexabackend.modules.ai_grading.provider.AIGradingProvider;
import com.owlexa.owlexabackend.modules.ai_grading.provider.AIGradingProviderException;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingCriterionOutput;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingImprovementOutput;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingOutput;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingProviderRequest;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingProviderResponse;
import com.owlexa.owlexabackend.modules.ai_grading.repository.AIGradingJobRepository;
import com.owlexa.owlexabackend.modules.ai_grading.repository.AIGradingResultRepository;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttempt;
import com.owlexa.owlexabackend.modules.student_submission.repository.SubmissionAttemptRepository;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.service.AuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIGradingServiceTest {

    private static final Long CENTER_ID = 10L;
    private static final Long TEACHER_ID = 20L;
    private static final Long ATTEMPT_ID = 30L;
    private static final Long JOB_ID = 40L;

    @Mock private AIGradingProvider provider;
    @Mock private AIGradingJobLifecycleService lifecycleService;
    @Mock private AIGradingJobRepository jobRepository;
    @Mock private AIGradingResultRepository resultRepository;
    @Mock private SubmissionAttemptRepository submissionAttemptRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private MembershipRepository membershipRepository;
    @Mock private AIGradingMapper mapper;

    private AIGradingProperties properties;
    private AIGradingService service;
    private User teacher;

    @BeforeEach
    void setUp() {
        properties = new AIGradingProperties();
        properties.setEnabled(true);
        properties.setProvider(AIModelProvider.OPENAI);
        properties.setModel("gpt-test");
        properties.setMaxTokens(1000);

        service = new AIGradingService(
                properties,
                List.of(provider),
                lifecycleService,
                jobRepository,
                resultRepository,
                submissionAttemptRepository,
                authorizationService,
                membershipRepository,
                mapper
        );

        teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setRole(Role.TEACHER);
        teacher.setFullName("Teacher");

        TenantContext.setCurrentTenantId(CENTER_ID);
        lenient().when(authorizationService.getCurrentUser()).thenReturn(teacher);
        lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID))
                .thenReturn(true);
        lenient().when(provider.provider()).thenReturn(AIModelProvider.OPENAI);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("start: executes a new job and returns completed status")
    void startGrading_whenNewJob_shouldExecuteAndComplete() {
        AIGradingProviderRequest request = providerRequest();
        AIGradingExecutionContext context = new AIGradingExecutionContext(
                JOB_ID,
                true,
                AIModelProvider.OPENAI,
                request
        );
        AIGradingProviderResponse providerResponse = new AIGradingProviderResponse(
                new AIGradingOutput(
                        "Summary",
                        "Feedback",
                        "Ngữ pháp",
                        new BigDecimal("0.9000"),
                        List.of(new AIGradingCriterionOutput(
                                "Ngữ pháp",
                                new BigDecimal("4.00"),
                                new BigDecimal("5.00"),
                                "Tương đối chắc."
                        )),
                        List.of(new AIGradingImprovementOutput(
                                "Ngữ pháp",
                                "Một vài câu còn cứng.",
                                "Đa dạng hóa cấu trúc câu.",
                                "Thử kết hợp câu đơn và câu phức."
                        )),
                        List.of()
                ),
                "{\"status\":\"completed\"}"
        );
        AIGradingJobSummaryResponse completed = jobSummary(AIGradingJobStatus.COMPLETED);

        when(lifecycleService.createPendingJob(
                ATTEMPT_ID,
                CENTER_ID,
                TEACHER_ID,
                AIModelProvider.OPENAI,
                "gpt-test",
                null,
                1000
        )).thenReturn(context);
        when(provider.grade(request)).thenReturn(providerResponse);
        when(lifecycleService.getJobSummary(JOB_ID, CENTER_ID)).thenReturn(completed);

        AIGradingJobSummaryResponse response = service.startGrading(ATTEMPT_ID);

        assertThat(response.getStatus()).isEqualTo(AIGradingJobStatus.COMPLETED);
        verify(lifecycleService).markRunning(JOB_ID);
        verify(lifecycleService).completeJob(
                JOB_ID,
                providerResponse.output(),
                providerResponse.rawResponse()
        );
    }

    @Test
    @DisplayName("start: returns an existing active job without invoking provider")
    void startGrading_whenActiveJobExists_shouldBeIdempotent() {
        AIGradingExecutionContext context = new AIGradingExecutionContext(
                JOB_ID,
                false,
                AIModelProvider.OPENAI,
                null
        );
        AIGradingJobSummaryResponse running = jobSummary(AIGradingJobStatus.RUNNING);
        when(lifecycleService.createPendingJob(
                ATTEMPT_ID,
                CENTER_ID,
                TEACHER_ID,
                AIModelProvider.OPENAI,
                "gpt-test",
                null,
                1000
        )).thenReturn(context);
        when(lifecycleService.getJobSummary(JOB_ID, CENTER_ID)).thenReturn(running);

        AIGradingJobSummaryResponse response = service.startGrading(ATTEMPT_ID);

        assertThat(response.getStatus()).isEqualTo(AIGradingJobStatus.RUNNING);
        verify(provider, never()).grade(any());
        verify(lifecycleService, never()).markRunning(anyLong());
    }

    @Test
    @DisplayName("start: unique-key race returns the winning active job")
    void startGrading_whenConcurrentCreateWins_shouldReturnActiveJob() {
        DataIntegrityViolationException conflict = new DataIntegrityViolationException("active key");
        AIGradingJobSummaryResponse running = jobSummary(AIGradingJobStatus.RUNNING);
        when(lifecycleService.createPendingJob(
                ATTEMPT_ID,
                CENTER_ID,
                TEACHER_ID,
                AIModelProvider.OPENAI,
                "gpt-test",
                null,
                1000
        )).thenThrow(conflict);
        when(lifecycleService.findActiveJobSummary(ATTEMPT_ID)).thenReturn(Optional.of(running));

        AIGradingJobSummaryResponse response = service.startGrading(ATTEMPT_ID);

        assertThat(response).isSameAs(running);
        verify(provider, never()).grade(any());
    }

    @Test
    @DisplayName("start: provider failure transitions the job to failed")
    void startGrading_whenProviderFails_shouldFailJob() {
        AIGradingProviderRequest request = providerRequest();
        AIGradingExecutionContext context = new AIGradingExecutionContext(
                JOB_ID,
                true,
                AIModelProvider.OPENAI,
                request
        );
        AIGradingJobSummaryResponse failed = jobSummary(AIGradingJobStatus.FAILED);
        when(lifecycleService.createPendingJob(
                ATTEMPT_ID,
                CENTER_ID,
                TEACHER_ID,
                AIModelProvider.OPENAI,
                "gpt-test",
                null,
                1000
        )).thenReturn(context);
        when(provider.grade(request)).thenThrow(new AIGradingProviderException("Provider timeout"));
        when(lifecycleService.failJob(JOB_ID, "Provider timeout")).thenReturn(failed);

        AIGradingJobSummaryResponse response = service.startGrading(ATTEMPT_ID);

        assertThat(response.getStatus()).isEqualTo(AIGradingJobStatus.FAILED);
        verify(lifecycleService).failJob(JOB_ID, "Provider timeout");
        verify(lifecycleService, never()).completeJob(anyLong(), any(), any());
    }

    @Test
    @DisplayName("retry: resolves the failed job attempt and creates a new job")
    void retryJob_whenFailed_shouldCreateNewJob() {
        AIGradingExecutionContext context = new AIGradingExecutionContext(
                JOB_ID + 1,
                false,
                AIModelProvider.OPENAI,
                null
        );
        AIGradingJobSummaryResponse pending = AIGradingJobSummaryResponse.builder()
                .id(JOB_ID + 1)
                .status(AIGradingJobStatus.PENDING)
                .build();
        when(lifecycleService.getRetryAttemptId(JOB_ID, CENTER_ID)).thenReturn(ATTEMPT_ID);
        when(lifecycleService.createPendingJob(
                ATTEMPT_ID,
                CENTER_ID,
                TEACHER_ID,
                AIModelProvider.OPENAI,
                "gpt-test",
                null,
                1000
        )).thenReturn(context);
        when(lifecycleService.getJobSummary(JOB_ID + 1, CENTER_ID)).thenReturn(pending);

        AIGradingJobSummaryResponse response = service.retryJob(JOB_ID);

        assertThat(response.getId()).isEqualTo(JOB_ID + 1);
        verify(lifecycleService).getRetryAttemptId(JOB_ID, CENTER_ID);
    }

    @Test
    @DisplayName("latest result: always selects the latest completed result")
    void getLatestResult_shouldQueryCompletedResultOnly() {
        SubmissionAttempt attempt = new SubmissionAttempt();
        attempt.setId(ATTEMPT_ID);
        AIGradingResult result = new AIGradingResult();
        AIGradingResultResponse mapped = AIGradingResultResponse.builder().id(50L).build();

        when(submissionAttemptRepository
                .findByIdAndAssignmentRecipient_Assignment_Center_IdAndAssignmentRecipient_Assignment_DeletedAtIsNull(
                        ATTEMPT_ID,
                        CENTER_ID
                )).thenReturn(Optional.of(attempt));
        when(resultRepository
                .findTopBySubmissionAttempt_IdAndSubmissionAttempt_AssignmentRecipient_Assignment_Center_IdAndSubmissionAttempt_AssignmentRecipient_Assignment_DeletedAtIsNullAndJob_StatusOrderByCreatedAtDesc(
                        ATTEMPT_ID,
                        CENTER_ID,
                        AIGradingJobStatus.COMPLETED
                )).thenReturn(Optional.of(result));
        when(mapper.toResultResponse(result)).thenReturn(mapped);

        AIGradingResultResponse response = service.getLatestResult(ATTEMPT_ID);

        assertThat(response).isSameAs(mapped);
    }

    @Test
    @DisplayName("auth: non-teacher cannot start AI grading")
    void startGrading_whenCurrentUserIsNotTeacher_shouldThrowAccessDenied() {
        teacher.setRole(Role.STUDENT);

        assertThatThrownBy(() -> service.startGrading(ATTEMPT_ID))
                .isInstanceOf(AccessDeniedException.class);
        verify(lifecycleService, never()).createPendingJob(
                anyLong(),
                anyLong(),
                anyLong(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("auth: missing tenant context is rejected")
    void getJob_whenTenantMissing_shouldThrowBadRequest() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.getJob(JOB_ID))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("auth: teacher without center membership is rejected")
    void startGrading_whenTeacherIsNotMember_shouldThrowAccessDenied() {
        when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.startGrading(ATTEMPT_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    private AIGradingProviderRequest providerRequest() {
        return new AIGradingProviderRequest("gpt-test", null, 1000, "system", "user");
    }

    private AIGradingJobSummaryResponse jobSummary(AIGradingJobStatus status) {
        return AIGradingJobSummaryResponse.builder()
                .id(JOB_ID)
                .submissionAttemptId(ATTEMPT_ID)
                .status(status)
                .build();
    }
}
