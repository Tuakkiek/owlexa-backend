package com.owlexa.owlexabackend.modules.teacher_review.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingJobStatus;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingResult;
import com.owlexa.owlexabackend.modules.ai_grading.repository.AIGradingResultRepository;
import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItem;
import com.owlexa.owlexabackend.modules.assignment.repository.AssignmentRepository;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAnswer;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttempt;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttemptStatus;
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
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherReviewService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final String UNREVIEWED_FILTER = "UNREVIEWED";
    private static final Set<SubmissionAttemptStatus> REVIEWABLE_SUBMISSION_STATUSES =
            EnumSet.of(SubmissionAttemptStatus.SUBMITTED, SubmissionAttemptStatus.AUTO_SUBMITTED);

    private final TeacherReviewRepository teacherReviewRepository;
    private final SubmissionAttemptRepository submissionAttemptRepository;
    private final AssignmentRepository assignmentRepository;
    private final AIGradingResultRepository aiGradingResultRepository;
    private final AuthorizationService authorizationService;
    private final MembershipRepository membershipRepository;
    private final TeacherReviewMapper teacherReviewMapper;

    @Transactional
    public TeacherReviewDetailResponse createOrGetReview(Long submissionAttemptId) {
        User teacher = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        TeacherReview existingReview = teacherReviewRepository
                .findDetailBySubmissionAttemptIdAndCenterId(submissionAttemptId, centerId)
                .orElse(null);
        if (existingReview != null) {
            return teacherReviewMapper.toDetailResponse(existingReview);
        }

        SubmissionAttempt attempt = findTeacherAttempt(submissionAttemptId, centerId);
        requireSubmittedAttempt(attempt);

        TeacherReview review = createReview(attempt, teacher);
        return teacherReviewMapper.toDetailResponse(teacherReviewRepository.saveAndFlush(review));
    }

    @Transactional(readOnly = true)
    public TeacherReviewDetailResponse getTeacherReview(Long submissionAttemptId) {
        requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        TeacherReview review = teacherReviewRepository
                .findDetailBySubmissionAttemptIdAndCenterId(submissionAttemptId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher review not found for submission attempt: " + submissionAttemptId
                ));
        return teacherReviewMapper.toDetailResponse(review);
    }

    @Transactional
    public TeacherReviewDetailResponse updateReview(Long reviewId, TeacherReviewUpdateRequest request) {
        User teacher = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        TeacherReview review = findTeacherReview(reviewId, centerId);

        requireMutable(review);
        validateUpdateRequest(review, request);
        review.setSelectedAiGradingResult(resolveSelectedAiResult(
                request.getSelectedAiGradingResultId(),
                review.getSubmissionAttempt().getId(),
                centerId
        ));
        review.setOverallComment(normalizeOptionalText(request.getOverallComment()));
        applyItemUpdates(review, request.getItems());
        review.setUpdatedBy(teacher);

        return saveReview(review);
    }

    @Transactional
    public TeacherReviewDetailResponse finalizeReview(Long reviewId) {
        User teacher = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        TeacherReview review = findTeacherReview(reviewId, centerId);

        requireMutable(review);
        validateSelectedAiResult(review, centerId);
        validateEssayCoverage(review);
        BigDecimal maxScore = review.getSubmissionAttempt().getMaxScore();
        if (maxScore == null) {
            throw new BadRequestException("Thiếu điểm tối đa của bài nộp");
        }
        BigDecimal finalScore = calculateFinalScore(review);
        if (finalScore.compareTo(maxScore) > 0) {
            throw new BadRequestException("Điểm số cuối cùng không được vượt quá điểm tối đa");
        }

        Instant now = Instant.now();
        review.setMaxScore(maxScore);
        review.setFinalScore(finalScore);
        review.setStatus(TeacherReviewStatus.FINALIZED);
        review.setFinalizedBy(teacher);
        review.setFinalizedAt(now);
        review.setUpdatedBy(teacher);

        return saveReview(review);
    }

    @Transactional
    public TeacherReviewDetailResponse releaseReview(Long reviewId) {
        User teacher = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        TeacherReview review = findTeacherReview(reviewId, centerId);

        if (review.getStatus() != TeacherReviewStatus.FINALIZED) {
            throw new BadRequestException("Chỉ có bài đánh giá đã hoàn tất mới có thể công bố");
        }
        if (review.getFinalScore() == null) {
            throw new BadRequestException("Thiếu điểm của bài đánh giá đã hoàn tất");
        }

        Instant now = Instant.now();
        review.setStatus(TeacherReviewStatus.RELEASED);
        review.setReleasedBy(teacher);
        review.setReleasedAt(now);
        review.setUpdatedBy(teacher);

        return saveReview(review);
    }

    @Transactional(readOnly = true)
    public Page<TeacherReviewSummaryResponse> findReviewQueue(
            Long assignmentId,
            String reviewStatus,
            Pageable pageable
    ) {
        requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        Assignment assignment = assignmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(assignmentId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài tập với ID: " + assignmentId));

        Page<SubmissionAttempt> attempts = findQueueAttempts(
                assignmentId,
                centerId,
                reviewStatus,
                pageable
        );
        if (attempts.isEmpty()) {
            return attempts.map(attempt -> teacherReviewMapper.toSummaryResponse(
                    attempt,
                    null,
                    hasEssay(assignment),
                    false
            ));
        }

        List<Long> attemptIds = attempts.stream().map(SubmissionAttempt::getId).toList();
        Map<Long, TeacherReview> reviewsByAttemptId = teacherReviewRepository
                .findAllBySubmissionAttempt_IdIn(attemptIds)
                .stream()
                .collect(Collectors.toMap(
                        review -> review.getSubmissionAttempt().getId(),
                        Function.identity()
                ));
        Set<Long> attemptsWithAiResult = aiGradingResultRepository.findAttemptIdsWithResult(
                attemptIds,
                centerId,
                AIGradingJobStatus.COMPLETED
        );
        boolean assignmentHasEssay = hasEssay(assignment);

        return attempts.map(attempt -> teacherReviewMapper.toSummaryResponse(
                attempt,
                reviewsByAttemptId.get(attempt.getId()),
                assignmentHasEssay,
                attemptsWithAiResult.contains(attempt.getId())
        ));
    }

    @Transactional(readOnly = true)
    public StudentReviewResultResponse getStudentReleasedResult(Long submissionAttemptId) {
        User student = requireStudentInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        TeacherReview review = teacherReviewRepository.findReleasedDetailForStudent(
                        submissionAttemptId,
                        student.getId(),
                        centerId,
                        TeacherReviewStatus.RELEASED
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy kết quả đánh giá đã công bố cho bài nộp: " + submissionAttemptId
                ));
        return teacherReviewMapper.toStudentResultResponse(review);
    }

    private TeacherReview createReview(SubmissionAttempt attempt, User teacher) {
        Assignment assignment = attempt.getAssignmentRecipient().getAssignment();
        if (attempt.getMaxScore() == null) {
            throw new BadRequestException("Thiếu điểm tối đa của bài nộp");
        }

        Map<Long, SubmissionAnswer> answersByItemId = attempt.getAnswers().stream()
                .collect(Collectors.toMap(answer -> answer.getAssignmentItem().getId(), Function.identity()));
        TeacherReview review = TeacherReview.builder()
                .submissionAttempt(attempt)
                .status(TeacherReviewStatus.IN_PROGRESS)
                .maxScore(attempt.getMaxScore())
                .createdBy(teacher)
                .updatedBy(teacher)
                .build();

        assignment.getItems().stream()
                .filter(item -> item.getQuestionType() == QuestionType.ESSAY)
                .sorted(java.util.Comparator.comparing(AssignmentItem::getDisplayOrder))
                .map(item -> createReviewItem(review, item, answersByItemId.get(item.getId())))
                .forEach(review.getItems()::add);
        return review;
    }

    private TeacherReviewItem createReviewItem(
            TeacherReview review,
            AssignmentItem assignmentItem,
            SubmissionAnswer answer
    ) {
        return TeacherReviewItem.builder()
                .review(review)
                .assignmentItem(assignmentItem)
                .submissionAnswer(answer)
                .questionTitleSnapshot(assignmentItem.getTitle())
                .displayOrderSnapshot(assignmentItem.getDisplayOrder())
                .maxScore(scoreValue(assignmentItem.getPoints()))
                .build();
    }

    private void validateUpdateRequest(TeacherReview review, TeacherReviewUpdateRequest request) {
        if (request == null || request.getVersion() == null) {
            throw new BadRequestException("Phiên bản đánh giá là bắt buộc");
        }
        if (!Objects.equals(request.getVersion(), review.getVersion())) {
            throw new BadRequestException("Đánh giá của giáo viên đã bị thay đổi; vui lòng tải lại trước khi lưu");
        }
        if (request.getItems() == null) {
            throw new BadRequestException("Danh sách mục đánh giá không được để trống");
        }
    }

    private void applyItemUpdates(TeacherReview review, List<TeacherReviewItemRequest> requests) {
        Map<Long, TeacherReviewItem> itemsByAssignmentItemId = review.getItems().stream()
                .collect(Collectors.toMap(item -> item.getAssignmentItem().getId(), Function.identity()));
        Set<Long> requestItemIds = new HashSet<>();

        for (TeacherReviewItemRequest request : requests) {
            if (request == null || request.getAssignmentItemId() == null) {
                throw new BadRequestException("Mã mục bài tập không được để trống");
            }
            if (!requestItemIds.add(request.getAssignmentItemId())) {
                throw new BadRequestException("Không cho phép mục đánh giá trùng lặp");
            }

            TeacherReviewItem item = itemsByAssignmentItemId.get(request.getAssignmentItemId());
            if (item == null) {
                throw new BadRequestException("Mục đánh giá không thuộc về bài đánh giá này");
            }
            validateItemScore(request.getFinalScore(), item.getMaxScore());
            item.setFinalScore(request.getFinalScore());
            item.setItemComment(normalizeOptionalText(request.getItemComment()));
        }

        if (!requestItemIds.equals(itemsByAssignmentItemId.keySet())) {
            throw new BadRequestException("Cập nhật phải bao gồm đầy đủ từng mục đánh giá đúng 1 lần");
        }
    }

    private AIGradingResult resolveSelectedAiResult(
            Long resultId,
            Long submissionAttemptId,
            Long centerId
    ) {
        if (resultId == null) {
            return null;
        }
        return aiGradingResultRepository.findSelectableResult(
                        resultId,
                        submissionAttemptId,
                        centerId,
                        AIGradingJobStatus.COMPLETED
                )
                .orElseThrow(() -> new BadRequestException(
                        "Kết quả chấm bằng AI được chọn không hợp lệ cho lượt làm bài này"
                ));
    }

    private void validateSelectedAiResult(TeacherReview review, Long centerId) {
        if (review.getSelectedAiGradingResult() == null) {
            return;
        }
        resolveSelectedAiResult(
                review.getSelectedAiGradingResult().getId(),
                review.getSubmissionAttempt().getId(),
                centerId
        );
    }

    private BigDecimal calculateFinalScore(TeacherReview review) {
        BigDecimal finalScore = scoreValue(review.getSubmissionAttempt().getAutoScore());
        for (TeacherReviewItem item : review.getItems()) {
            if (item.getFinalScore() == null) {
                throw new BadRequestException("Tất cả câu tự luận phải được cho điểm trước khi hoàn tất");
            }
            validateItemScore(item.getFinalScore(), item.getMaxScore());
            finalScore = finalScore.add(item.getFinalScore());
        }
        return finalScore;
    }

    private void validateEssayCoverage(TeacherReview review) {
        Set<Long> expectedEssayItemIds = review.getSubmissionAttempt()
                .getAssignmentRecipient()
                .getAssignment()
                .getItems()
                .stream()
                .filter(item -> item.getQuestionType() == QuestionType.ESSAY)
                .map(AssignmentItem::getId)
                .collect(Collectors.toSet());
        Set<Long> reviewItemIds = review.getItems()
                .stream()
                .map(item -> item.getAssignmentItem().getId())
                .collect(Collectors.toSet());

        if (!expectedEssayItemIds.equals(reviewItemIds)) {
            throw new BadRequestException("Đánh giá của giáo viên phải chứa đầy đủ tất cả câu hỏi tự luận");
        }
    }

    private void validateItemScore(BigDecimal finalScore, BigDecimal maxScore) {
        if (finalScore == null) {
            return;
        }
        if (finalScore.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Điểm số của mục đánh giá không được âm");
        }
        if (finalScore.compareTo(maxScore) > 0) {
            throw new BadRequestException("Điểm số của mục đánh giá không được vượt quá điểm tối đa");
        }
    }

    private Page<SubmissionAttempt> findQueueAttempts(
            Long assignmentId,
            Long centerId,
            String reviewStatus,
            Pageable pageable
    ) {
        if (reviewStatus == null || reviewStatus.isBlank()) {
            return teacherReviewRepository.findReviewQueueAttempts(
                    assignmentId,
                    centerId,
                    REVIEWABLE_SUBMISSION_STATUSES,
                    pageable
            );
        }

        String normalizedStatus = reviewStatus.trim().toUpperCase(Locale.ROOT);
        if (UNREVIEWED_FILTER.equals(normalizedStatus)) {
            return teacherReviewRepository.findUnreviewedQueueAttempts(
                    assignmentId,
                    centerId,
                    REVIEWABLE_SUBMISSION_STATUSES,
                    pageable
            );
        }

        TeacherReviewStatus status;
        try {
            status = TeacherReviewStatus.valueOf(normalizedStatus);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Bộ lọc trạng thái đánh giá không hợp lệ: " + reviewStatus);
        }
        return teacherReviewRepository.findQueueAttemptsByReviewStatus(
                assignmentId,
                centerId,
                REVIEWABLE_SUBMISSION_STATUSES,
                status,
                pageable
        );
    }

    private TeacherReview findTeacherReview(Long reviewId, Long centerId) {
        return teacherReviewRepository.findDetailByIdAndCenterId(reviewId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá của giáo viên với ID: " + reviewId));
    }

    private SubmissionAttempt findTeacherAttempt(Long submissionAttemptId, Long centerId) {
        return submissionAttemptRepository
                .findByIdAndAssignmentRecipient_Assignment_Center_IdAndAssignmentRecipient_Assignment_DeletedAtIsNull(
                        submissionAttemptId,
                        centerId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy lượt nộp bài với ID: " + submissionAttemptId
                ));
    }

    private void requireSubmittedAttempt(SubmissionAttempt attempt) {
        if (!REVIEWABLE_SUBMISSION_STATUSES.contains(attempt.getStatus())) {
            throw new BadRequestException("Chỉ có bài nộp ở trạng thái đã nộp mới có thể đánh giá");
        }
    }

    private void requireMutable(TeacherReview review) {
        if (review.getStatus() != TeacherReviewStatus.IN_PROGRESS) {
            throw new BadRequestException("Chỉ có bài đánh giá đang thực hiện mới có thể cập nhật");
        }
    }

    private TeacherReviewDetailResponse saveReview(TeacherReview review) {
        try {
            return teacherReviewMapper.toDetailResponse(teacherReviewRepository.saveAndFlush(review));
        } catch (OptimisticLockingFailureException exception) {
            throw new BadRequestException("Đánh giá của giáo viên đã bị thay đổi; vui lòng tải lại trước khi tiếp tục");
        }
    }

    private boolean hasEssay(Assignment assignment) {
        return assignment.getItems().stream()
                .anyMatch(item -> item.getQuestionType() == QuestionType.ESSAY);
    }

    private BigDecimal scoreValue(BigDecimal score) {
        return score == null ? ZERO : score;
    }

    private User requireTeacherInCurrentCenter() {
        User currentUser = authorizationService.getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("Chỉ có Giáo viên mới có quyền quản lý đánh giá");
        }
        if (!membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId)) {
            throw new AccessDeniedException("Người dùng không thuộc trung tâm này");
        }
        return currentUser;
    }

    private User requireStudentInCurrentCenter() {
        User currentUser = authorizationService.getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.STUDENT) {
            throw new AccessDeniedException("Chỉ có Học viên mới có quyền xem kết quả đánh giá");
        }
        if (!membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId)) {
            throw new AccessDeniedException("Người dùng không thuộc trung tâm này");
        }
        return currentUser;
    }

    private Long requiredCurrentCenterId() {
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new BadRequestException("Không xác định được trung tâm làm việc");
        }
        return centerId;
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
