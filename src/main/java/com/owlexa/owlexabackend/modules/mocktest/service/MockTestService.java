package com.owlexa.owlexabackend.modules.mocktest.service;
import com.owlexa.owlexabackend.modules.mocktest.dto.request.MockTestQuestionRequest;
import com.owlexa.owlexabackend.modules.mocktest.dto.request.MockTestRequest;
import com.owlexa.owlexabackend.modules.mocktest.dto.request.MockTestSaveAnswerRequest;
import com.owlexa.owlexabackend.modules.mocktest.dto.request.MockTestSubmitAnswerRequest;
import com.owlexa.owlexabackend.modules.mocktest.dto.request.MockTestSubmitRequest;
import com.owlexa.owlexabackend.modules.mocktest.dto.response.MockTestAttemptAnswerResponse;
import com.owlexa.owlexabackend.modules.mocktest.dto.response.MockTestAttemptResponse;
import com.owlexa.owlexabackend.modules.mocktest.dto.response.MockTestQuestionResponse;
import com.owlexa.owlexabackend.modules.mocktest.dto.response.MockTestResponse;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTest;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestAttempt;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestAttemptAnswer;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestAttemptStatus;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestLevel;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestQuestion;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.mocktest.repository.MockTestAttemptAnswerRepository;
import com.owlexa.owlexabackend.modules.mocktest.repository.MockTestAttemptRepository;
import com.owlexa.owlexabackend.modules.mocktest.repository.MockTestQuestionRepository;
import com.owlexa.owlexabackend.modules.mocktest.repository.MockTestRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MockTestService {

    private final MockTestRepository mockTestRepository;
    private final MockTestQuestionRepository questionRepository;
    private final MockTestAttemptRepository attemptRepository;
    private final MockTestAttemptAnswerRepository answerRepository;
    private final CenterRepository centerRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;

    @Transactional(readOnly = true)
    public List<MockTestResponse> getOwnerTests() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertOwnerAndCenterAccess(currentUser, centerId);

        return mockTestRepository.findAllByCenter_IdOrderByCreatedAtDesc(centerId)
                .stream()
                .map(test -> toResponse(test, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MockTestResponse> getAvailableTestsForStudent() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertStudentAccess(currentUser, centerId);

        return mockTestRepository.findAllByCenter_IdOrderByCreatedAtDesc(centerId)
                .stream()
                .filter(test -> Boolean.TRUE.equals(test.getIsActive()))
                .map(test -> toResponse(test, false))
                .toList();
    }

    @Transactional
    public MockTestResponse createTest(MockTestRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertOwnerAndCenterAccess(currentUser, centerId);

        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));

        MockTest mockTest = MockTest.builder()
                .center(center)
                .title(request.getTitle().trim())
                .description(trimToNull(request.getDescription()))
                .level(request.getLevel())
                .duration(request.getDuration() == null ? 90 : request.getDuration())
                .totalQuestions(request.getTotalQuestions() == null ? 0 : request.getTotalQuestions())
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();

        return toResponse(mockTestRepository.save(mockTest), false);
    }

    @Transactional
    public MockTestResponse updateTest(Long testId, MockTestRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertOwnerAndCenterAccess(currentUser, centerId);

        MockTest mockTest = findOwnedTest(testId, centerId);
        mockTest.setTitle(request.getTitle().trim());
        mockTest.setDescription(trimToNull(request.getDescription()));
        mockTest.setLevel(request.getLevel());
        mockTest.setDuration(request.getDuration() == null ? mockTest.getDuration() : request.getDuration());
        mockTest.setTotalQuestions(request.getTotalQuestions() == null ? mockTest.getTotalQuestions() : request.getTotalQuestions());
        if (request.getIsActive() != null) {
            mockTest.setActive(request.getIsActive());
        }

        return toResponse(mockTestRepository.save(mockTest), false);
    }

    @Transactional
    public void deleteTest(Long testId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertOwnerAndCenterAccess(currentUser, centerId);

        MockTest mockTest = findOwnedTest(testId, centerId);
        mockTest.setActive(false);
        mockTestRepository.save(mockTest);
    }

    @Transactional(readOnly = true)
    public List<MockTestQuestionResponse> getQuestionsForOwner(Long testId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertOwnerAndCenterAccess(currentUser, centerId);

        MockTest mockTest = findOwnedTest(testId, centerId);
        return questionRepository.findAllByMockTest_IdOrderBySortOrderAscIdAsc(mockTest.getId())
                .stream()
                .map(this::toQuestionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MockTestQuestionResponse> getQuestionsForStudent(Long testId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertStudentAccess(currentUser, centerId);

        MockTest mockTest = findActiveTestInCenter(testId, centerId);
        return questionRepository.findAllByMockTest_IdOrderBySortOrderAscIdAsc(mockTest.getId())
                .stream()
                .map(question -> toQuestionResponse(question, false))
                .toList();
    }

    @Transactional
    public MockTestQuestionResponse addQuestion(Long testId, MockTestQuestionRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertOwnerAndCenterAccess(currentUser, centerId);

        MockTest mockTest = findOwnedTest(testId, centerId);
        MockTestQuestion question = MockTestQuestion.builder()
                .mockTest(mockTest)
                .questionText(request.getQuestionText().trim())
                .optionA(request.getOptionA().trim())
                .optionB(request.getOptionB().trim())
                .optionC(request.getOptionC().trim())
                .optionD(request.getOptionD().trim())
                .correctAnswer(request.getCorrectAnswer().trim().toUpperCase())
                .explanation(trimToNull(request.getExplanation()))
                .sortOrder(request.getSortOrder() == null
                        ? questionRepository.findAllByMockTest_IdOrderBySortOrderAscIdAsc(mockTest.getId()).size()
                        : request.getSortOrder())
                .build();

        return toQuestionResponse(questionRepository.save(question), true);
    }

    @Transactional
    public MockTestQuestionResponse updateQuestion(Long testId, Long questionId, MockTestQuestionRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertOwnerAndCenterAccess(currentUser, centerId);

        MockTestQuestion question = findOwnedQuestion(testId, questionId, centerId);
        question.setQuestionText(request.getQuestionText().trim());
        question.setOptionA(request.getOptionA().trim());
        question.setOptionB(request.getOptionB().trim());
        question.setOptionC(request.getOptionC().trim());
        question.setOptionD(request.getOptionD().trim());
        question.setCorrectAnswer(request.getCorrectAnswer().trim().toUpperCase());
        question.setExplanation(trimToNull(request.getExplanation()));
        if (request.getSortOrder() != null) {
            question.setSortOrder(request.getSortOrder());
        }

        return toQuestionResponse(questionRepository.save(question), true);
    }

    @Transactional
    public void deleteQuestion(Long testId, Long questionId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertOwnerAndCenterAccess(currentUser, centerId);

        MockTestQuestion question = findOwnedQuestion(testId, questionId, centerId);
        questionRepository.delete(question);
    }

    @Transactional(readOnly = true)
    public List<MockTestAttemptResponse> getAttemptsForOwner(Long testId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertOwnerAndCenterAccess(currentUser, centerId);

        MockTest mockTest = findOwnedTest(testId, centerId);
        return attemptRepository.findAllByMockTest_IdOrderByStartedAtDesc(mockTest.getId())
                .stream()
                .map(attempt -> toAttemptResponse(attempt, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public MockTestAttemptResponse getAttemptDetail(Long attemptId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        MockTestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found with id: " + attemptId));

        if (currentUser.getRole() == Role.OWNER) {
            assertOwnerAndCenterAccess(currentUser, centerId);
            if (!attempt.getCenter().getId().equals(centerId)) {
                throw new TenancyViolationException("Attempt " + attemptId + " belongs to another center");
            }
            return toAttemptResponse(attempt, true);
        }

        if (currentUser.getRole() == Role.STUDENT) {
            assertStudentAccess(currentUser, centerId);
            if (!attempt.getStudentUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You do not have permission to view this attempt");
            }
            return toAttemptResponse(attempt, true);
        }

        throw new AccessDeniedException("You do not have permission to view this attempt");
    }

    @Transactional(readOnly = true)
    public List<MockTestAttemptResponse> getMyResults() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertStudentAccess(currentUser, centerId);

        return attemptRepository.findAllByStudentUser_IdAndStatusOrderByStartedAtDesc(currentUser.getId(), MockTestAttemptStatus.COMPLETED)
                .stream()
                .filter(attempt -> attempt.getCenter().getId().equals(centerId))
                .map(attempt -> toAttemptResponse(attempt, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public MockTestAttemptResponse getMyResult(Long attemptId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertStudentAccess(currentUser, centerId);

        MockTestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found with id: " + attemptId));

        if (!attempt.getStudentUser().getId().equals(currentUser.getId()) || !attempt.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("You do not have permission to view this result");
        }

        return toAttemptResponse(attempt, true);
    }

    @Transactional
    public MockTestAttemptResponse startTest(Long testId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertStudentAccess(currentUser, centerId);

        MockTest mockTest = findActiveTestInCenter(testId, centerId);
        List<MockTestQuestion> questions = questionRepository.findAllByMockTest_IdOrderBySortOrderAscIdAsc(mockTest.getId());
        if (questions.isEmpty()) {
            throw new BadRequestException("Mock test does not have any questions yet");
        }

        MockTestAttempt existing = attemptRepository
                .findTopByStudentUser_IdAndMockTest_IdAndStatusOrderByStartedAtDesc(currentUser.getId(), mockTest.getId(), MockTestAttemptStatus.IN_PROGRESS)
                .orElse(null);
        if (existing != null) {
            return toAttemptResponse(existing, false);
        }

        MockTestAttempt attempt = MockTestAttempt.builder()
                .mockTest(mockTest)
                .center(mockTest.getCenter())
                .studentUser(currentUser)
                .testTitleSnapshot(mockTest.getTitle())
                .score(0)
                .maxScore(questions.size())
                .correctAnswers(0)
                .totalQuestions(questions.size())
                .status(MockTestAttemptStatus.IN_PROGRESS)
                .build();

        MockTestAttempt savedAttempt = attemptRepository.save(attempt);

        List<MockTestAttemptAnswer> blankAnswers = questions.stream()
                .map(question -> MockTestAttemptAnswer.builder()
                        .attempt(savedAttempt)
                        .questionId(question.getId())
                        .questionText(question.getQuestionText())
                        .studentAnswer(null)
                        .isCorrect(false)
                        .correctAnswer(null)
                        .build())
                .toList();

        answerRepository.saveAll(blankAnswers);
        return toAttemptResponse(savedAttempt, false);
    }

    @Transactional
    public MockTestAttemptResponse saveAnswer(Long testId, Long questionId, MockTestSaveAnswerRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertStudentAccess(currentUser, centerId);

        MockTestAttempt attempt = findCurrentAttempt(currentUser.getId(), testId, centerId);
        MockTestQuestion question = findQuestionInTest(testId, questionId, centerId);
        upsertAnswer(attempt, question, request.getAnswer());
        return toAttemptResponse(attempt, false);
    }

    @Transactional
    public MockTestAttemptResponse submitTest(Long testId, MockTestSubmitRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertStudentAccess(currentUser, centerId);

        MockTestAttempt attempt = findCurrentAttempt(currentUser.getId(), testId, centerId);
        MockTest mockTest = attempt.getMockTest();
        List<MockTestQuestion> questions = questionRepository.findAllByMockTest_IdOrderBySortOrderAscIdAsc(mockTest.getId());
        Map<Long, MockTestSubmitAnswerRequest> submittedAnswers = (request.getAnswers() == null ? List.<MockTestSubmitAnswerRequest>of() : request.getAnswers())
                .stream()
                .filter(item -> item.getQuestionId() != null)
                .collect(Collectors.toMap(
                        MockTestSubmitAnswerRequest::getQuestionId,
                        item -> item,
                        (left, right) -> right
                ));

        int correctAnswers = 0;
        for (MockTestQuestion question : questions) {
            MockTestSubmitAnswerRequest submitted = submittedAnswers.get(question.getId());
            String studentAnswer = submitted == null ? null : trimToNull(submitted.getAnswer());
            MockTestAttemptAnswer answer = upsertAnswer(attempt, question, studentAnswer);
            boolean isCorrect = studentAnswer != null && question.getCorrectAnswer().equalsIgnoreCase(studentAnswer);
            answer.setIsCorrect(isCorrect);
            answer.setCorrectAnswer(question.getCorrectAnswer());
            answerRepository.save(answer);
            if (isCorrect) {
                correctAnswers++;
            }
        }

        attempt.setCorrectAnswers(correctAnswers);
        attempt.setScore(correctAnswers);
        attempt.setMaxScore(questions.size());
        attempt.setTotalQuestions(questions.size());
        attempt.setStatus(MockTestAttemptStatus.COMPLETED);
        attempt.setCompletedAt(Instant.now());
        attemptRepository.save(attempt);

        return toAttemptResponse(attempt, true);
    }

    @Transactional(readOnly = true)
    public MockTestAttemptResponse getAttemptForStudent(Long attemptId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertStudentAccess(currentUser, centerId);

        MockTestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found with id: " + attemptId));

        if (!attempt.getStudentUser().getId().equals(currentUser.getId()) || !attempt.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Attempt " + attemptId + " does not belong to current center");
        }

        return toAttemptResponse(attempt, true);
    }

    @Transactional(readOnly = true)
    public MockTestAttemptResponse getAttemptForOwner(Long attemptId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertOwnerAndCenterAccess(currentUser, centerId);

        MockTestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found with id: " + attemptId));

        if (!attempt.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Attempt " + attemptId + " belongs to another center");
        }

        return toAttemptResponse(attempt, true);
    }

    @Transactional(readOnly = true)
    public List<MockTestAttemptResponse> getAttemptsForTeacher(Long classId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertTeacherAccess(currentUser, centerId);

        List<Long> classIds = getTeacherClassIds(currentUser.getId(), centerId);
        if (classId != null) {
            if (!classIds.contains(classId)) {
                throw new AccessDeniedException("You do not have permission to view this class");
            }
            classIds = List.of(classId);
        }

        List<Long> studentIds = getActiveStudentIdsInClasses(classIds);
        if (studentIds.isEmpty()) {
            return List.of();
        }

        return attemptRepository
                .findAllByStudentUser_IdInAndCenter_IdAndStatusOrderByStartedAtDesc(
                        studentIds,
                        centerId,
                        MockTestAttemptStatus.COMPLETED
                )
                .stream()
                .map(attempt -> toAttemptResponse(attempt, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public MockTestAttemptResponse getAttemptForTeacher(Long attemptId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertTeacherAccess(currentUser, centerId);

        MockTestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found with id: " + attemptId));

        if (!attempt.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Attempt " + attemptId + " belongs to another center");
        }

        List<Long> classIds = getTeacherClassIds(currentUser.getId(), centerId);
        Set<Long> studentIds = Set.copyOf(getActiveStudentIdsInClasses(classIds));
        if (!studentIds.contains(attempt.getStudentUser().getId())) {
            throw new AccessDeniedException("You do not have permission to view this attempt");
        }

        return toAttemptResponse(attempt, true);
    }

    private MockTestAttemptAnswer upsertAnswer(MockTestAttempt attempt, MockTestQuestion question, String answerValue) {
        MockTestAttemptAnswer answer = answerRepository.findByAttempt_IdAndQuestionId(attempt.getId(), question.getId())
                .orElseGet(() -> MockTestAttemptAnswer.builder()
                        .attempt(attempt)
                        .questionId(question.getId())
                        .questionText(question.getQuestionText())
                        .isCorrect(false)
                        .correctAnswer(null)
                        .build());

        answer.setStudentAnswer(trimToNull(answerValue));
        answer.setQuestionText(question.getQuestionText());
        if (answer.getCorrectAnswer() == null || answer.getCorrectAnswer().isBlank()) {
            answer.setCorrectAnswer(question.getCorrectAnswer());
        }
        answer.setIsCorrect(answer.getStudentAnswer() != null
                && question.getCorrectAnswer().equalsIgnoreCase(answer.getStudentAnswer()));
        return answerRepository.save(answer);
    }

    private MockTestQuestion findQuestionInTest(Long testId, Long questionId, Long centerId) {
        MockTestQuestion question = questionRepository.findByIdAndMockTest_Id(questionId, testId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));
        if (!question.getMockTest().getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Question " + questionId + " belongs to another center");
        }
        return question;
    }

    private MockTestQuestion findOwnedQuestion(Long testId, Long questionId, Long centerId) {
        MockTestQuestion question = findQuestionInTest(testId, questionId, centerId);
        assertOwnedTest(question.getMockTest(), centerId);
        return question;
    }

    private MockTest findOwnedTest(Long testId, Long centerId) {
        MockTest mockTest = mockTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Mock test not found with id: " + testId));
        assertOwnedTest(mockTest, centerId);
        return mockTest;
    }

    private MockTest findActiveTestInCenter(Long testId, Long centerId) {
        MockTest mockTest = mockTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Mock test not found with id: " + testId));
        if (!mockTest.getCenter().getId().equals(centerId) || !Boolean.TRUE.equals(mockTest.getIsActive())) {
            throw new TenancyViolationException("Mock test " + testId + " belongs to another center");
        }
        return mockTest;
    }

    private MockTestAttempt findCurrentAttempt(Long studentUserId, Long testId, Long centerId) {
        return attemptRepository
                .findTopByStudentUser_IdAndMockTest_IdAndStatusOrderByStartedAtDesc(studentUserId, testId, MockTestAttemptStatus.IN_PROGRESS)
                .orElseThrow(() -> new BadRequestException("Please start the test first"));
    }

    private MockTestResponse toResponse(MockTest test, boolean includeCounts) {
        int questionCount = test.getQuestions() == null ? 0 : test.getQuestions().size();
        int attemptCount = 0;
        if (includeCounts) {
            attemptCount = attemptRepository.findAllByMockTest_IdOrderByStartedAtDesc(test.getId()).size();
        }

        return MockTestResponse.builder()
                .id(test.getId())
                .title(test.getTitle())
                .description(test.getDescription())
                .level(test.getLevel())
                .duration(test.getDuration())
                .totalQuestions(test.getTotalQuestions())
                .createdAt(test.getCreatedAt())
                .isActive(test.getIsActive())
                .questionCount(questionCount)
                .attemptCount(attemptCount)
                .build();
    }

    private MockTestQuestionResponse toQuestionResponse(MockTestQuestion question) {
        return toQuestionResponse(question, true);
    }

    private MockTestQuestionResponse toQuestionResponse(MockTestQuestion question, boolean includeCorrectAnswer) {
        return MockTestQuestionResponse.builder()
                .id(question.getId())
                .testId(question.getMockTest().getId())
                .questionText(question.getQuestionText())
                .optionA(question.getOptionA())
                .optionB(question.getOptionB())
                .optionC(question.getOptionC())
                .optionD(question.getOptionD())
                .correctAnswer(includeCorrectAnswer ? question.getCorrectAnswer() : null)
                .explanation(question.getExplanation())
                .sortOrder(question.getSortOrder())
                .build();
    }

    private MockTestAttemptResponse toAttemptResponse(MockTestAttempt attempt, boolean includeCorrectAnswer) {
        List<MockTestAttemptAnswerResponse> answers = answerRepository.findAllByAttempt_IdOrderByQuestionIdAsc(attempt.getId())
                .stream()
                .map(answer -> MockTestAttemptAnswerResponse.builder()
                        .questionId(answer.getQuestionId())
                        .questionText(answer.getQuestionText())
                        .studentAnswer(answer.getStudentAnswer())
                        .isCorrect(answer.getIsCorrect())
                        .correctAnswer(includeCorrectAnswer ? answer.getCorrectAnswer() : null)
                        .build())
                .toList();

        return MockTestAttemptResponse.builder()
                .id(attempt.getId())
                .studentId(attempt.getStudentUser().getId())
                .studentFullName(attempt.getStudentUser().getFullName())
                .testId(attempt.getMockTest().getId())
                .testTitle(attempt.getTestTitleSnapshot())
                .durationMinutes(attempt.getMockTest().getDuration())
                .score(attempt.getScore())
                .maxScore(attempt.getMaxScore())
                .correctAnswers(attempt.getCorrectAnswers())
                .totalQuestions(attempt.getTotalQuestions())
                .startedAt(attempt.getStartedAt())
                .completedAt(attempt.getCompletedAt())
                .status(attempt.getStatus())
                .answers(answers)
                .build();
    }

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new AccessDeniedException("User is not authenticated");
        }

        return userRepository.findByPhoneNumber(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private Long requiredCurrentCenterId() {
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new BadRequestException("Missing X-Tenant-ID header");
        }
        return centerId;
    }

    private void assertOwnerAndCenterAccess(User currentUser, Long centerId) {
        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can manage mock tests");
        }
        assertCenterAccess(currentUser, centerId);
    }

    private void assertStudentAccess(User currentUser, Long centerId) {
        if (currentUser.getRole() != Role.STUDENT) {
            throw new AccessDeniedException("Only STUDENT can access this resource");
        }
        assertCenterAccess(currentUser, centerId);
    }

    private void assertTeacherAccess(User currentUser, Long centerId) {
        if (currentUser.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("Only TEACHER can access this resource");
        }
        assertCenterAccess(currentUser, centerId);
    }

    private void assertCenterAccess(User currentUser, Long centerId) {
        boolean isMember = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
        boolean isCenterOwner = centerRepository.findById(centerId)
                .map(center -> center.getOwner() != null && Objects.equals(center.getOwner().getId(), currentUser.getId()))
                .orElse(false);
        if (!isMember && !isCenterOwner) {
            throw new AccessDeniedException("User is not allowed to access this center");
        }
    }

    private void assertOwnedTest(MockTest test, Long centerId) {
        if (!test.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Mock test " + test.getId() + " belongs to another center");
        }
    }

    private List<Long> getTeacherClassIds(Long teacherUserId, Long centerId) {
        return scheduleRepository.findAllByTeacherUser_IdAndCenter_Id(teacherUserId, centerId)
                .stream()
                .map(schedule -> schedule.getClazz().getId())
                .distinct()
                .toList();
    }

    private List<Long> getActiveStudentIdsInClasses(List<Long> classIds) {
        return classIds.stream()
                .flatMap(classId -> classEnrollmentRepository
                        .findAllByClazz_IdAndStatus(classId, EnrollmentStatus.ACTIVE)
                        .stream())
                .map(ClassEnrollment::getStudentUser)
                .map(User::getId)
                .distinct()
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
