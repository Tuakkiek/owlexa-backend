package com.owlexa.owlexabackend.modules.student_submission.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.richtext.RichTextDocumentService;
import com.owlexa.owlexabackend.modules.file.mapper.FileMapper;
import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItem;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItemOption;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipient;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipientStatus;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentStatus;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentTargetType;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingJob;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingJobStatus;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingResult;
import com.owlexa.owlexabackend.modules.ai_grading.provider.openai.OpenAIGradingResultParser;
import com.owlexa.owlexabackend.modules.ai_grading.repository.AIGradingResultRepository;
import com.owlexa.owlexabackend.modules.assignment.repository.AssignmentRecipientRepository;
import com.owlexa.owlexabackend.modules.ai_grading.service.AIGradingOutputReader;
import com.owlexa.owlexabackend.modules.ai_grading.service.AIGradingService;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.student_submission.dto.request.SaveSubmissionAnswersRequest;
import com.owlexa.owlexabackend.modules.student_submission.dto.request.SubmissionAnswerRequest;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.StudentAttemptDetailResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.TeacherSubmissionSummaryResponse;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAnswer;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAnswerOption;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttempt;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttemptStatus;
import com.owlexa.owlexabackend.modules.student_submission.mapper.SubmissionMapper;
import com.owlexa.owlexabackend.modules.student_submission.repository.SubmissionAttemptRepository;
import com.owlexa.owlexabackend.modules.teacher_review.repository.TeacherReviewRepository;
import com.owlexa.owlexabackend.modules.user.entity.Center;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.owlexa.owlexabackend.support.RichTextTestFixtures.serializedDocument;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock private AssignmentRecipientRepository assignmentRecipientRepository;
    @Mock private SubmissionAttemptRepository submissionAttemptRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private MembershipRepository membershipRepository;
    @Mock private AIGradingResultRepository aiGradingResultRepository;
    @Mock private AIGradingService aiGradingService;
    @Mock private TeacherReviewRepository teacherReviewRepository;

    private SubmissionService service;

    private static final Long CENTER_ID = 10L;
    private static final Long STUDENT_ID = 20L;
    private static final Long TEACHER_ID = 30L;
    private static final Long ASSIGNMENT_ID = 40L;
    private static final Long RECIPIENT_ID = 50L;
    private static final Long ATTEMPT_ID = 60L;
    private static final Long MC_ITEM_ID = 70L;
    private static final Long ESSAY_ITEM_ID = 71L;
    private static final Long CORRECT_OPTION_ID = 80L;
    private static final Long WRONG_OPTION_ID = 81L;
    private static final Long OTHER_OPTION_ID = 82L;

    private Center center;
    private User student;
    private User teacher;

    @BeforeEach
    void setUp() {
        RichTextDocumentService documentService = new RichTextDocumentService(new ObjectMapper());
        FileMapper fileMapper = new FileMapper();
        service = new SubmissionService(
                assignmentRecipientRepository,
                submissionAttemptRepository,
                authorizationService,
                membershipRepository,
                new SubmissionMapper(
                        documentService,
                        fileMapper
                ),
                aiGradingResultRepository,
                aiGradingService,
                new AIGradingOutputReader(new OpenAIGradingResultParser(new ObjectMapper())),
                teacherReviewRepository
        );

        TenantContext.setCurrentTenantId(CENTER_ID);

        center = new Center();
        center.setId(CENTER_ID);
        student = user(STUDENT_ID, Role.STUDENT, "Student One");
        teacher = user(TEACHER_ID, Role.TEACHER, "Teacher One");

        lenient().when(authorizationService.getCurrentUser()).thenReturn(student);
        lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(STUDENT_ID, CENTER_ID)).thenReturn(true);
        lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("start: resumes existing in-progress attempt")
    void startOrResume_whenInProgressExists_shouldReturnExistingAttempt() {
        AssignmentRecipient recipient = recipient(activeAssignment(null, null, null));
        SubmissionAttempt attempt = attempt(recipient, SubmissionAttemptStatus.IN_PROGRESS, 1);
        when(assignmentRecipientRepository.findByAssignment_IdAndStudentUser_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNull(
                ASSIGNMENT_ID, STUDENT_ID, CENTER_ID)).thenReturn(Optional.of(recipient));
        when(submissionAttemptRepository.findByAssignmentRecipient_IdAndStatus(RECIPIENT_ID, SubmissionAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));

        StudentAttemptDetailResponse response = service.startOrResumeAttempt(ASSIGNMENT_ID, null);

        assertThat(response.getId()).isEqualTo(ATTEMPT_ID);
        assertThat(response.getAttemptNumber()).isEqualTo(1);
        verify(submissionAttemptRepository, never()).save(any(SubmissionAttempt.class));
    }

    @Test
    @DisplayName("start: creates first attempt with snapshot and active key")
    void startOrResume_whenNoAttemptExists_shouldCreateFirstAttempt() {
        Assignment assignment = activeAssignment(null, null, 3);
        AssignmentRecipient recipient = recipient(assignment);
        when(assignmentRecipientRepository.findByAssignment_IdAndStudentUser_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNull(
                ASSIGNMENT_ID, STUDENT_ID, CENTER_ID)).thenReturn(Optional.of(recipient));
        when(submissionAttemptRepository.findByAssignmentRecipient_IdAndStatus(RECIPIENT_ID, SubmissionAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(submissionAttemptRepository.countByAssignmentRecipient_Id(RECIPIENT_ID)).thenReturn(0L);
        when(submissionAttemptRepository.findTopByAssignmentRecipient_IdOrderByAttemptNumberDesc(RECIPIENT_ID))
                .thenReturn(Optional.empty());
        when(submissionAttemptRepository.save(any(SubmissionAttempt.class))).thenAnswer(invocation -> {
            SubmissionAttempt saved = invocation.getArgument(0);
            saved.setId(ATTEMPT_ID);
            return saved;
        });

        StudentAttemptDetailResponse response = service.startOrResumeAttempt(ASSIGNMENT_ID, null);

        assertThat(response.getAttemptNumber()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo(SubmissionAttemptStatus.IN_PROGRESS);
        assertThat(response.getAssignmentTitleSnapshot()).isEqualTo("Homework 1");
        assertThat(response.getAssignmentContent().toString()).contains("PART 3");
        assertThat(response.getAssignmentContent().toString()).contains("Directions");
        verify(submissionAttemptRepository).save(any(SubmissionAttempt.class));
    }

    @Test
    @DisplayName("student attempt response: excludes teacher-only question fields")
    void studentAttemptResponse_shouldExcludeTeacherOnlyQuestionFields() throws Exception {
        Assignment assignment = activeAssignment(null, null, null);
        AssignmentItem item = assignment.getItems().get(0);

        item.setExplanationJson(serializedDocument("Teacher explanation"));
        item.setSampleAnswerJson(serializedDocument("Expected answer"));
        item.setGradingCriteriaName("Teacher rubric");
        item.setGradingCriteriaContentJson(serializedDocument("Rubric details"));
        SubmissionAttempt attempt = attempt(recipient(assignment), SubmissionAttemptStatus.IN_PROGRESS, 1);

        RichTextDocumentService documentService = new RichTextDocumentService(new ObjectMapper());
        FileMapper fileMapper = new FileMapper();
        StudentAttemptDetailResponse response = new SubmissionMapper(
                documentService,
                fileMapper
        ).toStudentAttemptDetailResponse(attempt);

        String json = new ObjectMapper().writeValueAsString(response);

        assertThat(json)
                .doesNotContain("explanation")
                .doesNotContain("sampleAnswer")
                .doesNotContain("gradingCriteria")
                .doesNotContain("isCorrect")
                .contains("assignmentItemId", "displayOrder", "options");
    }

    @Test
    @DisplayName("start: creates next attempt number after submitted attempts")
    void startOrResume_whenPreviousAttemptExists_shouldCreateNextAttemptNumber() {
        Assignment assignment = activeAssignment(null, null, 3);
        AssignmentRecipient recipient = recipient(assignment);
        SubmissionAttempt previous = attempt(recipient, SubmissionAttemptStatus.SUBMITTED, 1);
        when(assignmentRecipientRepository.findByAssignment_IdAndStudentUser_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNull(
                ASSIGNMENT_ID, STUDENT_ID, CENTER_ID)).thenReturn(Optional.of(recipient));
        when(submissionAttemptRepository.findByAssignmentRecipient_IdAndStatus(RECIPIENT_ID, SubmissionAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(submissionAttemptRepository.countByAssignmentRecipient_Id(RECIPIENT_ID)).thenReturn(1L);
        when(submissionAttemptRepository.findTopByAssignmentRecipient_IdOrderByAttemptNumberDesc(RECIPIENT_ID))
                .thenReturn(Optional.of(previous));
        when(submissionAttemptRepository.save(any(SubmissionAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentAttemptDetailResponse response = service.startOrResumeAttempt(ASSIGNMENT_ID, null);

        assertThat(response.getAttemptNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("start: attempt limit blocks new attempt")
    void startOrResume_whenAttemptLimitReached_shouldThrowBadRequest() {
        AssignmentRecipient recipient = recipient(activeAssignment(null, null, 1));
        when(assignmentRecipientRepository.findByAssignment_IdAndStudentUser_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNull(
                ASSIGNMENT_ID, STUDENT_ID, CENTER_ID)).thenReturn(Optional.of(recipient));
        when(submissionAttemptRepository.findByAssignmentRecipient_IdAndStatus(RECIPIENT_ID, SubmissionAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(submissionAttemptRepository.countByAssignmentRecipient_Id(RECIPIENT_ID)).thenReturn(1L);

        assertThatThrownBy(() -> service.startOrResumeAttempt(ASSIGNMENT_ID, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("start: non-recipient student cannot start assignment")
    void startOrResume_whenStudentIsNotRecipient_shouldThrowNotFound() {
        when(assignmentRecipientRepository.findByAssignment_IdAndStudentUser_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNull(
                ASSIGNMENT_ID, STUDENT_ID, CENTER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startOrResumeAttempt(ASSIGNMENT_ID, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("start: assignment outside time window is blocked")
    void startOrResume_whenAssignmentNotOpenOrPastDue_shouldThrowBadRequest() {
        Assignment notOpen = activeAssignment(Instant.now().plusSeconds(3600), null, null);
        when(assignmentRecipientRepository.findByAssignment_IdAndStudentUser_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNull(
                ASSIGNMENT_ID, STUDENT_ID, CENTER_ID)).thenReturn(Optional.of(recipient(notOpen)));

        assertThatThrownBy(() -> service.startOrResumeAttempt(ASSIGNMENT_ID, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("save: full replacement allows empty answers and updates lastSavedAt")
    void saveAnswers_whenEmptyList_shouldClearAnswersAndUpdateLastSavedAt() {
        AssignmentRecipient recipient = recipient(activeAssignment(null, null, null));
        SubmissionAttempt attempt = attempt(recipient, SubmissionAttemptStatus.IN_PROGRESS, 1);
        attempt.getAnswers().add(answer(attempt, mcItem(recipient.getAssignment()), CORRECT_OPTION_ID));
        whenStudentAttemptFound(attempt);
        when(submissionAttemptRepository.save(any(SubmissionAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentAttemptDetailResponse response = service.saveAnswers(
                ATTEMPT_ID,
                SaveSubmissionAnswersRequest.builder().answers(List.of()).build()
        );

        assertThat(response.getAnswers()).isEmpty();
        assertThat(attempt.getAnswers()).isEmpty();
        assertThat(attempt.getLastSavedAt()).isNotNull();
    }

    @Test
    @DisplayName("save: duplicate assignment item answers are rejected")
    void saveAnswers_whenDuplicateAssignmentItem_shouldThrowBadRequest() {
        SubmissionAttempt attempt = attempt(recipient(activeAssignment(null, null, null)), SubmissionAttemptStatus.IN_PROGRESS, 1);
        whenStudentAttemptFound(attempt);
        SaveSubmissionAnswersRequest request = SaveSubmissionAnswersRequest.builder()
                .answers(List.of(
                        mcAnswerRequest(MC_ITEM_ID, List.of(CORRECT_OPTION_ID)),
                        mcAnswerRequest(MC_ITEM_ID, List.of(CORRECT_OPTION_ID))
                ))
                .build();

        assertThatThrownBy(() -> service.saveAnswers(ATTEMPT_ID, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("save: selected option must belong to the assignment item")
    void saveAnswers_whenOptionBelongsToAnotherItem_shouldThrowBadRequest() {
        SubmissionAttempt attempt = attempt(recipient(activeAssignment(null, null, null)), SubmissionAttemptStatus.IN_PROGRESS, 1);
        whenStudentAttemptFound(attempt);

        assertThatThrownBy(() -> service.saveAnswers(
                ATTEMPT_ID,
                SaveSubmissionAnswersRequest.builder()
                        .answers(List.of(mcAnswerRequest(MC_ITEM_ID, List.of(OTHER_OPTION_ID))))
                        .build()
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("save: validates answer shape by question type")
    void saveAnswers_whenAnswerShapeIsInvalid_shouldThrowBadRequest() {
        SubmissionAttempt attempt = attempt(recipient(activeAssignment(null, null, null)), SubmissionAttemptStatus.IN_PROGRESS, 1);
        whenStudentAttemptFound(attempt);

        assertThatThrownBy(() -> service.saveAnswers(
                ATTEMPT_ID,
                SaveSubmissionAnswersRequest.builder()
                        .answers(List.of(essayAnswerRequest(ESSAY_ITEM_ID, "Essay", List.of(CORRECT_OPTION_ID))))
                        .build()
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("submit: scores exact-match multiple choice and leaves essay unscored")
    void submitAttempt_whenWithinDueTime_shouldScoreMcAndSubmit() {
        Assignment assignment = activeAssignment(null, Instant.now().plusSeconds(3600), null);
        AssignmentRecipient recipient = recipient(assignment);
        SubmissionAttempt attempt = attempt(recipient, SubmissionAttemptStatus.IN_PROGRESS, 1);
        attempt.setActiveAttemptKey(RECIPIENT_ID);
        attempt.getAnswers().add(answer(attempt, mcItem(assignment), CORRECT_OPTION_ID));
        attempt.getAnswers().add(essayAnswer(attempt, essayItem(assignment), "My essay"));
        whenStudentAttemptFound(attempt);
        when(submissionAttemptRepository.save(any(SubmissionAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentAttemptDetailResponse response = service.submitAttempt(ATTEMPT_ID);

        assertThat(response.getStatus()).isEqualTo(SubmissionAttemptStatus.SUBMITTED);
        assertThat(response.getAutoScore()).isEqualByComparingTo("2.00");
        assertThat(response.getMaxScore()).isEqualByComparingTo("7.00");
        assertThat(attempt.getActiveAttemptKey()).isNull();
        assertThat(attempt.getAnswers())
                .filteredOn(answer -> answer.getAssignmentItem().getQuestionType() == QuestionType.ESSAY)
                .first()
                .extracting(SubmissionAnswer::getAutoScore)
                .isNull();
    }

    @Test
    @DisplayName("submit: late submit becomes auto-submitted")
    void submitAttempt_whenPastDue_shouldAutoSubmit() {
        Assignment assignment = activeAssignment(null, Instant.now().minusSeconds(60), null);
        AssignmentRecipient recipient = recipient(assignment);
        SubmissionAttempt attempt = attempt(recipient, SubmissionAttemptStatus.IN_PROGRESS, 1);
        whenStudentAttemptFound(attempt);
        when(submissionAttemptRepository.save(any(SubmissionAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentAttemptDetailResponse response = service.submitAttempt(ATTEMPT_ID);

        assertThat(response.getStatus()).isEqualTo(SubmissionAttemptStatus.AUTO_SUBMITTED);
        assertThat(response.getAutoScore()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("submit: submitted attempts cannot be submitted again")
    void submitAttempt_whenAlreadySubmitted_shouldThrowBadRequest() {
        SubmissionAttempt attempt = attempt(recipient(activeAssignment(null, null, null)), SubmissionAttemptStatus.SUBMITTED, 1);
        whenStudentAttemptFound(attempt);

        assertThatThrownBy(() -> service.submitAttempt(ATTEMPT_ID))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("submit+auto-grade: commits submission then triggers automatic AI grading")
    void submitAttemptWithAutoGrading_shouldSubmitAndTriggerAutoGrading() {
        Assignment assignment = activeAssignment(null, Instant.now().plusSeconds(3600), null);
        AssignmentRecipient recipient = recipient(assignment);
        SubmissionAttempt attempt = attempt(recipient, SubmissionAttemptStatus.IN_PROGRESS, 1);
        attempt.setActiveAttemptKey(RECIPIENT_ID);
        attempt.getAnswers().add(answer(attempt, mcItem(assignment), CORRECT_OPTION_ID));
        attempt.getAnswers().add(essayAnswer(attempt, essayItem(assignment), "My essay"));
        whenStudentAttemptFound(attempt);
        when(submissionAttemptRepository.save(any(SubmissionAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiGradingService.autoGradeOnSubmit(ATTEMPT_ID, CENTER_ID, STUDENT_ID)).thenReturn(true);
        when(aiGradingResultRepository.findTopBySubmissionAttempt_IdAndJob_StatusOrderByCreatedAtDesc(
                ATTEMPT_ID,
                AIGradingJobStatus.COMPLETED
        )).thenReturn(Optional.of(completedAiResult(attempt, "4.00")));

        StudentAttemptDetailResponse response = service.submitAttemptWithAutoGrading(ATTEMPT_ID);

        assertThat(response.getStatus()).isEqualTo(SubmissionAttemptStatus.SUBMITTED);
        assertThat(response.getAutoScore()).isEqualByComparingTo("2.00");
        assertThat(response.getAiResult()).isNotNull();
        assertThat(response.getAiResult().getAiScore()).isEqualByComparingTo("4.00");
        verify(aiGradingService).autoGradeOnSubmit(ATTEMPT_ID, CENTER_ID, STUDENT_ID);
    }

    @Test
    @DisplayName("submit+auto-grade: AI grading failure does not block the submission")
    void submitAttemptWithAutoGrading_whenAiGradingThrows_shouldStillSubmit() {
        Assignment assignment = activeAssignment(null, Instant.now().plusSeconds(3600), null);
        AssignmentRecipient recipient = recipient(assignment);
        SubmissionAttempt attempt = attempt(recipient, SubmissionAttemptStatus.IN_PROGRESS, 1);
        attempt.setActiveAttemptKey(RECIPIENT_ID);
        attempt.getAnswers().add(essayAnswer(attempt, essayItem(assignment), "My essay"));
        whenStudentAttemptFound(attempt);
        when(submissionAttemptRepository.save(any(SubmissionAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiGradingService.autoGradeOnSubmit(ATTEMPT_ID, CENTER_ID, STUDENT_ID))
                .thenThrow(new RuntimeException("AI provider unavailable"));

        StudentAttemptDetailResponse response = service.submitAttemptWithAutoGrading(ATTEMPT_ID);

        assertThat(response.getStatus()).isEqualTo(SubmissionAttemptStatus.SUBMITTED);
    }

    @Test
    @DisplayName("submit+auto-grade: blank essay still creates a placeholder answer and triggers AI grading")
    void submitAttemptWithAutoGrading_whenEssayLeftBlank_shouldStillTriggerAutoGrading() {
        Assignment assignment = activeAssignment(null, Instant.now().plusSeconds(3600), null);
        AssignmentRecipient recipient = recipient(assignment);
        SubmissionAttempt attempt = attempt(recipient, SubmissionAttemptStatus.IN_PROGRESS, 1);
        attempt.setActiveAttemptKey(RECIPIENT_ID);
        attempt.getAnswers().add(answer(attempt, mcItem(assignment), CORRECT_OPTION_ID));
        whenStudentAttemptFound(attempt);
        when(submissionAttemptRepository.save(any(SubmissionAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiGradingService.autoGradeOnSubmit(ATTEMPT_ID, CENTER_ID, STUDENT_ID)).thenReturn(true);

        StudentAttemptDetailResponse response = service.submitAttemptWithAutoGrading(ATTEMPT_ID);

        assertThat(response.getStatus()).isEqualTo(SubmissionAttemptStatus.SUBMITTED);
        assertThat(attempt.getAnswers())
                .filteredOn(answer -> answer.getAssignmentItem().getQuestionType() == QuestionType.ESSAY)
                .hasSize(1)
                .first()
                .extracting(SubmissionAnswer::getAnswerText)
                .isNull();
        verify(aiGradingService).autoGradeOnSubmit(ATTEMPT_ID, CENTER_ID, STUDENT_ID);
    }

    @Test
    @DisplayName("teacher submissions: includes recipients without attempts")
    void findAssignmentSubmissions_whenRecipientHasNoAttempt_shouldReturnSummaryWithoutLatestAttempt() {
        when(authorizationService.getCurrentUser()).thenReturn(teacher);
        AssignmentRecipient recipient = recipient(activeAssignment(null, null, null));
        when(assignmentRecipientRepository.findAllByAssignment_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNull(
                ASSIGNMENT_ID,
                CENTER_ID,
                PageRequest.of(0, 20)
        )).thenReturn(new PageImpl<>(List.of(recipient)));
        when(submissionAttemptRepository.findTopByAssignmentRecipient_IdOrderByStartedAtDesc(RECIPIENT_ID))
                .thenReturn(Optional.empty());
        when(submissionAttemptRepository.countByAssignmentRecipient_Id(RECIPIENT_ID)).thenReturn(0L);

        Page<TeacherSubmissionSummaryResponse> response = service.findAssignmentSubmissions(
                ASSIGNMENT_ID,
                PageRequest.of(0, 20)
        );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getRecipientId()).isEqualTo(RECIPIENT_ID);
        assertThat(response.getContent().get(0).getLatestAttemptId()).isNull();
        assertThat(response.getContent().get(0).getAttemptsCount()).isZero();
    }

    @Test
    @DisplayName("auth: non-student cannot manage submission attempts")
    void startOrResume_whenCurrentUserIsNotStudent_shouldThrowAccessDenied() {
        when(authorizationService.getCurrentUser()).thenReturn(teacher);

        assertThatThrownBy(() -> service.startOrResumeAttempt(ASSIGNMENT_ID, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("auth: missing tenant context is rejected")
    void startOrResume_whenTenantMissing_shouldThrowBadRequest() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.startOrResumeAttempt(ASSIGNMENT_ID, null))
                .isInstanceOf(BadRequestException.class);
    }

    // ── Phase 1: Deadline Enforcement Tests ──────────────────────────────────

    @Test
    @DisplayName("deadline: save succeeds when now < dueAt")
    void saveAnswers_whenBeforeDeadline_shouldSucceed() {
        Assignment assignment = activeAssignment(null, Instant.now().plusSeconds(3600), null);
        AssignmentRecipient recipient = recipient(assignment);
        SubmissionAttempt attempt = attempt(recipient, SubmissionAttemptStatus.IN_PROGRESS, 1);
        whenStudentAttemptFound(attempt);
        when(submissionAttemptRepository.save(any(SubmissionAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentAttemptDetailResponse response = service.saveAnswers(
                ATTEMPT_ID,
                SaveSubmissionAnswersRequest.builder()
                        .answers(List.of(mcAnswerRequest(MC_ITEM_ID, List.of(CORRECT_OPTION_ID))))
                        .build()
        );

        assertThat(response.getStatus()).isEqualTo(SubmissionAttemptStatus.IN_PROGRESS);
        assertThat(attempt.getLastSavedAt()).isNotNull();
    }

    @Test
    @DisplayName("deadline: save rejected when now >= dueAt — attempt finalized")
    void saveAnswers_whenPastDeadline_shouldRejectAndFinalize() {
        Assignment assignment = activeAssignment(null, Instant.now().minusSeconds(60), null);
        AssignmentRecipient recipient = recipient(assignment);
        SubmissionAttempt attempt = attempt(recipient, SubmissionAttemptStatus.IN_PROGRESS, 1);
        whenStudentAttemptFound(attempt);
        when(submissionAttemptRepository.save(any(SubmissionAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.saveAnswers(
                ATTEMPT_ID,
                SaveSubmissionAnswersRequest.builder()
                        .answers(List.of(mcAnswerRequest(MC_ITEM_ID, List.of(CORRECT_OPTION_ID))))
                        .build()
        )).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("hết hạn");

        assertThat(attempt.getStatus()).isEqualTo(SubmissionAttemptStatus.AUTO_SUBMITTED);
    }

    @Test
    @DisplayName("deadline: manual submit after deadline returns auto-submitted — no new answers accepted")
    void submitAttempt_whenPastDeadline_shouldAutoSubmitWithoutNewAnswers() {
        Assignment assignment = activeAssignment(null, Instant.now().minusSeconds(60), null);
        AssignmentRecipient recipient = recipient(assignment);
        SubmissionAttempt attempt = attempt(recipient, SubmissionAttemptStatus.IN_PROGRESS, 1);
        attempt.getAnswers().add(answer(attempt, mcItem(assignment), CORRECT_OPTION_ID));
        whenStudentAttemptFound(attempt);
        when(submissionAttemptRepository.save(any(SubmissionAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentAttemptDetailResponse response = service.submitAttempt(ATTEMPT_ID);

        assertThat(response.getStatus()).isEqualTo(SubmissionAttemptStatus.AUTO_SUBMITTED);
        assertThat(attempt.getActiveAttemptKey()).isNull();
    }

    @Test
    @DisplayName("deadline: auto-submitted attempt is not editable")
    void saveAnswers_whenAutoSubmitted_shouldThrowBadRequest() {
        Assignment assignment = activeAssignment(null, Instant.now().plusSeconds(3600), null);
        AssignmentRecipient recipient = recipient(assignment);
        SubmissionAttempt attempt = attempt(recipient, SubmissionAttemptStatus.AUTO_SUBMITTED, 1);
        whenStudentAttemptFound(attempt);

        assertThatThrownBy(() -> service.saveAnswers(
                ATTEMPT_ID,
                SaveSubmissionAnswersRequest.builder()
                        .answers(List.of(mcAnswerRequest(MC_ITEM_ID, List.of(CORRECT_OPTION_ID))))
                        .build()
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("deadline: student cannot bypass deadline via direct API save call")
    void saveAnswers_directApiCallAfterDeadline_shouldBeBlocked() {
        Assignment assignment = activeAssignment(null, Instant.now().minusSeconds(1), null);
        AssignmentRecipient recipient = recipient(assignment);
        SubmissionAttempt attempt = attempt(recipient, SubmissionAttemptStatus.IN_PROGRESS, 1);
        whenStudentAttemptFound(attempt);
        when(submissionAttemptRepository.save(any(SubmissionAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.saveAnswers(
                ATTEMPT_ID,
                SaveSubmissionAnswersRequest.builder()
                        .answers(List.of(essayAnswerRequest(ESSAY_ITEM_ID, "Sneaky late answer", List.of())))
                        .build()
        )).isInstanceOf(BadRequestException.class);

        assertThat(attempt.getStatus()).isEqualTo(SubmissionAttemptStatus.AUTO_SUBMITTED);
    }

    @Test
    @DisplayName("deadline: boundary test — now == dueAt is treated as past due")
    void saveAnswers_whenExactlyAtDeadline_shouldReject() {
        Instant exactDeadline = Instant.now();
        Assignment assignment = activeAssignment(null, exactDeadline, null);
        AssignmentRecipient recipient = recipient(assignment);
        SubmissionAttempt attempt = attempt(recipient, SubmissionAttemptStatus.IN_PROGRESS, 1);
        whenStudentAttemptFound(attempt);
        when(submissionAttemptRepository.save(any(SubmissionAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.saveAnswers(
                ATTEMPT_ID,
                SaveSubmissionAnswersRequest.builder()
                        .answers(List.of(mcAnswerRequest(MC_ITEM_ID, List.of(CORRECT_OPTION_ID))))
                        .build()
        )).isInstanceOf(BadRequestException.class);

        assertThat(attempt.getStatus()).isEqualTo(SubmissionAttemptStatus.AUTO_SUBMITTED);
    }

    @Test
    @DisplayName("deadline: normal submit before deadline is not regressed")
    void submitAttempt_whenBeforeDeadline_shouldSubmitNormally() {
        Assignment assignment = activeAssignment(null, Instant.now().plusSeconds(3600), null);
        AssignmentRecipient recipient = recipient(assignment);
        SubmissionAttempt attempt = attempt(recipient, SubmissionAttemptStatus.IN_PROGRESS, 1);
        attempt.setActiveAttemptKey(RECIPIENT_ID);
        attempt.getAnswers().add(answer(attempt, mcItem(assignment), CORRECT_OPTION_ID));
        attempt.getAnswers().add(essayAnswer(attempt, essayItem(assignment), "My essay"));
        whenStudentAttemptFound(attempt);
        when(submissionAttemptRepository.save(any(SubmissionAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentAttemptDetailResponse response = service.submitAttempt(ATTEMPT_ID);

        assertThat(response.getStatus()).isEqualTo(SubmissionAttemptStatus.SUBMITTED);
        assertThat(response.getAutoScore()).isEqualByComparingTo("2.00");
        assertThat(response.getMaxScore()).isEqualByComparingTo("7.00");
        assertThat(attempt.getActiveAttemptKey()).isNull();
    }

    @Test
    @DisplayName("deadline: resume past-due attempt returns auto-submitted result")
    void startOrResume_whenInProgressAndPastDue_shouldFinalizeAndReturn() {
        Assignment assignment = activeAssignment(null, Instant.now().minusSeconds(60), null);
        AssignmentRecipient recipient = recipient(assignment);
        SubmissionAttempt attempt = attempt(recipient, SubmissionAttemptStatus.IN_PROGRESS, 1);
        when(assignmentRecipientRepository.findByAssignment_IdAndStudentUser_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNull(
                ASSIGNMENT_ID, STUDENT_ID, CENTER_ID)).thenReturn(Optional.of(recipient));
        when(submissionAttemptRepository.findByAssignmentRecipient_IdAndStatus(RECIPIENT_ID, SubmissionAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));
        when(submissionAttemptRepository.save(any(SubmissionAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentAttemptDetailResponse response = service.startOrResumeAttempt(ASSIGNMENT_ID, null);

        assertThat(response.getStatus()).isEqualTo(SubmissionAttemptStatus.AUTO_SUBMITTED);
    }

    @Test
    @DisplayName("deadline: getAttemptDetail finalizes expired in-progress attempt")
    void getAttemptDetail_whenInProgressAndPastDue_shouldFinalize() {
        Assignment assignment = activeAssignment(null, Instant.now().minusSeconds(60), null);
        AssignmentRecipient recipient = recipient(assignment);
        SubmissionAttempt attempt = attempt(recipient, SubmissionAttemptStatus.IN_PROGRESS, 1);
        whenStudentAttemptFound(attempt);
        when(submissionAttemptRepository.save(any(SubmissionAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentAttemptDetailResponse response = service.getAttemptDetail(ATTEMPT_ID);

        assertThat(response.getStatus()).isEqualTo(SubmissionAttemptStatus.AUTO_SUBMITTED);
    }

    private void whenStudentAttemptFound(SubmissionAttempt attempt) {
        when(submissionAttemptRepository.findByIdAndAssignmentRecipient_StudentUser_IdAndAssignmentRecipient_Assignment_Center_IdAndAssignmentRecipient_Assignment_DeletedAtIsNull(
                ATTEMPT_ID,
                STUDENT_ID,
                CENTER_ID
        )).thenReturn(Optional.of(attempt));
    }

    private Assignment activeAssignment(Instant openAt, Instant dueAt, Integer attemptLimit) {
        Assignment assignment = Assignment.builder()
                .center(center)
                .status(AssignmentStatus.ACTIVE)
                .title("Homework 1")
                .contentJson(serializedDocument("PART 3\n\nDirections: You will hear some conversations."))
                .openAt(openAt)
                .dueAt(dueAt)
                .attemptLimit(attemptLimit)
                .items(new ArrayList<>())
                .build();
        assignment.setId(ASSIGNMENT_ID);
        assignment.getItems().add(mcItem(assignment));
        assignment.getItems().add(essayItem(assignment));
        return assignment;
    }

    private AssignmentRecipient recipient(Assignment assignment) {
        AssignmentRecipient recipient = AssignmentRecipient.builder()
                .assignment(assignment)
                .studentUser(student)
                .sourceType(AssignmentTargetType.STUDENT)
                .status(AssignmentRecipientStatus.ASSIGNED)
                .assignedAt(Instant.now())
                .build();
        recipient.setId(RECIPIENT_ID);
        return recipient;
    }

    private SubmissionAttempt attempt(AssignmentRecipient recipient, SubmissionAttemptStatus status, int attemptNumber) {
        SubmissionAttempt attempt = SubmissionAttempt.builder()
                .assignmentRecipient(recipient)
                .status(status)
                .attemptNumber(attemptNumber)
                .assignmentTitleSnapshot(recipient.getAssignment().getTitle())
                .startedAt(Instant.now())
                .activeAttemptKey(status == SubmissionAttemptStatus.IN_PROGRESS ? recipient.getId() : null)
                .answers(new ArrayList<>())
                .build();
        attempt.setId(ATTEMPT_ID);
        return attempt;
    }

    private AssignmentItem mcItem(Assignment assignment) {
        AssignmentItem item = AssignmentItem.builder()
                .assignment(assignment)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .contentJson(serializedDocument("Choose correct options"))
                .points(new BigDecimal("2.00"))
                .displayOrder(1)
                .options(new ArrayList<>())
                .build();
        item.setId(MC_ITEM_ID);
        item.getOptions().add(option(item, CORRECT_OPTION_ID, true, 1));
        item.getOptions().add(option(item, WRONG_OPTION_ID, false, 2));
        return item;
    }

    private AssignmentItem essayItem(Assignment assignment) {
        AssignmentItem item = AssignmentItem.builder()
                .assignment(assignment)
                .questionType(QuestionType.ESSAY)
                .contentJson(serializedDocument("Write essay"))
                .points(new BigDecimal("5.00"))
                .displayOrder(2)
                .options(new ArrayList<>())
                .build();
        item.setId(ESSAY_ITEM_ID);
        return item;
    }

    private AssignmentItemOption option(AssignmentItem item, Long id, boolean correct, int order) {
        AssignmentItemOption option = AssignmentItemOption.builder()
                .assignmentItem(item)
                .content("Option " + id)
                .isCorrect(correct)
                .displayOrder(order)
                .build();
        option.setId(id);
        return option;
    }

    private SubmissionAnswer answer(SubmissionAttempt attempt, AssignmentItem item, Long selectedOptionId) {
        AssignmentItemOption selected = item.getOptions().stream()
                .filter(option -> option.getId().equals(selectedOptionId))
                .findFirst()
                .orElseThrow();
        SubmissionAnswer answer = SubmissionAnswer.builder()
                .attempt(attempt)
                .assignmentItem(item)
                .selectedOptions(new ArrayList<>())
                .build();
        answer.getSelectedOptions().add(SubmissionAnswerOption.builder()
                .submissionAnswer(answer)
                .assignmentItemOption(selected)
                .build());
        return answer;
    }

    private SubmissionAnswer essayAnswer(SubmissionAttempt attempt, AssignmentItem item, String text) {
        return SubmissionAnswer.builder()
                .attempt(attempt)
                .assignmentItem(item)
                .answerText(text)
                .selectedOptions(new ArrayList<>())
                .build();
    }

    private AIGradingResult completedAiResult(SubmissionAttempt attempt, String aiScore) {
        AIGradingJob job = AIGradingJob.builder()
                .id(999L)
                .status(AIGradingJobStatus.COMPLETED)
                .submissionAttempt(attempt)
                .build();
        return AIGradingResult.builder()
                .id(1000L)
                .job(job)
                .submissionAttempt(attempt)
                .summary("AI summary")
                .overallFeedback("AI overall feedback")
                .aiScore(new BigDecimal(aiScore))
                .maxScore(new BigDecimal("5.00"))
                .confidence(new BigDecimal("0.9000"))
                .itemResults(new ArrayList<>())
                .build();
    }

    private SubmissionAnswerRequest mcAnswerRequest(Long itemId, List<Long> selectedOptionIds) {
        return SubmissionAnswerRequest.builder()
                .assignmentItemId(itemId)
                .selectedOptionIds(selectedOptionIds)
                .build();
    }

    private SubmissionAnswerRequest essayAnswerRequest(Long itemId, String answerText, List<Long> selectedOptionIds) {
        return SubmissionAnswerRequest.builder()
                .assignmentItemId(itemId)
                .answerText(answerText)
                .selectedOptionIds(selectedOptionIds)
                .build();
    }

    private User user(Long id, Role role, String fullName) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setFullName(fullName);
        return user;
    }
}
