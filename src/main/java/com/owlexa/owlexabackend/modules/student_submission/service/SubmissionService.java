package com.owlexa.owlexabackend.modules.student_submission.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItem;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItemOption;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipient;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentStatus;
import com.owlexa.owlexabackend.modules.assignment.repository.AssignmentRecipientRepository;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.student_submission.dto.request.SaveSubmissionAnswersRequest;
import com.owlexa.owlexabackend.modules.student_submission.dto.request.SubmissionAnswerRequest;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.StudentAttemptDetailResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.StudentAttemptSummaryResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.TeacherAttemptDetailResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.TeacherSubmissionSummaryResponse;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAnswer;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAnswerOption;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttempt;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttemptStatus;
import com.owlexa.owlexabackend.modules.student_submission.mapper.SubmissionMapper;
import com.owlexa.owlexabackend.modules.student_submission.repository.SubmissionAttemptRepository;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final AssignmentRecipientRepository assignmentRecipientRepository;
    private final SubmissionAttemptRepository submissionAttemptRepository;
    private final AuthorizationService authorizationService;
    private final MembershipRepository membershipRepository;
    private final SubmissionMapper submissionMapper;

    @Transactional
    public StudentAttemptDetailResponse startOrResumeAttempt(Long assignmentId) {
        User student = requireStudentInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        Instant now = Instant.now();

        AssignmentRecipient recipient = findStudentRecipient(assignmentId, student.getId(), centerId);
        validateCanStartAttempt(recipient.getAssignment(), now);

        SubmissionAttempt attempt = submissionAttemptRepository
                .findByAssignmentRecipient_IdAndStatus(recipient.getId(), SubmissionAttemptStatus.IN_PROGRESS)
                .orElseGet(() -> createAttempt(recipient, now));

        return submissionMapper.toStudentAttemptDetailResponse(attempt);
    }

    @Transactional(readOnly = true)
    public List<StudentAttemptSummaryResponse> getAttemptHistory(Long assignmentId) {
        User student = requireStudentInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        AssignmentRecipient recipient = findStudentRecipient(assignmentId, student.getId(), centerId);
        return submissionAttemptRepository.findAllByAssignmentRecipient_IdOrderByAttemptNumberDesc(recipient.getId())
                .stream()
                .map(submissionMapper::toStudentAttemptSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentAttemptDetailResponse getAttemptDetail(Long attemptId) {
        User student = requireStudentInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        SubmissionAttempt attempt = submissionAttemptRepository
                .findByIdAndAssignmentRecipient_StudentUser_IdAndAssignmentRecipient_Assignment_Center_IdAndAssignmentRecipient_Assignment_DeletedAtIsNull(
                        attemptId,
                        student.getId(),
                        centerId
                )
                .orElseThrow(() -> new ResourceNotFoundException("Submission attempt not found with id: " + attemptId));

        return submissionMapper.toStudentAttemptDetailResponse(attempt);
    }

    @Transactional
    public StudentAttemptDetailResponse saveAnswers(Long attemptId, SaveSubmissionAnswersRequest request) {
        User student = requireStudentInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        Instant now = Instant.now();

        SubmissionAttempt attempt = findStudentAttempt(attemptId, student.getId(), centerId);
        requireInProgress(attempt, "Only in-progress attempts can be updated");
        validateAssignmentStillAccessible(attempt.getAssignmentRecipient().getAssignment());
        replaceAnswers(attempt, request);
        attempt.setLastSavedAt(now);

        return submissionMapper.toStudentAttemptDetailResponse(submissionAttemptRepository.save(attempt));
    }

    @Transactional
    public StudentAttemptDetailResponse submitAttempt(Long attemptId) {
        User student = requireStudentInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        Instant now = Instant.now();

        SubmissionAttempt attempt = findStudentAttempt(attemptId, student.getId(), centerId);
        requireInProgress(attempt, "Only in-progress attempts can be submitted");

        Assignment assignment = attempt.getAssignmentRecipient().getAssignment();
        validateAssignmentStillAccessible(assignment);
        scoreAttempt(attempt, assignment, now);
        attempt.setSubmittedAt(now);
        attempt.setStatus(isPastDue(assignment, now)
                ? SubmissionAttemptStatus.AUTO_SUBMITTED
                : SubmissionAttemptStatus.SUBMITTED);
        attempt.setActiveAttemptKey(null);
        attempt.setLastSavedAt(now);

        return submissionMapper.toStudentAttemptDetailResponse(submissionAttemptRepository.save(attempt));
    }

    @Transactional(readOnly = true)
    public Page<TeacherSubmissionSummaryResponse> findAssignmentSubmissions(Long assignmentId, Pageable pageable) {
        requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        return assignmentRecipientRepository
                .findAllByAssignment_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNull(
                        assignmentId,
                        centerId,
                        pageable
                )
                .map(recipient -> {
                    SubmissionAttempt latestAttempt = submissionAttemptRepository
                            .findTopByAssignmentRecipient_IdOrderByStartedAtDesc(recipient.getId())
                            .orElse(null);
                    long attemptsCount = submissionAttemptRepository.countByAssignmentRecipient_Id(recipient.getId());
                    return submissionMapper.toTeacherSubmissionSummaryResponse(recipient, latestAttempt, attemptsCount);
                });
    }

    @Transactional(readOnly = true)
    public TeacherAttemptDetailResponse findAttemptDetailForTeacher(Long attemptId) {
        requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        SubmissionAttempt attempt = submissionAttemptRepository
                .findByIdAndAssignmentRecipient_Assignment_Center_IdAndAssignmentRecipient_Assignment_DeletedAtIsNull(
                        attemptId,
                        centerId
                )
                .orElseThrow(() -> new ResourceNotFoundException("Submission attempt not found with id: " + attemptId));

        return submissionMapper.toTeacherAttemptDetailResponse(attempt);
    }

    private SubmissionAttempt createAttempt(AssignmentRecipient recipient, Instant now) {
        Assignment assignment = recipient.getAssignment();
        validateAttemptLimit(recipient);

        int nextAttemptNumber = submissionAttemptRepository
                .findTopByAssignmentRecipient_IdOrderByAttemptNumberDesc(recipient.getId())
                .map(latest -> latest.getAttemptNumber() + 1)
                .orElse(1);

        SubmissionAttempt attempt = SubmissionAttempt.builder()
                .assignmentRecipient(recipient)
                .status(SubmissionAttemptStatus.IN_PROGRESS)
                .attemptNumber(nextAttemptNumber)
                .assignmentTitleSnapshot(assignment.getTitle())
                .assignmentTypeSnapshot(assignment.getType())
                .startedAt(now)
                .lastSavedAt(now)
                .activeAttemptKey(recipient.getId())
                .build();

        return submissionAttemptRepository.save(attempt);
    }

    private void validateAttemptLimit(AssignmentRecipient recipient) {
        Integer attemptLimit = recipient.getAssignment().getAttemptLimit();
        if (attemptLimit == null) {
            return;
        }

        long attemptsCount = submissionAttemptRepository.countByAssignmentRecipient_Id(recipient.getId());
        if (attemptsCount >= attemptLimit) {
            throw new BadRequestException("Attempt limit has been reached");
        }
    }

    private void replaceAnswers(SubmissionAttempt attempt, SaveSubmissionAnswersRequest request) {
        if (request == null || request.getAnswers() == null) {
            throw new BadRequestException("Answers are required");
        }

        Assignment assignment = attempt.getAssignmentRecipient().getAssignment();
        Map<Long, AssignmentItem> itemsById = assignment.getItems().stream()
                .collect(Collectors.toMap(AssignmentItem::getId, Function.identity()));

        Set<Long> answerItemIds = new HashSet<>();
        List<SubmissionAnswer> answers = request.getAnswers().stream()
                .map(answerRequest -> toAnswer(attempt, answerRequest, itemsById, answerItemIds))
                .toList();

        attempt.getAnswers().clear();
        attempt.getAnswers().addAll(answers);
    }

    private SubmissionAnswer toAnswer(
            SubmissionAttempt attempt,
            SubmissionAnswerRequest request,
            Map<Long, AssignmentItem> itemsById,
            Set<Long> answerItemIds
    ) {
        if (request == null || request.getAssignmentItemId() == null) {
            throw new BadRequestException("Assignment item id is required");
        }
        if (!answerItemIds.add(request.getAssignmentItemId())) {
            throw new BadRequestException("Duplicate answer for assignment item is not allowed");
        }

        AssignmentItem item = itemsById.get(request.getAssignmentItemId());
        if (item == null) {
            throw new BadRequestException("Answer assignment item is invalid");
        }

        validateAnswerShape(item, request);

        SubmissionAnswer answer = SubmissionAnswer.builder()
                .attempt(attempt)
                .assignmentItem(item)
                .answerText(normalizeOptionalText(request.getAnswerText()))
                .build();

        if (item.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
            attachSelectedOptions(answer, item, request.getSelectedOptionIds());
        }

        return answer;
    }

    private void validateAnswerShape(AssignmentItem item, SubmissionAnswerRequest request) {
        boolean hasAnswerText = request.getAnswerText() != null && !request.getAnswerText().isBlank();
        boolean hasSelectedOptions = request.getSelectedOptionIds() != null && !request.getSelectedOptionIds().isEmpty();

        if (item.getQuestionType() == QuestionType.MULTIPLE_CHOICE && hasAnswerText) {
            throw new BadRequestException("Multiple choice answers must use selected options");
        }
        if (item.getQuestionType() == QuestionType.ESSAY && hasSelectedOptions) {
            throw new BadRequestException("Essay answers must use answer text");
        }
    }

    private void attachSelectedOptions(
            SubmissionAnswer answer,
            AssignmentItem item,
            List<Long> selectedOptionIds
    ) {
        if (selectedOptionIds == null || selectedOptionIds.isEmpty()) {
            return;
        }

        Map<Long, AssignmentItemOption> optionsById = item.getOptions().stream()
                .collect(Collectors.toMap(AssignmentItemOption::getId, Function.identity()));
        Set<Long> uniqueSelectedIds = new LinkedHashSet<>(selectedOptionIds);
        if (uniqueSelectedIds.size() != selectedOptionIds.size()) {
            throw new BadRequestException("Duplicate selected option is not allowed");
        }

        for (Long optionId : uniqueSelectedIds) {
            AssignmentItemOption option = optionsById.get(optionId);
            if (option == null) {
                throw new BadRequestException("Selected option does not belong to the assignment item");
            }
            answer.getSelectedOptions().add(SubmissionAnswerOption.builder()
                    .submissionAnswer(answer)
                    .assignmentItemOption(option)
                    .build());
        }
    }

    private void scoreAttempt(SubmissionAttempt attempt, Assignment assignment, Instant gradedAt) {
        Map<Long, SubmissionAnswer> answersByItemId = attempt.getAnswers().stream()
                .collect(Collectors.toMap(answer -> answer.getAssignmentItem().getId(), Function.identity()));

        BigDecimal autoScore = ZERO;
        BigDecimal maxScore = ZERO;
        Map<Long, AssignmentItem> itemsById = new HashMap<>();
        assignment.getItems().forEach(item -> itemsById.put(item.getId(), item));

        for (AssignmentItem item : assignment.getItems()) {
            BigDecimal itemMaxScore = scoreValue(item.getPoints());
            maxScore = maxScore.add(itemMaxScore);

            SubmissionAnswer answer = answersByItemId.get(item.getId());
            if (answer == null) {
                continue;
            }

            answer.setMaxScore(itemMaxScore);
            if (item.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                BigDecimal itemAutoScore = isExactMatch(answer, item) ? itemMaxScore : ZERO;
                answer.setAutoScore(itemAutoScore);
                answer.setGradedAt(gradedAt);
                autoScore = autoScore.add(itemAutoScore);
            } else {
                answer.setAutoScore(null);
                answer.setGradedAt(null);
            }
        }

        attempt.setAutoScore(autoScore);
        attempt.setMaxScore(maxScore);
    }

    private boolean isExactMatch(SubmissionAnswer answer, AssignmentItem item) {
        Set<Long> correctOptionIds = item.getOptions().stream()
                .filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                .map(AssignmentItemOption::getId)
                .collect(Collectors.toSet());
        Set<Long> selectedOptionIds = answer.getSelectedOptions().stream()
                .map(option -> option.getAssignmentItemOption().getId())
                .collect(Collectors.toSet());
        return Objects.equals(correctOptionIds, selectedOptionIds);
    }

    private BigDecimal scoreValue(BigDecimal points) {
        return points == null ? ZERO : points;
    }

    private void validateCanStartAttempt(Assignment assignment, Instant now) {
        validateAssignmentStillAccessible(assignment);
        if (assignment.getStatus() != AssignmentStatus.ACTIVE && assignment.getStatus() != AssignmentStatus.SCHEDULED) {
            throw new BadRequestException("Assignment is not open for attempts");
        }
        if (assignment.getOpenAt() != null && assignment.getOpenAt().isAfter(now)) {
            throw new BadRequestException("Assignment is not open yet");
        }
        if (isPastDue(assignment, now)) {
            throw new BadRequestException("Assignment due time has passed");
        }
    }

    private void validateAssignmentStillAccessible(Assignment assignment) {
        if (assignment.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Assignment not found with id: " + assignment.getId());
        }
        if (assignment.getStatus() == AssignmentStatus.CLOSED || assignment.getStatus() == AssignmentStatus.ARCHIVED) {
            throw new BadRequestException("Assignment is no longer accepting submissions");
        }
    }

    private boolean isPastDue(Assignment assignment, Instant now) {
        return assignment.getDueAt() != null && now.isAfter(assignment.getDueAt());
    }

    private AssignmentRecipient findStudentRecipient(Long assignmentId, Long studentUserId, Long centerId) {
        return assignmentRecipientRepository
                .findByAssignment_IdAndStudentUser_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNull(
                        assignmentId,
                        studentUserId,
                        centerId
                )
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));
    }

    private SubmissionAttempt findStudentAttempt(Long attemptId, Long studentUserId, Long centerId) {
        return submissionAttemptRepository
                .findByIdAndAssignmentRecipient_StudentUser_IdAndAssignmentRecipient_Assignment_Center_IdAndAssignmentRecipient_Assignment_DeletedAtIsNull(
                        attemptId,
                        studentUserId,
                        centerId
                )
                .orElseThrow(() -> new ResourceNotFoundException("Submission attempt not found with id: " + attemptId));
    }

    private void requireInProgress(SubmissionAttempt attempt, String message) {
        if (attempt.getStatus() != SubmissionAttemptStatus.IN_PROGRESS) {
            throw new BadRequestException(message);
        }
    }

    private User requireTeacherInCurrentCenter() {
        User currentUser = authorizationService.getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("Only TEACHER can view submissions");
        }

        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
        if (!hasMembership) {
            throw new AccessDeniedException("User is not a member of this center");
        }

        return currentUser;
    }

    private User requireStudentInCurrentCenter() {
        User currentUser = authorizationService.getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.STUDENT) {
            throw new AccessDeniedException("Only STUDENT can manage submission attempts");
        }

        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
        if (!hasMembership) {
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

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
