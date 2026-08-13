package com.owlexa.owlexabackend.modules.ai_grading.service;

import com.owlexa.owlexabackend.common.exception.BadRequestException;
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
import com.owlexa.owlexabackend.modules.ai_grading.repository.AIGradingJobRepository;
import com.owlexa.owlexabackend.modules.ai_grading.repository.AIGradingResultRepository;
import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItem;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipient;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAnswer;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttempt;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttemptStatus;
import com.owlexa.owlexabackend.modules.student_submission.repository.SubmissionAttemptRepository;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.owlexa.owlexabackend.support.RichTextTestFixtures.serializedDocument;

@ExtendWith(MockitoExtension.class)
class AIGradingJobLifecycleServiceTest {

    private static final Long CENTER_ID = 10L;
    private static final Long TEACHER_ID = 20L;
    private static final Long ATTEMPT_ID = 30L;
    private static final Long ANSWER_ID = 40L;
    private static final Long ITEM_ID = 50L;
    private static final Long JOB_ID = 60L;

    @Mock private AIGradingJobRepository jobRepository;
    @Mock private AIGradingResultRepository resultRepository;
    @Mock private SubmissionAttemptRepository submissionAttemptRepository;
    @Mock private UserRepository userRepository;
    @Mock private AIGradingPromptBuilder promptBuilder;

    private AIGradingJobLifecycleService service;
    private User teacher;

    @BeforeEach
    void setUp() {
        service = new AIGradingJobLifecycleService(
                jobRepository,
                resultRepository,
                submissionAttemptRepository,
                userRepository,
                promptBuilder,
                new AIGradingMapper()
        );

        teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setRole(Role.TEACHER);
        teacher.setFullName("Teacher");
    }

    @Test
    @DisplayName("create: snapshots prompt and configuration into a pending job")
    void createPendingJob_whenValidAttempt_shouldCreateSnapshot() {
        SubmissionAttempt attempt = submittedAttempt();
        AIGradingPromptSnapshot prompt = new AIGradingPromptSnapshot(
                "template-v1",
                "builder-v1",
                "system",
                "user"
        );
        whenAttemptFound(attempt);
        when(jobRepository.findByActiveJobKey(ATTEMPT_ID)).thenReturn(Optional.empty());
        when(promptBuilder.build(any())).thenReturn(prompt);
        when(userRepository.getReferenceById(TEACHER_ID)).thenReturn(teacher);
        when(jobRepository.saveAndFlush(any(AIGradingJob.class))).thenAnswer(invocation -> {
            AIGradingJob job = invocation.getArgument(0);
            job.setId(JOB_ID);
            return job;
        });

        AIGradingExecutionContext context = service.createPendingJob(
                ATTEMPT_ID,
                CENTER_ID,
                TEACHER_ID,
                 AIModelProvider.GEMINI,
                "gpt-test",
                new BigDecimal("0.20"),
                1000
        );

        ArgumentCaptor<AIGradingJob> jobCaptor = ArgumentCaptor.forClass(AIGradingJob.class);
        verify(jobRepository).saveAndFlush(jobCaptor.capture());
        AIGradingJob job = jobCaptor.getValue();
        assertThat(job.getStatus()).isEqualTo(AIGradingJobStatus.PENDING);
        assertThat(job.getActiveJobKey()).isEqualTo(ATTEMPT_ID);
        assertThat(job.getPromptTemplateVersion()).isEqualTo("template-v1");
        assertThat(job.getPromptBuilderVersion()).isEqualTo("builder-v1");
        assertThat(job.getSystemPrompt()).isEqualTo("system");
        assertThat(job.getUserPrompt()).isEqualTo("user");
        assertThat(context.shouldExecute()).isTrue();
        assertThat(context.providerRequest().modelName()).isEqualTo("gpt-test");
    }

