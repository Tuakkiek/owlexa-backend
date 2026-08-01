package com.owlexa.owlexabackend.modules.teacher_review.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingJob;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingJobStatus;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingResult;
import com.owlexa.owlexabackend.modules.ai_grading.repository.AIGradingResultRepository;
import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItem;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipient;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipientStatus;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentStatus;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentTargetType;
import com.owlexa.owlexabackend.modules.assignment.repository.AssignmentRepository;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.StudentAttemptDetailResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.StudentAttemptItemResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.SubmissionAnswerResponse;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAnswer;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttempt;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttemptStatus;
import com.owlexa.owlexabackend.modules.student_submission.mapper.SubmissionMapper;
import com.owlexa.owlexabackend.modules.student_submission.repository.SubmissionAttemptRepository;
import com.owlexa.owlexabackend.modules.teacher_review.dto.request.TeacherReviewItemRequest;
import com.owlexa.owlexabackend.modules.teacher_review.dto.request.TeacherReviewUpdateRequest;
import com.owlexa.owlexabackend.modules.teacher_review.dto.response.StudentReviewResultResponse;
import com.owlexa.owlexabackend.modules.teacher_review.dto.response.TeacherReviewDetailResponse;
import com.owlexa.owlexabackend.modules.teacher_review.dto.response.TeacherReviewSummaryResponse;
import com.owlexa.owlexabackend.modules.teacher_review.entity.TeacherReview;
import com.owlexa.owlexabackend.modules.teacher_review.entity.TeacherReviewItem;
import com.owlexa.owlexabackend.modules.teacher_review.entity.TeacherReviewStatus;
import com.owlexa.owlexabackend.modules.teacher_review.mapper.TeacherReviewMapper;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.owlexa.owlexabackend.support.RichTextTestFixtures.serializedDocument;

@ExtendWith(MockitoExtension.class)
class TeacherReviewServiceTest {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    @Mock private TeacherReviewRepository teacherReviewRepository;
    @Mock private SubmissionAttemptRepository submissionAttemptRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AIGradingResultRepository aiGradingResultRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private MembershipRepository membershipRepository;
    @Mock private SubmissionMapper submissionMapper;

    private TeacherReviewService service;

    private static final Long CENTER_ID = 10L;
    private static final Long TEACHER_ID = 20L;
    private static final Long STUDENT_ID = 30L;
    private static final Long ASSIGNMENT_ID = 40L;
    private static final Long RECIPIENT_ID = 50L;
    private static final Long ATTEMPT_ID = 60L;
    private static final Long REVIEW_ID = 70L;
    private static final Long MC_ITEM_ID = 80L;
    private static final Long ESSAY_ITEM_ID = 81L;
    private static final Long SECOND_ESSAY_ITEM_ID = 82L;
    private static final Long ESSAY_ANSWER_ID = 90L;
    private static final Long AI_RESULT_ID = 100L;

    private Center center;
    private User teacher;
    private User student;

