package com.owlexa.owlexabackend.modules.student_submission.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingJobStatus;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingResult;
import com.owlexa.owlexabackend.modules.ai_grading.repository.AIGradingResultRepository;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingOutput;
import com.owlexa.owlexabackend.modules.ai_grading.service.AIGradingOutputReader;
import com.owlexa.owlexabackend.modules.ai_grading.service.AIGradingService;
import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItem;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItemOption;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipient;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentStatus;
import com.owlexa.owlexabackend.modules.assignment.repository.AssignmentRecipientRepository;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.student_submission.dto.request.SaveSubmissionAnswersRequest;
import com.owlexa.owlexabackend.modules.student_submission.dto.request.SubmissionAnswerRequest;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.StudentAIGradingCriterionResultResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.StudentAIGradingItemResultResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.StudentAIGradingImprovementResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.StudentAIGradingResultResponse;
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
import com.owlexa.owlexabackend.modules.teacher_review.entity.TeacherReview;
import com.owlexa.owlexabackend.modules.teacher_review.repository.TeacherReviewRepository;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final AssignmentRecipientRepository assignmentRecipientRepository;
    private final SubmissionAttemptRepository submissionAttemptRepository;
    private final AuthorizationService authorizationService;
    private final MembershipRepository membershipRepository;
    private final SubmissionMapper submissionMapper;
    private final AIGradingResultRepository aiGradingResultRepository;
    private final AIGradingService aiGradingService;
    private final AIGradingOutputReader aiGradingOutputReader;
    private final TeacherReviewRepository teacherReviewRepository;

    @Transactional
    public StudentAttemptDetailResponse startOrResumeAttempt(Long assignmentId, com.owlexa.owlexabackend.modules.student_submission.dto.request.StartAttemptRequest request) {
        User student = requireStudentInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        Instant now = Instant.now();

        AssignmentRecipient recipient = findStudentRecipient(assignmentId, student.getId(), centerId);
        Assignment assignment = recipient.getAssignment();

        Optional<SubmissionAttempt> existingAttempt = submissionAttemptRepository
                .findByAssignmentRecipient_IdAndStatus(recipient.getId(), SubmissionAttemptStatus.IN_PROGRESS);

        if (existingAttempt.isPresent()) {
            SubmissionAttempt attempt = finalizeIfExpired(existingAttempt.get(), now);
            return toStudentDetail(attempt);
        }

        validateCanStartAttempt(assignment, now);

        if (assignment.getAccessPassword() != null && !assignment.getAccessPassword().isBlank()) {
            String providedPassword = request != null && request.getPassword() != null ? request.getPassword().trim() : "";
            if (!assignment.getAccessPassword().trim().equals(providedPassword)) {
                throw new BadRequestException("Mật khẩu đề thi không đúng.");
            }
        }

        SubmissionAttempt attempt = createAttempt(recipient, now);
        return toStudentDetail(attempt);
    }

    @Transactional(readOnly = true)
    public List<StudentAttemptSummaryResponse> getAttemptHistory(Long assignmentId) {
        User student = requireStudentInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        AssignmentRecipient recipient = findStudentRecipient(assignmentId, student.getId(), centerId);
        return submissionAttemptRepository.findAllByAssignmentRecipient_IdOrderByAttemptNumberDesc(recipient.getId())
                .stream()
                .map(attempt -> toStudentSummary(attempt))
                .toList();
    }

    @Transactional
    public StudentAttemptDetailResponse getAttemptDetail(Long attemptId) {
        User student = requireStudentInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        Instant now = Instant.now();

        SubmissionAttempt attempt = submissionAttemptRepository
                .findByIdAndAssignmentRecipient_StudentUser_IdAndAssignmentRecipient_Assignment_Center_IdAndAssignmentRecipient_Assignment_DeletedAtIsNull(
                        attemptId,
                        student.getId(),
                        centerId
                )
                .orElseThrow(() -> new ResourceNotFoundException("Submission attempt not found with id: " + attemptId));

        attempt = finalizeIfExpired(attempt, now);
        return toStudentDetail(attempt);
    }

    @Transactional
    public StudentAttemptDetailResponse saveAnswers(Long attemptId, SaveSubmissionAnswersRequest request) {
        User student = requireStudentInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        Instant now = Instant.now();

        SubmissionAttempt attempt = findStudentAttempt(attemptId, student.getId(), centerId);
        requireInProgress(attempt, "Only in-progress attempts can be updated");
        validateAssignmentStillAccessible(attempt.getAssignmentRecipient().getAssignment());

        attempt = finalizeIfExpired(attempt, now);
        if (attempt.getStatus() != SubmissionAttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Bài tập đã hết hạn, không thể lưu câu trả lời.");
        }

        replaceAnswers(attempt, request);
        attempt.setLastSavedAt(now);

        return toStudentDetail(submissionAttemptRepository.save(attempt));
    }

    @Transactional
    public StudentAttemptDetailResponse saveAudioProgress(Long attemptId, com.owlexa.owlexabackend.modules.student_submission.dto.request.AudioProgressUpdateRequest request) {
        User student = requireStudentInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        Instant now = Instant.now();

        SubmissionAttempt attempt = findStudentAttempt(attemptId, student.getId(), centerId);
        requireInProgress(attempt, "Only in-progress attempts can be updated");
        validateAssignmentStillAccessible(attempt.getAssignmentRecipient().getAssignment());

        attempt = finalizeIfExpired(attempt, now);
        if (attempt.getStatus() != SubmissionAttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Bài tập đã hết hạn, không thể lưu audio progress.");
        }

        int currentPosition = attempt.getAudioPositionSeconds() != null ? attempt.getAudioPositionSeconds() : 0;
        int incomingPosition = request.getPositionSeconds() != null ? request.getPositionSeconds() : 0;
        
        attempt.setAudioPositionSeconds(Math.max(currentPosition, incomingPosition));
        
        if (Boolean.TRUE.equals(request.getCompleted())) {
            attempt.setAudioCompleted(true);
        }

        return toStudentDetail(submissionAttemptRepository.save(attempt));
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

        attempt = finalizeIfExpired(attempt, now);
        if (attempt.getStatus() != SubmissionAttemptStatus.IN_PROGRESS) {
            return toStudentDetail(attempt);
        }

        scoreAttempt(attempt, assignment, now);
        attempt.setSubmittedAt(now);
        attempt.setStatus(SubmissionAttemptStatus.SUBMITTED);
        attempt.setActiveAttemptKey(null);
        attempt.setLastSavedAt(now);

        return toStudentDetail(submissionAttemptRepository.save(attempt));
    }

    /**
     * Submits the attempt and, when the assignment contains essay questions,
     * automatically triggers AI grading so the AI score and feedback are stored
     * immediately after submission. Student visibility of the AI result still
     * depends on the assignment's "show score" setting. AI grading failures
     * never block or roll back the submission itself.
     */
    public StudentAttemptDetailResponse submitAttemptWithAutoGrading(Long attemptId) {
        // Commit the submission (auto-scoring + status change) first.
        StudentAttemptDetailResponse submittedAttempt = submitAttempt(attemptId);

        User student = requireStudentInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        int essayCount = (int) submittedAttempt.getItems().stream()
                .filter(item -> item.getQuestionType() == QuestionType.ESSAY)
                .count();

        log.info(
                "Student submission committed: attemptId={}, assignmentId={}, studentId={}, status={}, showScore={}, essayCount={}",
                attemptId,
                submittedAttempt.getAssignmentId(),
                student.getId(),
                submittedAttempt.getStatus(),
                submittedAttempt.getShowScore(),
                essayCount
        );

        try {
            boolean triggered = aiGradingService.autoGradeOnSubmit(attemptId, centerId, student.getId());
            log.info(
                    "Auto AI grading evaluation finished: attemptId={}, assignmentId={}, studentId={}, triggered={}, showScore={}, essayCount={}",
                    attemptId,
                    submittedAttempt.getAssignmentId(),
                    student.getId(),
                    triggered,
                    submittedAttempt.getShowScore(),
                    essayCount
            );
        } catch (Exception exception) {
            // Auto-grading must never block the submission.
            log.warn(
                    "Auto AI grading failed: attemptId={}, assignmentId={}, studentId={}, showScore={}, essayCount={}, error={}",
                    attemptId,
                    submittedAttempt.getAssignmentId(),
                    student.getId(),
                    submittedAttempt.getShowScore(),
                    essayCount,
                    exception.getMessage(),
                    exception
            );
        }

        // Re-read so the response includes the freshly created AI grading result.
        return getAttemptDetail(attemptId);
    }

    private StudentAttemptSummaryResponse toStudentSummary(SubmissionAttempt attempt) {
        StudentAttemptSummaryResponse response = submissionMapper.toStudentAttemptSummaryResponse(attempt);
        attachAiSummary(response, attempt);
        return response;
    }

    private StudentAttemptDetailResponse toStudentDetail(SubmissionAttempt attempt) {
        StudentAttemptDetailResponse response = submissionMapper.toStudentAttemptDetailResponse(attempt);
        attachAiResult(response, attempt);
        return response;
    }

    /**
     * Expose the latest completed AI grading result to the student only when the
     * assignment has "show score" enabled and the attempt is no longer in
     * progress. When disabled, the student cannot see AI feedback — the teacher
     * can always review it on the teacher side.
     */
    private void attachAiResult(StudentAttemptDetailResponse response, SubmissionAttempt attempt) {
        if (!Boolean.TRUE.equals(response.getShowScore())) {
            return;
        }
        if (attempt.getStatus() == SubmissionAttemptStatus.IN_PROGRESS) {
            return;
        }
        aiGradingResultRepository
                .findTopBySubmissionAttempt_IdAndJob_StatusOrderByCreatedAtDesc(
                        attempt.getId(),
                        AIGradingJobStatus.COMPLETED
                )
                .ifPresent(result -> response.setAiResult(toStudentAiResult(result)));
    }

    private void attachAiSummary(StudentAttemptSummaryResponse response, SubmissionAttempt attempt) {
        Assignment assignment = attempt.getAssignmentRecipient().getAssignment();
        boolean showScore = assignment.getShowScore() == null || assignment.getShowScore();
        if (!showScore) {
            response.setAutoScore(null);
            response.setAiScore(null);
            response.setDisplayedScore(null);
            response.setMaxScore(null);
            return;
        }
        if (attempt.getStatus() == SubmissionAttemptStatus.IN_PROGRESS) {
            response.setDisplayedScore(null);
            return;
        }

        aiGradingResultRepository
                .findTopBySubmissionAttempt_IdAndJob_StatusOrderByCreatedAtDesc(
                        attempt.getId(),
                        AIGradingJobStatus.COMPLETED
                )
                .ifPresent(result -> {
                    response.setAiScore(result.getAiScore());
                    response.setDisplayedScore(scoreValue(response.getAutoScore()).add(scoreValue(result.getAiScore())));
                });
    }

    private StudentAIGradingResultResponse toStudentAiResult(AIGradingResult result) {
        Optional<AIGradingOutput> structuredOutput = aiGradingOutputReader.read(result);
        return StudentAIGradingResultResponse.builder()
                .resultId(result.getId())
                .jobId(result.getJob().getId())
                .summary(result.getSummary())
                .overallFeedback(result.getOverallFeedback())
                .focusArea(structuredOutput.map(AIGradingOutput::focusArea).orElse(null))
                .aiScore(result.getAiScore())
                .maxScore(result.getMaxScore())
                .confidence(result.getConfidence())
                .createdAt(result.getCreatedAt())
                .criteria(structuredOutput
                        .map(output -> output.criteria() == null ? List.<StudentAIGradingCriterionResultResponse>of() : output.criteria().stream()
                                .map(criterion -> StudentAIGradingCriterionResultResponse.builder()
                                        .name(criterion.name())
                                        .score(criterion.score())
                                        .maxScore(criterion.maxScore())
                                        .feedback(criterion.feedback())
                                        .build())
                                .toList())
                        .orElseGet(List::of))
                .improvements(structuredOutput
                        .map(output -> output.improvements() == null ? List.<StudentAIGradingImprovementResponse>of() : output.improvements().stream()
                                .map(improvement -> StudentAIGradingImprovementResponse.builder()
                                        .category(improvement.category())
                                        .issue(improvement.issue())
                                        .suggestion(improvement.suggestion())
                                        .example(improvement.example())
                                        .build())
                                .toList())
                        .orElseGet(List::of))
                .itemResults(result.getItemResults().stream()
                        .sorted(Comparator.comparing(item -> item.getAssignmentItem().getDisplayOrder()))
                        .map(item -> StudentAIGradingItemResultResponse.builder()
                                .id(item.getId())
                                .assignmentItemId(item.getAssignmentItem().getId())
                                .aiScore(item.getAiScore())
                                .maxScore(item.getMaxScore())
                                .feedback(item.getFeedback())
                                .rubricAnalysis(item.getRubricAnalysis())
                                .confidence(item.getConfidence())
                                .build())
                        .toList())
                .build();
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

                    BigDecimal latestFinalScore = null;
                    boolean isGraded = false;

                    if (latestAttempt != null && latestAttempt.getStatus() != SubmissionAttemptStatus.IN_PROGRESS) {
                        Optional<TeacherReview> reviewOpt = teacherReviewRepository != null
                                ? teacherReviewRepository.findBySubmissionAttempt_Id(latestAttempt.getId())
                                : Optional.empty();
                        if (reviewOpt.isPresent() && reviewOpt.get().getFinalScore() != null) {
                            latestFinalScore = reviewOpt.get().getFinalScore();
                            isGraded = true;
                        } else {
                            Optional<AIGradingResult> aiOpt = aiGradingResultRepository != null
                                    ? aiGradingResultRepository.findTopBySubmissionAttempt_IdAndJob_StatusOrderByCreatedAtDesc(
                                            latestAttempt.getId(),
                                            AIGradingJobStatus.COMPLETED
                                    )
                                    : Optional.empty();
                            if (aiOpt.isPresent() && aiOpt.get().getAiScore() != null) {
                                BigDecimal auto = latestAttempt.getAutoScore() != null ? latestAttempt.getAutoScore() : BigDecimal.ZERO;
                                latestFinalScore = auto.add(aiOpt.get().getAiScore());
                                isGraded = true;
                            } else {
                                boolean hasSubjectiveItems = recipient.getAssignment().getItems() != null && recipient.getAssignment().getItems().stream()
                                        .anyMatch(item -> item.getQuestionType() != QuestionType.MULTIPLE_CHOICE);
                                if (!hasSubjectiveItems) {
                                    latestFinalScore = latestAttempt.getAutoScore();
                                    isGraded = true;
                                } else {
                                    latestFinalScore = latestAttempt.getAutoScore();
                                    isGraded = false;
                                }
                            }
                        }
                    }

                    return submissionMapper.toTeacherSubmissionSummaryResponse(
                            recipient,
                            latestAttempt,
                            attemptsCount,
                            latestFinalScore,
                            isGraded
                    );
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

        Instant expiresAt = null;
        Instant candidateByDuration = assignment.getTimeLimitMinutes() != null ? now.plus(assignment.getTimeLimitMinutes(), java.time.temporal.ChronoUnit.MINUTES) : null;
        Instant candidateByDueAt = assignment.getDueAt();
        
        if (candidateByDuration != null && candidateByDueAt != null) {
            expiresAt = candidateByDuration.isBefore(candidateByDueAt) ? candidateByDuration : candidateByDueAt;
        } else if (candidateByDuration != null) {
            expiresAt = candidateByDuration;
        } else if (candidateByDueAt != null) {
            expiresAt = candidateByDueAt;
        }

        SubmissionAttempt attempt = SubmissionAttempt.builder()
                .assignmentRecipient(recipient)
                .status(SubmissionAttemptStatus.IN_PROGRESS)
                .attemptNumber(nextAttemptNumber)
                .assignmentTitleSnapshot(assignment.getTitle())
                .startedAt(now)
                .lastSavedAt(now)
                .activeAttemptKey(recipient.getId())
                .expiresAt(expiresAt)
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
            throw new BadRequestException("Đã đạt số lần làm bài tối đa");
        }
    }

    private void replaceAnswers(SubmissionAttempt attempt, SaveSubmissionAnswersRequest request) {
        if (request == null || request.getAnswers() == null) {
            throw new BadRequestException("Danh sách câu trả lời không được để trống");
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
            throw new BadRequestException("Mã câu hỏi không được để trống");
        }
        if (!answerItemIds.add(request.getAssignmentItemId())) {
            throw new BadRequestException("Không được phép chọn câu trả lời trùng lặp cho cùng một câu hỏi");
        }

        AssignmentItem item = itemsById.get(request.getAssignmentItemId());
        if (item == null) {
            throw new BadRequestException("Câu hỏi trả lời không hợp lệ");
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
            throw new BadRequestException("Câu hỏi trắc nghiệm phải chọn đáp án");
        }
        if (item.getQuestionType() == QuestionType.ESSAY && hasSelectedOptions) {
            throw new BadRequestException("Câu hỏi tự luận phải nhập văn bản");
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
            throw new BadRequestException("Không được phép chọn cùng một đáp án nhiều lần");
        }

        for (Long optionId : uniqueSelectedIds) {
            AssignmentItemOption option = optionsById.get(optionId);
            if (option == null) {
                throw new BadRequestException("Đáp án được chọn không thuộc câu hỏi này");
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
                answer = createBlankAnswer(attempt, item);
                attempt.getAnswers().add(answer);
                answersByItemId.put(item.getId(), answer);
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

    private SubmissionAnswer createBlankAnswer(SubmissionAttempt attempt, AssignmentItem item) {
        return SubmissionAnswer.builder()
                .attempt(attempt)
                .assignmentItem(item)
                .selectedOptions(new java.util.ArrayList<>())
                .build();
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
            throw new BadRequestException("Bài tập chưa mở để làm bài");
        }
        if (assignment.getOpenAt() != null && assignment.getOpenAt().isAfter(now)) {
            throw new BadRequestException("Bài tập chưa đến thời gian làm bài");
        }
        if (isPastDue(assignment, now)) {
            throw new BadRequestException("Đã hết thời hạn làm bài tập");
        }
    }

    private void validateAssignmentStillAccessible(Assignment assignment) {
        if (assignment.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Không tìm thấy bài tập với ID: " + assignment.getId());
        }
        if (assignment.getStatus() == AssignmentStatus.CLOSED || assignment.getStatus() == AssignmentStatus.ARCHIVED) {
            throw new BadRequestException("Bài tập đã đóng hoặc được lưu trữ, không nhận bài nộp nữa");
        }
    }

    private boolean isPastDue(Assignment assignment, Instant now) {
        return assignment.getDueAt() != null && !now.isBefore(assignment.getDueAt());
    }

    /**
     * Single domain rule: if the attempt is IN_PROGRESS and the assignment deadline
     * has passed, finalize it as AUTO_SUBMITTED with the answers saved so far.
     * Returns the (possibly finalized) attempt.
     */
    private SubmissionAttempt finalizeIfExpired(SubmissionAttempt attempt, Instant now) {
        if (attempt.getStatus() != SubmissionAttemptStatus.IN_PROGRESS) {
            return attempt;
        }
        if (attempt.getExpiresAt() != null && !now.isBefore(attempt.getExpiresAt())) {
            Assignment assignment = attempt.getAssignmentRecipient().getAssignment();
            log.info("Finalizing expired attempt: attemptId={}, expiresAt={}, now={}",
                    attempt.getId(),
                    attempt.getExpiresAt(),
                    now);
            scoreAttempt(attempt, assignment, now);
            attempt.setSubmittedAt(now);
            attempt.setStatus(SubmissionAttemptStatus.AUTO_SUBMITTED);
            attempt.setActiveAttemptKey(null);
            attempt.setLastSavedAt(now);
            return submissionAttemptRepository.save(attempt);
        }
        return attempt;
    }

    /**
     * Scheduler entry point: finalize all IN_PROGRESS attempts whose assignment
     * deadline has passed. Idempotent — safe to call repeatedly.
     */
    @Transactional
    public int finalizeExpiredAttempts(Instant now) {
        List<SubmissionAttempt> expired = submissionAttemptRepository
                .findAllByStatusAndExpiresAtLessThanEqual(
                        SubmissionAttemptStatus.IN_PROGRESS, now);
        int count = 0;
        for (SubmissionAttempt attempt : expired) {
            finalizeIfExpired(attempt, now);
            count++;
        }
        if (count > 0) {
            log.info("SubmissionDeadlineJob: finalized {} expired attempts", count);
        }
        return count;
    }

    private AssignmentRecipient findStudentRecipient(Long assignmentId, Long studentUserId, Long centerId) {
        return assignmentRecipientRepository
                .findByAssignment_IdAndStudentUser_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNull(
                        assignmentId,
                        studentUserId,
                        centerId
                )
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài tập với ID: " + assignmentId));
    }

    private SubmissionAttempt findStudentAttempt(Long attemptId, Long studentUserId, Long centerId) {
        return submissionAttemptRepository
                .findByIdAndAssignmentRecipient_StudentUser_IdAndAssignmentRecipient_Assignment_Center_IdAndAssignmentRecipient_Assignment_DeletedAtIsNull(
                        attemptId,
                        studentUserId,
                        centerId
                )
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt làm bài với ID: " + attemptId));
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
            throw new AccessDeniedException("Chỉ có Giáo viên mới có quyền xem bài nộp");
        }

        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
        if (!hasMembership) {
            throw new AccessDeniedException("Người dùng không thuộc trung tâm này");
        }

        return currentUser;
    }

    private User requireStudentInCurrentCenter() {
        User currentUser = authorizationService.getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.STUDENT) {
            throw new AccessDeniedException("Chỉ có Học viên mới có quyền thực hiện bài nộp");
        }

        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
        if (!hasMembership) {
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