    @Test
    @DisplayName("create: active job is returned without creating another job")
    void createPendingJob_whenActiveJobExists_shouldReturnExisting() {
        SubmissionAttempt attempt = submittedAttempt();
        AIGradingJob activeJob = job(attempt, AIGradingJobStatus.RUNNING);
        whenAttemptFound(attempt);
        when(jobRepository.findByActiveJobKey(ATTEMPT_ID)).thenReturn(Optional.of(activeJob));

        AIGradingExecutionContext context = service.createPendingJob(
                ATTEMPT_ID,
                CENTER_ID,
                TEACHER_ID,
                 AIModelProvider.GEMINI,
                "gpt-test",
                null,
                1000
        );

        assertThat(context.jobId()).isEqualTo(JOB_ID);
        assertThat(context.shouldExecute()).isFalse();
        verify(promptBuilder, never()).build(any());
        verify(jobRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("create: only submitted attempts can be graded")
    void createPendingJob_whenAttemptInProgress_shouldReject() {
        SubmissionAttempt attempt = submittedAttempt();
        attempt.setStatus(SubmissionAttemptStatus.IN_PROGRESS);
        whenAttemptFound(attempt);
        when(jobRepository.findByActiveJobKey(ATTEMPT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPendingJob(
                ATTEMPT_ID,
                CENTER_ID,
                TEACHER_ID,
                 AIModelProvider.GEMINI,
                "gpt-test",
                null,
                1000
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create: attempt without essay answers is rejected")
    void createPendingJob_whenNoEssayAnswers_shouldReject() {
        SubmissionAttempt attempt = submittedAttempt();
        attempt.getAnswers().clear();
        whenAttemptFound(attempt);
        when(jobRepository.findByActiveJobKey(ATTEMPT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPendingJob(
                ATTEMPT_ID,
                CENTER_ID,
                TEACHER_ID,
                 AIModelProvider.GEMINI,
                "gpt-test",
                null,
                1000
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("eligibility: essay attempts auto-grade even when student score display is disabled")
    void isAutoGradeEligible_whenShowScoreDisabledButEssayExists_shouldStillReturnTrue() {
        SubmissionAttempt attempt = submittedAttempt();
        attempt.getAssignmentRecipient().getAssignment().setShowScore(false);
        whenAttemptFound(attempt);

        boolean eligible = service.isAutoGradeEligible(ATTEMPT_ID, CENTER_ID);

        assertThat(eligible).isTrue();
    }

    @Test
    @DisplayName("lifecycle: pending job transitions to running")
    void markRunning_whenPending_shouldSetStartedAt() {
        AIGradingJob job = job(submittedAttempt(), AIGradingJobStatus.PENDING);
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(AIGradingJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.markRunning(JOB_ID);

        assertThat(job.getStatus()).isEqualTo(AIGradingJobStatus.RUNNING);
        assertThat(job.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("complete: persists immutable item results and leaves submission unchanged")
    void completeJob_whenOutputIsValid_shouldPersistResultWithoutMutatingSubmission() {
        SubmissionAttempt attempt = submittedAttempt();
        SubmissionAnswer answer = attempt.getAnswers().get(0);
        AIGradingJob job = job(attempt, AIGradingJobStatus.RUNNING);
        job.setActiveJobKey(ATTEMPT_ID);
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(resultRepository.save(any(AIGradingResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.save(any(AIGradingJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubmissionAttemptStatus originalAttemptStatus = attempt.getStatus();
        String originalAnswerText = answer.getAnswerText();
        BigDecimal originalAnswerAutoScore = answer.getAutoScore();
        AIGradingOutput output = validOutput("3.50");

        service.completeJob(JOB_ID, output, "{\"status\":\"completed\"}");

        ArgumentCaptor<AIGradingResult> resultCaptor = ArgumentCaptor.forClass(AIGradingResult.class);
        verify(resultRepository).save(resultCaptor.capture());
        AIGradingResult result = resultCaptor.getValue();

        assertThat(job.getStatus()).isEqualTo(AIGradingJobStatus.COMPLETED);
        assertThat(job.getActiveJobKey()).isNull();
        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(result.getAiScore()).isEqualByComparingTo("3.50");
        assertThat(result.getMaxScore()).isEqualByComparingTo("5.00");
        assertThat(result.getItemResults()).hasSize(1);
        assertThat(result.getItemResults().get(0).getSubmissionAnswer()).isSameAs(answer);
        assertThat(result.getItemResults().get(0).getAssignmentItem()).isSameAs(answer.getAssignmentItem());
        assertThat(attempt.getStatus()).isEqualTo(originalAttemptStatus);
        assertThat(answer.getAnswerText()).isEqualTo(originalAnswerText);
        assertThat(answer.getAutoScore()).isEqualTo(originalAnswerAutoScore);
    }

    @Test
    @DisplayName("complete: item numbers map to assignment display order")
    void completeJob_whenProviderItemsAreUnordered_shouldMapByStableItemNumber() {
        SubmissionAttempt attempt = submittedAttempt();
        Assignment assignment = attempt.getAssignmentRecipient().getAssignment();
        AssignmentItem secondItem = AssignmentItem.builder()
                .id(ITEM_ID + 1)
                .assignment(assignment)
                .questionType(QuestionType.ESSAY)
                .contentJson(serializedDocument("Second essay"))
                .points(new BigDecimal("4.00"))
                .displayOrder(2)
                .build();
        assignment.getItems().add(secondItem);
        SubmissionAnswer secondAnswer = SubmissionAnswer.builder()
                .id(ANSWER_ID + 1)
                .attempt(attempt)
                .assignmentItem(secondItem)
                .answerText("Second response")
                .maxScore(new BigDecimal("4.00"))
                .build();
        attempt.getAnswers().add(0, secondAnswer);

        AIGradingJob job = job(attempt, AIGradingJobStatus.RUNNING);
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(resultRepository.save(any(AIGradingResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.save(any(AIGradingJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AIGradingOutput output = new AIGradingOutput(
                "Summary",
                "Feedback",
                "Ngữ pháp",
                new BigDecimal("0.9000"),
                criteria(),
                improvements(),
                List.of(
                        new AIGradingItemOutput(
                                2,
                                new BigDecimal("3.00"),
                                "Second feedback",
                                "Second rubric",
                                new BigDecimal("0.8000")
                        ),
                        new AIGradingItemOutput(
                                1,
                                new BigDecimal("4.00"),
                                "First feedback",
                                "First rubric",
                                new BigDecimal("0.8500")
                        )
                )
        );

        service.completeJob(JOB_ID, output, "{}");

        AIGradingResult result = job.getResult();
        assertThat(result.getItemResults()).extracting(itemResult -> itemResult.getAssignmentItem().getDisplayOrder())
                .containsExactly(1, 2);
        assertThat(result.getItemResults()).extracting(AIGradingItemResult::getAiScore)
                .containsExactly(new BigDecimal("4.00"), new BigDecimal("3.00"));
    }

    @Test
    @DisplayName("complete: rejects output with mismatched item count before persistence")
    void completeJob_whenItemCountDoesNotMatch_shouldReject() {
        AIGradingJob job = job(submittedAttempt(), AIGradingJobStatus.RUNNING);
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        AIGradingOutput output = new AIGradingOutput(
                "Summary",
                "Feedback",
                "Ngữ pháp",
                new BigDecimal("0.9000"),
                criteria(),
                improvements(),
                List.of()
        );

        assertThatThrownBy(() -> service.completeJob(JOB_ID, output, "{}"))
                .isInstanceOf(AIGradingProviderException.class);
        verify(resultRepository, never()).save(any());
        assertThat(job.getStatus()).isEqualTo(AIGradingJobStatus.RUNNING);
    }

    @Test
    @DisplayName("complete: rejects scores above assignment snapshot maximum")
    void completeJob_whenScoreExceedsMaximum_shouldReject() {
        AIGradingJob job = job(submittedAttempt(), AIGradingJobStatus.RUNNING);
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.completeJob(JOB_ID, validOutput("5.01"), "{}"))
                .isInstanceOf(AIGradingProviderException.class);
        verify(resultRepository, never()).save(any());
    }

    @Test
    @DisplayName("complete: rejects confidence outside zero-to-one range")
    void completeJob_whenConfidenceIsInvalid_shouldReject() {
        AIGradingJob job = job(submittedAttempt(), AIGradingJobStatus.RUNNING);
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        AIGradingOutput output = new AIGradingOutput(
                "Summary",
                "Feedback",
                "Ngữ pháp",
                new BigDecimal("1.1000"),
                criteria(),
                improvements(),
                List.of(new AIGradingItemOutput(
                        1,
                        new BigDecimal("3.00"),
                        "Item feedback",
                        "Rubric analysis",
                        new BigDecimal("0.8000")
                ))
        );

        assertThatThrownBy(() -> service.completeJob(JOB_ID, output, "{}"))
                .isInstanceOf(AIGradingProviderException.class);
        verify(resultRepository, never()).save(any());
    }

    @Test
    @DisplayName("fail: terminal failure clears active key and preserves submission")
    void failJob_whenRunning_shouldMarkFailedWithoutMutatingSubmission() {
        SubmissionAttempt attempt = submittedAttempt();
        AIGradingJob job = job(attempt, AIGradingJobStatus.RUNNING);
        job.setActiveJobKey(ATTEMPT_ID);
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(AIGradingJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.failJob(JOB_ID, "Provider timeout");

        assertThat(job.getStatus()).isEqualTo(AIGradingJobStatus.FAILED);
        assertThat(job.getActiveJobKey()).isNull();
        assertThat(job.getFailedAt()).isNotNull();
        assertThat(job.getErrorMessage()).isEqualTo("Provider timeout");
        assertThat(attempt.getStatus()).isEqualTo(SubmissionAttemptStatus.SUBMITTED);
    }

    private void whenAttemptFound(SubmissionAttempt attempt) {
        when(submissionAttemptRepository
                .findByIdAndAssignmentRecipient_Assignment_Center_IdAndAssignmentRecipient_Assignment_DeletedAtIsNull(
                        ATTEMPT_ID,
                        CENTER_ID
                )).thenReturn(Optional.of(attempt));
    }

    private SubmissionAttempt submittedAttempt() {
        Center center = new Center();
        center.setId(CENTER_ID);

        Assignment assignment = Assignment.builder()
                .id(70L)
                .center(center)
                .items(new ArrayList<>())
                .build();
        AssignmentRecipient recipient = AssignmentRecipient.builder()
                .id(80L)
                .assignment(assignment)
                .build();
        SubmissionAttempt attempt = SubmissionAttempt.builder()
                .id(ATTEMPT_ID)
                .assignmentRecipient(recipient)
                .status(SubmissionAttemptStatus.SUBMITTED)
                .answers(new ArrayList<>())
                .build();

        AssignmentItem item = AssignmentItem.builder()
                .id(ITEM_ID)
                .assignment(assignment)
                .questionType(QuestionType.ESSAY)
                .contentJson(serializedDocument("Explain the answer"))
                .gradingCriteriaName("Writing rubric")
                .gradingCriteriaContentJson(serializedDocument("Accuracy and clarity"))
                .points(new BigDecimal("5.00"))
                .displayOrder(1)
                .build();
        assignment.getItems().add(item);

        SubmissionAnswer answer = SubmissionAnswer.builder()
                .id(ANSWER_ID)
                .attempt(attempt)
                .assignmentItem(item)
                .answerText("Student response")
                .maxScore(new BigDecimal("5.00"))
                .build();
        attempt.getAnswers().add(answer);
        return attempt;
    }

    private AIGradingJob job(SubmissionAttempt attempt, AIGradingJobStatus status) {
        return AIGradingJob.builder()
                .id(JOB_ID)
                .submissionAttempt(attempt)
                .requestedBy(teacher)
                .status(status)
                .promptTemplateVersion("template-v1")
                .promptBuilderVersion("builder-v1")
                 .modelProvider(AIModelProvider.GEMINI)
                .modelName("gpt-test")
                .systemPrompt("system")
                .userPrompt("user")
                .build();
    }

    private AIGradingOutput validOutput(String score) {
        return new AIGradingOutput(
                "Summary",
                "Overall feedback",
                "Ngữ pháp",
                new BigDecimal("0.9000"),
                criteria(),
                improvements(),
                List.of(new AIGradingItemOutput(
                        1,
                        new BigDecimal(score),
                        "Item feedback",
                        "Rubric analysis",
                        new BigDecimal("0.8000")
                ))
        );
    }

    private List<AIGradingCriterionOutput> criteria() {
        return List.of(
                new AIGradingCriterionOutput(
                        "Hoàn thành nhiệm vụ",
                        new BigDecimal("3.50"),
                        new BigDecimal("5.00"),
                        "Đã bám đề nhưng cần phát triển ý sâu hơn."
                ),
                new AIGradingCriterionOutput(
                        "Ngữ pháp",
                        new BigDecimal("3.00"),
                        new BigDecimal("5.00"),
                        "Cần kiểm soát câu phức tốt hơn."
                )
        );
    }

    private List<AIGradingImprovementOutput> improvements() {
        return List.of(
                new AIGradingImprovementOutput(
                        "Ngữ pháp",
                        "Một số câu thiếu độ linh hoạt.",
                        "Kết hợp câu đơn và câu phức tự nhiên hơn.",
                        "Ví dụ: dùng mệnh đề quan hệ để nối ý."
                )
        );
    }
}