    @BeforeEach
    void setUp() {
        service = new TeacherReviewService(
                teacherReviewRepository,
                submissionAttemptRepository,
                assignmentRepository,
                aiGradingResultRepository,
                authorizationService,
                membershipRepository,
                new TeacherReviewMapper(),
                submissionMapper
        );

        TenantContext.setCurrentTenantId(CENTER_ID);
        center = new Center();
        center.setId(CENTER_ID);
        teacher = user(TEACHER_ID, Role.TEACHER, "Teacher One");
        student = user(STUDENT_ID, Role.STUDENT, "Student One");

        lenient().when(authorizationService.getCurrentUser()).thenReturn(teacher);
        lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID))
                .thenReturn(true);
        lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(STUDENT_ID, CENTER_ID))
                .thenReturn(true);
        lenient().when(teacherReviewRepository.saveAndFlush(any(TeacherReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("create: builds one review item for every essay and supports unanswered essays")
    void createOrGet_whenReviewDoesNotExist_shouldCreateDraftFromSubmissionSnapshot() {
        SubmissionAttempt attempt = submittedAttempt(true);
        when(teacherReviewRepository.findDetailBySubmissionAttemptIdAndCenterId(ATTEMPT_ID, CENTER_ID))
                .thenReturn(Optional.empty());
        whenTeacherAttemptFound(attempt);

        TeacherReviewDetailResponse response = service.createOrGetReview(ATTEMPT_ID);

        assertThat(response.getStatus()).isEqualTo(TeacherReviewStatus.IN_PROGRESS);
        assertThat(response.getMaxScore()).isEqualByComparingTo("12.00");
        assertThat(response.getFinalScore()).isNull();
        assertThat(response.getSelectedAiGradingResultId()).isNull();
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems())
                .extracting(item -> item.getAssignmentItemId())
                .containsExactly(ESSAY_ITEM_ID, SECOND_ESSAY_ITEM_ID);
        assertThat(response.getItems().get(0).getSubmissionAnswerId()).isEqualTo(ESSAY_ANSWER_ID);
        assertThat(response.getItems().get(1).getSubmissionAnswerId()).isNull();

        ArgumentCaptor<TeacherReview> captor = ArgumentCaptor.forClass(TeacherReview.class);
        verify(teacherReviewRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isSameAs(teacher);
        assertThat(captor.getValue().getUpdatedBy()).isSameAs(teacher);
    }

    @Test
    @DisplayName("create: POST is idempotent when a review already exists")
    void createOrGet_whenReviewExists_shouldReturnExistingWithoutCreatingAnotherReview() {
        TeacherReview existing = review(TeacherReviewStatus.IN_PROGRESS);
        when(teacherReviewRepository.findDetailBySubmissionAttemptIdAndCenterId(ATTEMPT_ID, CENTER_ID))
                .thenReturn(Optional.of(existing));

        TeacherReviewDetailResponse response = service.createOrGetReview(ATTEMPT_ID);

        assertThat(response.getId()).isEqualTo(REVIEW_ID);
        verify(submissionAttemptRepository, never())
                .findByIdAndAssignmentRecipient_Assignment_Center_IdAndAssignmentRecipient_Assignment_DeletedAtIsNull(
                        any(), any()
                );
        verify(teacherReviewRepository, never()).saveAndFlush(any(TeacherReview.class));
    }

    @Test
    @DisplayName("create: concurrent duplicate insert falls back to the already-created review")
    void createOrGet_whenConcurrentInsertCreatesDuplicate_shouldReturnExistingReview() {
        SubmissionAttempt attempt = submittedAttempt(true);
        TeacherReview existing = review(TeacherReviewStatus.IN_PROGRESS);
        when(teacherReviewRepository.findDetailBySubmissionAttemptIdAndCenterId(ATTEMPT_ID, CENTER_ID))
                .thenReturn(Optional.empty(), Optional.of(existing));
        whenTeacherAttemptFound(attempt);
        when(teacherReviewRepository.saveAndFlush(any(TeacherReview.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate entry '60' for key 'teacher_reviews.uk_teacher_reviews_submission_attempt_id'"
                ));

        TeacherReviewDetailResponse response = service.createOrGetReview(ATTEMPT_ID);

        assertThat(response.getId()).isEqualTo(REVIEW_ID);
        verify(teacherReviewRepository).saveAndFlush(any(TeacherReview.class));
    }

    @Test
    @DisplayName("create: only submitted and auto-submitted attempts can be reviewed")
    void createOrGet_whenAttemptIsInProgress_shouldReject() {
        SubmissionAttempt attempt = submittedAttempt(true);
        attempt.setStatus(SubmissionAttemptStatus.IN_PROGRESS);
        when(teacherReviewRepository.findDetailBySubmissionAttemptIdAndCenterId(ATTEMPT_ID, CENTER_ID))
                .thenReturn(Optional.empty());
        whenTeacherAttemptFound(attempt);

        assertThatThrownBy(() -> service.createOrGetReview(ATTEMPT_ID))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("update: replaces AI selection, comments and every item value")
    void updateReview_whenRequestIsValid_shouldReplaceAllEditableFields() {
        TeacherReview review = review(TeacherReviewStatus.IN_PROGRESS);
        AIGradingResult aiResult = aiResult(review.getSubmissionAttempt(), AI_RESULT_ID);
        whenTeacherReviewFound(review);
        when(aiGradingResultRepository.findSelectableResult(
                AI_RESULT_ID,
                ATTEMPT_ID,
                CENTER_ID,
                AIGradingJobStatus.COMPLETED
        )).thenReturn(Optional.of(aiResult));

        TeacherReviewDetailResponse response = service.updateReview(
                REVIEW_ID,
                updateRequest(
                        0L,
                        AI_RESULT_ID,
                        " Overall feedback ",
                        List.of(
                                itemRequest(ESSAY_ITEM_ID, "3.00", " First comment "),
                                itemRequest(SECOND_ESSAY_ITEM_ID, "5.00", "Second comment")
                        )
                )
        );

        assertThat(response.getSelectedAiGradingResultId()).isEqualTo(AI_RESULT_ID);
        assertThat(response.getOverallComment()).isEqualTo("Overall feedback");
        assertThat(response.getItems())
                .extracting(item -> item.getFinalScore())
                .containsExactly(new BigDecimal("3.00"), new BigDecimal("5.00"));
        assertThat(review.getItems().get(0).getItemComment()).isEqualTo("First comment");
        assertThat(review.getUpdatedBy()).isSameAs(teacher);
    }

    @Test
    @DisplayName("update: nullable AI selection clears the current reference")
    void updateReview_whenAiSelectionIsNull_shouldClearIt() {
        TeacherReview review = reviewWithScores();
        review.setSelectedAiGradingResult(aiResult(review.getSubmissionAttempt(), AI_RESULT_ID));
        whenTeacherReviewFound(review);

        service.updateReview(
                REVIEW_ID,
                updateRequest(
                        0L,
                        null,
                        null,
                        List.of(
                                itemRequest(ESSAY_ITEM_ID, "3.00", null),
                                itemRequest(SECOND_ESSAY_ITEM_ID, "5.00", null)
                        )
                )
        );

        assertThat(review.getSelectedAiGradingResult()).isNull();
    }

    @Test
    @DisplayName("update: stale request version is rejected before persistence")
    void updateReview_whenVersionIsStale_shouldReject() {
        TeacherReview review = review(TeacherReviewStatus.IN_PROGRESS);
        review.setVersion(2L);
        whenTeacherReviewFound(review);

        assertThatThrownBy(() -> service.updateReview(
                REVIEW_ID,
                updateRequest(1L, null, null, validItemRequests())
        )).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("đã bị thay đổi");

        verify(teacherReviewRepository, never()).saveAndFlush(any(TeacherReview.class));
    }

    @Test
    @DisplayName("update: persistence optimistic locking failures become a clear business error")
    void updateReview_whenConcurrentFlushFails_shouldReject() {
        TeacherReview review = review(TeacherReviewStatus.IN_PROGRESS);
        whenTeacherReviewFound(review);
        when(teacherReviewRepository.saveAndFlush(review))
                .thenThrow(new ObjectOptimisticLockingFailureException(TeacherReview.class, REVIEW_ID));

        assertThatThrownBy(() -> service.updateReview(
                REVIEW_ID,
                updateRequest(0L, null, null, validItemRequests())
        )).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("đã bị thay đổi");
    }

    @Test
    @DisplayName("update: finalized and released reviews are immutable")
    void updateReview_whenReviewIsFinalized_shouldReject() {
        TeacherReview review = review(TeacherReviewStatus.FINALIZED);
        whenTeacherReviewFound(review);

        assertThatThrownBy(() -> service.updateReview(
                REVIEW_ID,
                updateRequest(0L, null, null, validItemRequests())
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("update: selected AI result must be completed and belong to the attempt")
    void updateReview_whenAiResultIsInvalid_shouldReject() {
        TeacherReview review = review(TeacherReviewStatus.IN_PROGRESS);
        whenTeacherReviewFound(review);
        when(aiGradingResultRepository.findSelectableResult(
                AI_RESULT_ID,
                ATTEMPT_ID,
                CENTER_ID,
                AIGradingJobStatus.COMPLETED
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateReview(
                REVIEW_ID,
                updateRequest(0L, AI_RESULT_ID, null, validItemRequests())
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("update: PUT requires every review item exactly once")
    void updateReview_whenAnItemIsMissing_shouldReject() {
        TeacherReview review = review(TeacherReviewStatus.IN_PROGRESS);
        whenTeacherReviewFound(review);

        assertThatThrownBy(() -> service.updateReview(
                REVIEW_ID,
                updateRequest(
                        0L,
                        null,
                        null,
                        List.of(itemRequest(ESSAY_ITEM_ID, "3.00", null))
                )
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("update: item score must remain within its snapshot bounds")
    void updateReview_whenItemScoreExceedsMax_shouldReject() {
        TeacherReview review = review(TeacherReviewStatus.IN_PROGRESS);
        whenTeacherReviewFound(review);

        assertThatThrownBy(() -> service.updateReview(
                REVIEW_ID,
                updateRequest(
                        0L,
                        null,
                        null,
                        List.of(
                                itemRequest(ESSAY_ITEM_ID, "4.01", null),
                                itemRequest(SECOND_ESSAY_ITEM_ID, "5.00", null)
                        )
                )
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("finalize: calculates final score and refreshes max score from the attempt")
    void finalizeReview_whenDraftIsComplete_shouldFinalizeAndCalculateScore() {
        TeacherReview review = reviewWithScores();
        review.setMaxScore(new BigDecimal("99.00"));
        whenTeacherReviewFound(review);

        TeacherReviewDetailResponse response = service.finalizeReview(REVIEW_ID);

        assertThat(response.getStatus()).isEqualTo(TeacherReviewStatus.FINALIZED);
        assertThat(response.getFinalScore()).isEqualByComparingTo("10.00");
        assertThat(response.getMaxScore()).isEqualByComparingTo("12.00");
        assertThat(review.getFinalizedBy()).isSameAs(teacher);
        assertThat(review.getFinalizedAt()).isNotNull();
    }

    @Test
    @DisplayName("finalize: every essay item must have a score")
    void finalizeReview_whenAnEssayScoreIsMissing_shouldReject() {
        TeacherReview review = reviewWithScores();
        review.getItems().get(1).setFinalScore(null);
        whenTeacherReviewFound(review);

        assertThatThrownBy(() -> service.finalizeReview(REVIEW_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tất cả câu tự luận");
    }

    @Test
    @DisplayName("finalize: review items must cover every essay in the assignment snapshot")
    void finalizeReview_whenEssayItemCoverageIsIncomplete_shouldReject() {
        TeacherReview review = reviewWithScores();
        review.getItems().remove(1);
        whenTeacherReviewFound(review);

        assertThatThrownBy(() -> service.finalizeReview(REVIEW_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("đầy đủ tất cả câu hỏi tự luận");
    }

    @Test
    @DisplayName("finalize: pure multiple-choice attempts use the submission auto score")
    void finalizeReview_whenAssignmentHasNoEssay_shouldFinalizeWithAutoScore() {
        SubmissionAttempt attempt = submittedAttempt(false);
        TeacherReview review = reviewForAttempt(attempt, TeacherReviewStatus.IN_PROGRESS);
        whenTeacherReviewFound(review);

        TeacherReviewDetailResponse response = service.finalizeReview(REVIEW_ID);

        assertThat(response.getFinalScore()).isEqualByComparingTo("2.00");
        assertThat(response.getMaxScore()).isEqualByComparingTo("2.00");
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    @DisplayName("finalize: selected AI result remains fixed and is revalidated")
    void finalizeReview_whenAiResultWasSelected_shouldKeepExactSelectedResult() {
        TeacherReview review = reviewWithScores();
        AIGradingResult selectedResult = aiResult(review.getSubmissionAttempt(), AI_RESULT_ID);
        review.setSelectedAiGradingResult(selectedResult);
        whenTeacherReviewFound(review);
        when(aiGradingResultRepository.findSelectableResult(
                AI_RESULT_ID,
                ATTEMPT_ID,
                CENTER_ID,
                AIGradingJobStatus.COMPLETED
        )).thenReturn(Optional.of(selectedResult));

        TeacherReviewDetailResponse response = service.finalizeReview(REVIEW_ID);

        assertThat(response.getSelectedAiGradingResultId()).isEqualTo(AI_RESULT_ID);
    }

    @Test
    @DisplayName("release: only finalized reviews can become released")
    void releaseReview_whenFinalized_shouldReleaseAndRecordAudit() {
        TeacherReview review = reviewWithScores();
        review.setStatus(TeacherReviewStatus.FINALIZED);
        review.setFinalScore(new BigDecimal("10.00"));
        whenTeacherReviewFound(review);

        TeacherReviewDetailResponse response = service.releaseReview(REVIEW_ID);

        assertThat(response.getStatus()).isEqualTo(TeacherReviewStatus.RELEASED);
        assertThat(review.getReleasedBy()).isSameAs(teacher);
        assertThat(review.getReleasedAt()).isNotNull();
    }

    @Test
    @DisplayName("release: a released review cannot be released twice")
    void releaseReview_whenAlreadyReleased_shouldReject() {
        TeacherReview review = reviewWithScores();
        review.setStatus(TeacherReviewStatus.RELEASED);
        review.setFinalScore(new BigDecimal("10.00"));
        whenTeacherReviewFound(review);

        assertThatThrownBy(() -> service.releaseReview(REVIEW_ID))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("queue: UNREVIEWED filter pages attempts and uses batch review and AI lookup")
    void findReviewQueue_whenUnreviewedFilterUsed_shouldReturnUnreviewedAttemptsInBatch() {
        Assignment assignment = assignment(true);
        SubmissionAttempt first = submittedAttempt(assignment, ATTEMPT_ID, RECIPIENT_ID, student);
        User secondStudent = user(STUDENT_ID + 1, Role.STUDENT, "Student Two");
        SubmissionAttempt second = submittedAttempt(
                assignment,
                ATTEMPT_ID + 1,
                RECIPIENT_ID + 1,
                secondStudent
        );
        PageRequest pageable = PageRequest.of(0, 20);
        when(assignmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSIGNMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(assignment));
        when(teacherReviewRepository.findUnreviewedQueueAttempts(
                eq(ASSIGNMENT_ID),
                eq(CENTER_ID),
                anyCollection(),
                eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(first, second), pageable, 2));
        when(teacherReviewRepository.findAllBySubmissionAttempt_IdIn(anyCollection())).thenReturn(List.of());
        when(aiGradingResultRepository.findAttemptIdsWithResult(
                anyCollection(),
                eq(CENTER_ID),
                eq(AIGradingJobStatus.COMPLETED)
        )).thenReturn(Set.of());

        Page<TeacherReviewSummaryResponse> response = service.findReviewQueue(
                ASSIGNMENT_ID,
                "UNREVIEWED",
                pageable
        );

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent()).allSatisfy(summary -> {
            assertThat(summary.getReviewId()).isNull();
            assertThat(summary.isHasEssay()).isTrue();
            assertThat(summary.isHasAiResult()).isFalse();
        });
        verify(teacherReviewRepository, times(1)).findAllBySubmissionAttempt_IdIn(anyCollection());
        verify(aiGradingResultRepository, times(1)).findAttemptIdsWithResult(
                anyCollection(),
                eq(CENTER_ID),
                eq(AIGradingJobStatus.COMPLETED)
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> attemptIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(teacherReviewRepository).findAllBySubmissionAttempt_IdIn(attemptIdsCaptor.capture());
        assertThat(attemptIdsCaptor.getValue()).containsExactly(ATTEMPT_ID, ATTEMPT_ID + 1);
    }

    @Test
    @DisplayName("queue: persisted review status filter returns review and AI metadata")
    void findReviewQueue_whenFinalizedFilterUsed_shouldReturnMatchingSummary() {
        TeacherReview review = reviewWithScores();
        review.setStatus(TeacherReviewStatus.FINALIZED);
        review.setSelectedAiGradingResult(aiResult(review.getSubmissionAttempt(), AI_RESULT_ID));
        Assignment assignment = review.getSubmissionAttempt().getAssignmentRecipient().getAssignment();
        PageRequest pageable = PageRequest.of(0, 20);
        when(assignmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSIGNMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(assignment));
        when(teacherReviewRepository.findQueueAttemptsByReviewStatus(
                eq(ASSIGNMENT_ID),
                eq(CENTER_ID),
                anyCollection(),
                eq(TeacherReviewStatus.FINALIZED),
                eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(review.getSubmissionAttempt()), pageable, 1));
        when(teacherReviewRepository.findAllBySubmissionAttempt_IdIn(List.of(ATTEMPT_ID)))
                .thenReturn(List.of(review));
        when(aiGradingResultRepository.findAttemptIdsWithResult(
                List.of(ATTEMPT_ID),
                CENTER_ID,
                AIGradingJobStatus.COMPLETED
        )).thenReturn(Set.of(ATTEMPT_ID));

        Page<TeacherReviewSummaryResponse> response = service.findReviewQueue(
                ASSIGNMENT_ID,
                "finalized",
                pageable
        );

        TeacherReviewSummaryResponse summary = response.getContent().get(0);
        assertThat(summary.getReviewId()).isEqualTo(REVIEW_ID);
        assertThat(summary.getReviewStatus()).isEqualTo(TeacherReviewStatus.FINALIZED);
        assertThat(summary.getSelectedAiGradingResultId()).isEqualTo(AI_RESULT_ID);
        assertThat(summary.isHasAiResult()).isTrue();
    }

    @Test
    @DisplayName("queue: invalid virtual or persisted status filter is rejected")
    void findReviewQueue_whenStatusFilterIsInvalid_shouldReject() {
        Assignment assignment = assignment(true);
        when(assignmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSIGNMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> service.findReviewQueue(
                ASSIGNMENT_ID,
                "UNKNOWN",
                PageRequest.of(0, 20)
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("student result: owner can read only a released review")
    void getStudentReleasedResult_whenReleasedReviewBelongsToStudent_shouldReturnPublicResult() {
        when(authorizationService.getCurrentUser()).thenReturn(student);
        TeacherReview review = reviewWithScores();
        review.setStatus(TeacherReviewStatus.RELEASED);
        review.setFinalScore(new BigDecimal("10.00"));
        review.setOverallComment("Released feedback");
        review.setReleasedAt(Instant.now());
        review.setSelectedAiGradingResult(aiResult(review.getSubmissionAttempt(), AI_RESULT_ID));
        when(teacherReviewRepository.findReleasedDetailForStudent(
                ATTEMPT_ID,
                STUDENT_ID,
                CENTER_ID,
                TeacherReviewStatus.RELEASED
        )).thenReturn(Optional.of(review));
        when(submissionMapper.toStudentAttemptDetailResponse(review.getSubmissionAttempt()))
                .thenReturn(StudentAttemptDetailResponse.builder()

                        .items(List.of(StudentAttemptItemResponse.builder()
                                .assignmentItemId(ESSAY_ITEM_ID)
                                .build()))
                        .answers(List.of(SubmissionAnswerResponse.builder()
                                .assignmentItemId(ESSAY_ITEM_ID)
                                .build()))
                        .build());

        StudentReviewResultResponse response = service.getStudentReleasedResult(ATTEMPT_ID);

        assertThat(response.getSubmissionAttemptId()).isEqualTo(ATTEMPT_ID);
        assertThat(response.getFinalScore()).isEqualByComparingTo("10.00");
        assertThat(response.getOverallComment()).isEqualTo("Released feedback");

        assertThat(response.getItems()).extracting(StudentAttemptItemResponse::getAssignmentItemId)
                .containsExactly(ESSAY_ITEM_ID);
        assertThat(response.getAnswers()).extracting(SubmissionAnswerResponse::getAssignmentItemId)
                .containsExactly(ESSAY_ITEM_ID);
        assertThat(response.getEssayItems()).hasSize(2);
        verify(teacherReviewRepository).findReleasedDetailForStudent(
                ATTEMPT_ID,
                STUDENT_ID,
                CENTER_ID,
                TeacherReviewStatus.RELEASED
        );
    }

    @Test
    @DisplayName("student result: draft and finalized reviews remain invisible")
    void getStudentReleasedResult_whenNoReleasedReviewExists_shouldReturnNotFound() {
        when(authorizationService.getCurrentUser()).thenReturn(student);
        when(teacherReviewRepository.findReleasedDetailForStudent(
                ATTEMPT_ID,
                STUDENT_ID,
                CENTER_ID,
                TeacherReviewStatus.RELEASED
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStudentReleasedResult(ATTEMPT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("authorization: non-teacher cannot manage reviews")
    void createOrGet_whenCurrentUserIsStudent_shouldReject() {
        when(authorizationService.getCurrentUser()).thenReturn(student);

        assertThatThrownBy(() -> service.createOrGetReview(ATTEMPT_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("authorization: teacher without center membership is rejected")
    void createOrGet_whenTeacherIsNotCenterMember_shouldReject() {
        when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.createOrGetReview(ATTEMPT_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("tenant: missing tenant context is rejected")
    void createOrGet_whenTenantContextIsMissing_shouldReject() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.createOrGetReview(ATTEMPT_ID))
                .isInstanceOf(BadRequestException.class);
    }

    private void whenTeacherAttemptFound(SubmissionAttempt attempt) {
        when(submissionAttemptRepository
                .findByIdAndAssignmentRecipient_Assignment_Center_IdAndAssignmentRecipient_Assignment_DeletedAtIsNull(
                        ATTEMPT_ID,
                        CENTER_ID
                ))
                .thenReturn(Optional.of(attempt));
    }

    private void whenTeacherReviewFound(TeacherReview review) {
        when(teacherReviewRepository.findDetailByIdAndCenterId(REVIEW_ID, CENTER_ID))
                .thenReturn(Optional.of(review));
    }

    private Assignment assignment(boolean includeEssays) {
        Assignment assignment = Assignment.builder()
                .center(center)
                .status(AssignmentStatus.CLOSED)
                .title("Final Exam")
                .items(new ArrayList<>())
                .build();
        assignment.setId(ASSIGNMENT_ID);
        assignment.getItems().add(item(assignment, MC_ITEM_ID, QuestionType.MULTIPLE_CHOICE, "MC", "2.00", 1));
        if (includeEssays) {
            assignment.getItems().add(item(
                    assignment,
                    ESSAY_ITEM_ID,
                    QuestionType.ESSAY,
                    "Essay One",
                    "4.00",
                    2
            ));
            assignment.getItems().add(item(
                    assignment,
                    SECOND_ESSAY_ITEM_ID,
                    QuestionType.ESSAY,
                    "Essay Two",
                    "6.00",
                    3
            ));
        }
        return assignment;
    }

    private AssignmentItem item(
            Assignment assignment,
            Long id,
            QuestionType questionType,
            String title,
            String points,
            int displayOrder
    ) {
        AssignmentItem item = AssignmentItem.builder()
                .assignment(assignment)
                .questionType(questionType)
                .title(title)
                .contentJson(serializedDocument(title + " content"))
                .points(new BigDecimal(points))
                .displayOrder(displayOrder)
                .build();
        item.setId(id);
        return item;
    }

    private SubmissionAttempt submittedAttempt(boolean includeEssays) {
        return submittedAttempt(assignment(includeEssays), ATTEMPT_ID, RECIPIENT_ID, student);
    }

    private SubmissionAttempt submittedAttempt(
            Assignment assignment,
            Long attemptId,
            Long recipientId,
            User recipientStudent
    ) {
        AssignmentRecipient recipient = AssignmentRecipient.builder()
                .assignment(assignment)
                .studentUser(recipientStudent)
                .sourceType(AssignmentTargetType.STUDENT)
                .status(AssignmentRecipientStatus.ASSIGNED)
                .assignedAt(Instant.now())
                .build();
        recipient.setId(recipientId);

        SubmissionAttempt attempt = SubmissionAttempt.builder()
                .assignmentRecipient(recipient)
                .status(SubmissionAttemptStatus.SUBMITTED)
                .attemptNumber(1)
                .assignmentTitleSnapshot(assignment.getTitle())
                .startedAt(Instant.now().minusSeconds(600))
                .submittedAt(Instant.now())
                .autoScore(new BigDecimal("2.00"))
                .maxScore(assignment.getItems().stream()
                        .map(AssignmentItem::getPoints)
                        .reduce(ZERO, BigDecimal::add))
                .answers(new ArrayList<>())
                .build();
        attempt.setId(attemptId);

        assignment.getItems().stream()
                .filter(item -> item.getId().equals(ESSAY_ITEM_ID))
                .findFirst()
                .ifPresent(essayItem -> {
                    SubmissionAnswer answer = SubmissionAnswer.builder()
                            .attempt(attempt)
                            .assignmentItem(essayItem)
                            .answerText("Student essay")
                            .build();
                    answer.setId(ESSAY_ANSWER_ID);
                    attempt.getAnswers().add(answer);
                });
        return attempt;
    }

    private TeacherReview review(TeacherReviewStatus status) {
        return reviewForAttempt(submittedAttempt(true), status);
    }

    private TeacherReview reviewWithScores() {
        TeacherReview review = review(TeacherReviewStatus.IN_PROGRESS);
        review.getItems().get(0).setFinalScore(new BigDecimal("3.00"));
        review.getItems().get(1).setFinalScore(new BigDecimal("5.00"));
        return review;
    }

    private TeacherReview reviewForAttempt(SubmissionAttempt attempt, TeacherReviewStatus status) {
        TeacherReview review = TeacherReview.builder()
                .submissionAttempt(attempt)
                .status(status)
                .maxScore(attempt.getMaxScore())
                .createdBy(teacher)
                .updatedBy(teacher)
                .version(0L)
                .items(new ArrayList<>())
                .build();
        review.setId(REVIEW_ID);

        for (AssignmentItem assignmentItem : attempt.getAssignmentRecipient().getAssignment().getItems()) {
            if (assignmentItem.getQuestionType() != QuestionType.ESSAY) {
                continue;
            }
            SubmissionAnswer answer = attempt.getAnswers().stream()
                    .filter(candidate -> candidate.getAssignmentItem().getId().equals(assignmentItem.getId()))
                    .findFirst()
                    .orElse(null);
            TeacherReviewItem reviewItem = TeacherReviewItem.builder()
                    .review(review)
                    .assignmentItem(assignmentItem)
                    .submissionAnswer(answer)
                    .questionTitleSnapshot(assignmentItem.getTitle())
                    .displayOrderSnapshot(assignmentItem.getDisplayOrder())
                    .maxScore(assignmentItem.getPoints())
                    .build();
            review.getItems().add(reviewItem);
        }
        return review;
    }

    private AIGradingResult aiResult(SubmissionAttempt attempt, Long resultId) {
        AIGradingJob job = AIGradingJob.builder()
                .submissionAttempt(attempt)
                .status(AIGradingJobStatus.COMPLETED)
                .requestedBy(teacher)
                .build();
        AIGradingResult result = AIGradingResult.builder()
                .job(job)
                .submissionAttempt(attempt)
                .aiScore(new BigDecimal("8.00"))
                .maxScore(attempt.getMaxScore())
                .build();
        result.setId(resultId);
        return result;
    }

    private TeacherReviewUpdateRequest updateRequest(
            Long version,
            Long selectedAiResultId,
            String overallComment,
            List<TeacherReviewItemRequest> items
    ) {
        return TeacherReviewUpdateRequest.builder()
                .version(version)
                .selectedAiGradingResultId(selectedAiResultId)
                .overallComment(overallComment)
                .items(items)
                .build();
    }

    private List<TeacherReviewItemRequest> validItemRequests() {
        return List.of(
                itemRequest(ESSAY_ITEM_ID, "3.00", null),
                itemRequest(SECOND_ESSAY_ITEM_ID, "5.00", null)
        );
    }

    private TeacherReviewItemRequest itemRequest(
            Long assignmentItemId,
            String finalScore,
            String comment
    ) {
        return TeacherReviewItemRequest.builder()
                .assignmentItemId(assignmentItemId)
                .finalScore(finalScore == null ? null : new BigDecimal(finalScore))
                .itemComment(comment)
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
