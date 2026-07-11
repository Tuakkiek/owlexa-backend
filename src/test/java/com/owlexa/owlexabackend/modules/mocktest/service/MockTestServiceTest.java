package com.owlexa.owlexabackend.modules.mocktest.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.mocktest.dto.request.MockTestRequest;
import com.owlexa.owlexabackend.modules.mocktest.dto.request.MockTestSubmitAnswerRequest;
import com.owlexa.owlexabackend.modules.mocktest.dto.request.MockTestSubmitRequest;
import com.owlexa.owlexabackend.modules.mocktest.dto.response.MockTestAttemptResponse;
import com.owlexa.owlexabackend.modules.mocktest.dto.response.MockTestResponse;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTest;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestAttempt;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestAttemptAnswer;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestAttemptStatus;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestLevel;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestQuestion;
import com.owlexa.owlexabackend.modules.mocktest.repository.MockTestAttemptAnswerRepository;
import com.owlexa.owlexabackend.modules.mocktest.repository.MockTestAttemptRepository;
import com.owlexa.owlexabackend.modules.mocktest.repository.MockTestQuestionRepository;
import com.owlexa.owlexabackend.modules.mocktest.repository.MockTestRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockTestServiceTest {

    @Mock private MockTestRepository mockTestRepository;
    @Mock private MockTestQuestionRepository questionRepository;
    @Mock private MockTestAttemptRepository attemptRepository;
    @Mock private MockTestAttemptAnswerRepository answerRepository;
    @Mock private CenterRepository centerRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private UserRepository userRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;

    private MockTestService service;

    private static final String OWNER_PHONE = "0900000001";
    private static final Long OWNER_ID = 1L;
    private static final Long CENTER_ID = 10L;
    private static final Long OTHER_CENTER_ID = 99L;
    private static final Long MOCK_TEST_ID = 50L;

    @BeforeEach
    void setUp() {
        service = new MockTestService(
                mockTestRepository, questionRepository, attemptRepository, answerRepository,
                centerRepository, membershipRepository, userRepository, scheduleRepository,
                classEnrollmentRepository
        );
        TenantContext.setCurrentTenantId(CENTER_ID);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(OWNER_PHONE, null, List.of())
        );

        User owner = new User();
        owner.setId(OWNER_ID);
        owner.setPhoneNumber(OWNER_PHONE);
        owner.setRole(Role.OWNER);
        lenient().when(userRepository.findByPhoneNumber(OWNER_PHONE)).thenReturn(Optional.of(owner));
        lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(OWNER_ID, CENTER_ID)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private Center buildCenter(Long id) {
        Center center = new Center();
        center.setId(id);
        return center;
    }

    private MockTest buildMockTest(Long centerId) {
        MockTest mockTest = new MockTest();
        mockTest.setId(MOCK_TEST_ID);
        mockTest.setCenter(buildCenter(centerId));
        mockTest.setTitle("VSTEP B1 Mock Test");
        mockTest.setLevel(MockTestLevel.INTERMEDIATE);
        mockTest.setDuration(60);
        mockTest.setTotalQuestions(40);
        mockTest.setActive(true);
        return mockTest;
    }

    private MockTestRequest buildCreateRequest() {
        MockTestRequest req = new MockTestRequest();
        req.setTitle("VSTEP B1 Mock Test");
        req.setLevel(MockTestLevel.INTERMEDIATE);
        req.setDuration(60);
        req.setTotalQuestions(40);
        req.setIsActive(true);
        return req;
    }

    @Test
    @DisplayName("createTest: OWNER + hợp lệ → tạo mock test")
    void createTest_whenValid_shouldCreateMockTest() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter(CENTER_ID)));
        when(mockTestRepository.save(any(MockTest.class))).thenAnswer(invocation -> {
            MockTest m = invocation.getArgument(0);
            m.setId(MOCK_TEST_ID);
            return m;
        });

        MockTestResponse response = service.createTest(buildCreateRequest());

        assertThat(response.getId()).isEqualTo(MOCK_TEST_ID);
        assertThat(response.getTitle()).isEqualTo("VSTEP B1 Mock Test");
    }

    @Test
    @DisplayName("createTest: caller không phải OWNER → AccessDeniedException")
    void createTest_whenCallerIsNotOwner_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("teacher-x", null, List.of())
        );
        User teacher = new User();
        teacher.setId(2L);
        teacher.setPhoneNumber("teacher-x");
        teacher.setRole(Role.TEACHER);
        when(userRepository.findByPhoneNumber("teacher-x")).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.createTest(buildCreateRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getOwnerTests: trả về danh sách mock test trong center")
    void getOwnerTests_shouldReturnMockTests() {
        when(mockTestRepository.findAllByCenter_IdOrderByCreatedAtDesc(CENTER_ID))
                .thenReturn(List.of(buildMockTest(CENTER_ID), buildMockTest(CENTER_ID)));

        List<MockTestResponse> response = service.getOwnerTests();

        assertThat(response).hasSize(2);
    }

    @Test
    @DisplayName("getOwnerTests: caller không phải OWNER → AccessDeniedException")
    void getOwnerTests_whenCallerIsNotOwner_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("teacher-x", null, List.of())
        );
        User teacher = new User();
        teacher.setId(2L);
        teacher.setPhoneNumber("teacher-x");
        teacher.setRole(Role.TEACHER);
        when(userRepository.findByPhoneNumber("teacher-x")).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.getOwnerTests())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("updateTest: mock test thuộc center khác → TenancyViolationException")
    void updateTest_whenInOtherCenter_shouldThrowTenancyViolation() {
        when(mockTestRepository.findById(MOCK_TEST_ID)).thenReturn(Optional.of(buildMockTest(OTHER_CENTER_ID)));

        assertThatThrownBy(() -> service.updateTest(MOCK_TEST_ID, buildCreateRequest()))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("updateTest: hợp lệ → cập nhật")
    void updateTest_whenValid_shouldUpdate() {
        when(mockTestRepository.findById(MOCK_TEST_ID)).thenReturn(Optional.of(buildMockTest(CENTER_ID)));
        when(mockTestRepository.save(any(MockTest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockTestRequest req = new MockTestRequest();
        req.setTitle("Updated Title");
        req.setLevel(MockTestLevel.ADVANCED);
        req.setDuration(90);
        req.setTotalQuestions(50);
        req.setIsActive(false);

        MockTestResponse response = service.updateTest(MOCK_TEST_ID, req);

        assertThat(response.getTitle()).isEqualTo("Updated Title");
        assertThat(response.getLevel()).isEqualTo(MockTestLevel.ADVANCED);
    }

    @Test
    @DisplayName("deleteTest: mock test thuộc center khác → TenancyViolationException")
    void deleteTest_whenInOtherCenter_shouldThrowTenancyViolation() {
        when(mockTestRepository.findById(MOCK_TEST_ID)).thenReturn(Optional.of(buildMockTest(OTHER_CENTER_ID)));

        assertThatThrownBy(() -> service.deleteTest(MOCK_TEST_ID))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("deleteTest: hợp lệ → soft-delete (set isActive=false)")
    void deleteTest_whenValid_shouldSoftDelete() throws Exception {
        MockTest existing = buildMockTest(CENTER_ID);
        when(mockTestRepository.findById(MOCK_TEST_ID)).thenReturn(Optional.of(existing));

        org.mockito.ArgumentCaptor<MockTest> captor =
                org.mockito.ArgumentCaptor.forClass(MockTest.class);
        when(mockTestRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteTest(MOCK_TEST_ID);

        MockTest saved = captor.getValue();
        java.lang.reflect.Field f = MockTest.class.getDeclaredField("isActive");
        f.setAccessible(true);
        assertThat((boolean) f.getBoolean(saved)).isFalse();
    }

    @Test
    @DisplayName("deleteTest: mock test không tồn tại → ResourceNotFoundException")
    void deleteTest_whenNotFound_shouldThrowResourceNotFound() {
        when(mockTestRepository.findById(MOCK_TEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteTest(MOCK_TEST_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createTest: TenantContext null → BadRequestException")
    void createTest_whenTenantContextIsNull_shouldThrowBadRequest() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.createTest(buildCreateRequest()))
                .isInstanceOf(BadRequestException.class);
    }

    // ─────────────────────────────────────────────────────────────────
    // submitTest tests
    // ─────────────────────────────────────────────────────────────────

    private static final String STUDENT_PHONE = "0900000099";
    private static final Long STUDENT_ID = 99L;

    private void loginAsStudent() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(STUDENT_PHONE, null, List.of())
        );
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber(STUDENT_PHONE);
        student.setRole(Role.STUDENT);
        student.setFullName("Test Student");
        lenient().when(userRepository.findByPhoneNumber(STUDENT_PHONE)).thenReturn(Optional.of(student));
        lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(STUDENT_ID, CENTER_ID)).thenReturn(true);
    }

    private MockTestAttempt buildInProgressAttempt(Long id, MockTest mockTest, User student) {
        return MockTestAttempt.builder()
                .id(id)
                .mockTest(mockTest)
                .studentUser(student)
                .center(mockTest.getCenter())
                .status(MockTestAttemptStatus.IN_PROGRESS)
                .score(0)
                .maxScore(0)
                .correctAnswers(0)
                .totalQuestions(0)
                .testTitleSnapshot(mockTest.getTitle())
                .startedAt(Instant.now())
                .build();
    }

    private MockTestQuestion buildQuestion(Long id, MockTest mockTest, String correct, int order) {
        return MockTestQuestion.builder()
                .id(id)
                .mockTest(mockTest)
                .questionText("Q" + order)
                .optionA("A")
                .optionB("B")
                .optionC("C")
                .optionD("D")
                .correctAnswer(correct)
                .sortOrder(order)
                .build();
    }

    private MockTestSubmitAnswerRequest buildAnswer(Long questionId, String choice) {
        MockTestSubmitAnswerRequest answer = new MockTestSubmitAnswerRequest();
        answer.setQuestionId(questionId);
        answer.setAnswer(choice);
        return answer;
    }

    @Test
    @DisplayName("submitTest: caller không phải STUDENT → AccessDeniedException")
    void submitTest_whenCallerIsNotStudent_shouldThrowAccessDenied() {
        // Default setUp login as OWNER
        assertThatThrownBy(() -> service.submitTest(MOCK_TEST_ID, new MockTestSubmitRequest()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only STUDENT");
    }

    @Test
    @DisplayName("submitTest: student chưa start (không có attempt IN_PROGRESS) → BadRequestException 'Please start the test first'")
    void submitTest_whenNoInProgressAttempt_shouldThrowBadRequest() {
        loginAsStudent();

        when(attemptRepository.findTopByStudentUser_IdAndMockTest_IdAndStatusOrderByStartedAtDesc(
                STUDENT_ID, MOCK_TEST_ID, MockTestAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitTest(MOCK_TEST_ID, new MockTestSubmitRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Please start the test first");
    }

    @Test
    @DisplayName("submitTest: happy path - 2 đáp án đúng + 1 sai + 1 không trả lời → score = 2/4")
    void submitTest_whenMixedAnswers_shouldComputeCorrectScore() {
        loginAsStudent();
        MockTest mockTest = buildMockTest(CENTER_ID);
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber(STUDENT_PHONE);
        student.setRole(Role.STUDENT);
        student.setFullName("Test Student");

        MockTestAttempt attempt = buildInProgressAttempt(1L, mockTest, student);
        MockTestQuestion q1 = buildQuestion(101L, mockTest, "A", 1);
        MockTestQuestion q2 = buildQuestion(102L, mockTest, "B", 2);
        MockTestQuestion q3 = buildQuestion(103L, mockTest, "C", 3);
        MockTestQuestion q4 = buildQuestion(104L, mockTest, "D", 4);

        when(attemptRepository.findTopByStudentUser_IdAndMockTest_IdAndStatusOrderByStartedAtDesc(
                STUDENT_ID, MOCK_TEST_ID, MockTestAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));
        when(questionRepository.findAllByMockTest_IdOrderBySortOrderAscIdAsc(MOCK_TEST_ID))
                .thenReturn(List.of(q1, q2, q3, q4));
        when(answerRepository.findByAttempt_IdAndQuestionId(any(), any()))
                .thenReturn(Optional.empty());
        when(answerRepository.save(any(MockTestAttemptAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(attemptRepository.save(any(MockTestAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MockTestSubmitRequest request = new MockTestSubmitRequest();
        request.setTestId(MOCK_TEST_ID);
        request.setAnswers(List.of(
                buildAnswer(101L, "A"), // correct
                buildAnswer(102L, "D"), // wrong
                buildAnswer(103L, "C"), // correct
                buildAnswer(104L, null) // not answered
        ));

        MockTestAttemptResponse response = service.submitTest(MOCK_TEST_ID, request);

        assertThat(response.getScore()).isEqualTo(2);
        assertThat(response.getMaxScore()).isEqualTo(4);
        assertThat(response.getCorrectAnswers()).isEqualTo(2);
        assertThat(response.getTotalQuestions()).isEqualTo(4);
        assertThat(response.getStatus()).isEqualTo(MockTestAttemptStatus.COMPLETED);
        assertThat(response.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("submitTest: tất cả đáp án đúng → score = maxScore")
    void submitTest_whenAllCorrect_shouldScoreFull() {
        loginAsStudent();
        MockTest mockTest = buildMockTest(CENTER_ID);
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber(STUDENT_PHONE);
        student.setRole(Role.STUDENT);
        student.setFullName("Test Student");

        MockTestAttempt attempt = buildInProgressAttempt(2L, mockTest, student);
        MockTestQuestion q1 = buildQuestion(101L, mockTest, "A", 1);
        MockTestQuestion q2 = buildQuestion(102L, mockTest, "B", 2);

        when(attemptRepository.findTopByStudentUser_IdAndMockTest_IdAndStatusOrderByStartedAtDesc(
                STUDENT_ID, MOCK_TEST_ID, MockTestAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));
        when(questionRepository.findAllByMockTest_IdOrderBySortOrderAscIdAsc(MOCK_TEST_ID))
                .thenReturn(List.of(q1, q2));
        when(answerRepository.findByAttempt_IdAndQuestionId(any(), any()))
                .thenReturn(Optional.empty());
        when(answerRepository.save(any(MockTestAttemptAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(attemptRepository.save(any(MockTestAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MockTestSubmitRequest request = new MockTestSubmitRequest();
        request.setTestId(MOCK_TEST_ID);
        request.setAnswers(List.of(buildAnswer(101L, "A"), buildAnswer(102L, "B")));

        MockTestAttemptResponse response = service.submitTest(MOCK_TEST_ID, request);

        assertThat(response.getScore()).isEqualTo(2);
        assertThat(response.getMaxScore()).isEqualTo(2);
    }

    @Test
    @DisplayName("submitTest: empty answers → score = 0, status = COMPLETED")
    void submitTest_whenEmptyAnswers_shouldScoreZero() {
        loginAsStudent();
        MockTest mockTest = buildMockTest(CENTER_ID);
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber(STUDENT_PHONE);
        student.setRole(Role.STUDENT);
        student.setFullName("Test Student");

        MockTestAttempt attempt = buildInProgressAttempt(3L, mockTest, student);
        MockTestQuestion q1 = buildQuestion(101L, mockTest, "A", 1);
        MockTestQuestion q2 = buildQuestion(102L, mockTest, "B", 2);

        when(attemptRepository.findTopByStudentUser_IdAndMockTest_IdAndStatusOrderByStartedAtDesc(
                STUDENT_ID, MOCK_TEST_ID, MockTestAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));
        when(questionRepository.findAllByMockTest_IdOrderBySortOrderAscIdAsc(MOCK_TEST_ID))
                .thenReturn(List.of(q1, q2));
        when(answerRepository.findByAttempt_IdAndQuestionId(any(), any()))
                .thenReturn(Optional.empty());
        when(answerRepository.save(any(MockTestAttemptAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(attemptRepository.save(any(MockTestAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MockTestSubmitRequest request = new MockTestSubmitRequest();
        request.setTestId(MOCK_TEST_ID);
        request.setAnswers(List.of());

        MockTestAttemptResponse response = service.submitTest(MOCK_TEST_ID, request);

        assertThat(response.getScore()).isEqualTo(0);
        assertThat(response.getCorrectAnswers()).isEqualTo(0);
        assertThat(response.getStatus()).isEqualTo(MockTestAttemptStatus.COMPLETED);
    }

    @Test
    @DisplayName("submitTest: case-insensitive - 'a' matches correct answer 'A'")
    void submitTest_whenAnswerCaseInsensitive_shouldMatch() {
        loginAsStudent();
        MockTest mockTest = buildMockTest(CENTER_ID);
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber(STUDENT_PHONE);
        student.setRole(Role.STUDENT);
        student.setFullName("Test Student");

        MockTestAttempt attempt = buildInProgressAttempt(4L, mockTest, student);
        MockTestQuestion q1 = buildQuestion(101L, mockTest, "A", 1);

        when(attemptRepository.findTopByStudentUser_IdAndMockTest_IdAndStatusOrderByStartedAtDesc(
                STUDENT_ID, MOCK_TEST_ID, MockTestAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));
        when(questionRepository.findAllByMockTest_IdOrderBySortOrderAscIdAsc(MOCK_TEST_ID))
                .thenReturn(List.of(q1));
        when(answerRepository.findByAttempt_IdAndQuestionId(any(), any()))
                .thenReturn(Optional.empty());
        when(answerRepository.save(any(MockTestAttemptAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(attemptRepository.save(any(MockTestAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MockTestSubmitRequest request = new MockTestSubmitRequest();
        request.setTestId(MOCK_TEST_ID);
        request.setAnswers(List.of(buildAnswer(101L, "a"))); // lowercase

        MockTestAttemptResponse response = service.submitTest(MOCK_TEST_ID, request);

        assertThat(response.getScore()).isEqualTo(1);
        assertThat(response.getCorrectAnswers()).isEqualTo(1);
    }

    @Test
    @DisplayName("submitTest: questionId không tồn tại trong submit map → đáp án = null → không đúng")
    void submitTest_whenAnswerForUnknownQuestion_shouldNotContributeToScore() {
        loginAsStudent();
        MockTest mockTest = buildMockTest(CENTER_ID);
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber(STUDENT_PHONE);
        student.setRole(Role.STUDENT);
        student.setFullName("Test Student");

        MockTestAttempt attempt = buildInProgressAttempt(5L, mockTest, student);
        MockTestQuestion q1 = buildQuestion(101L, mockTest, "A", 1);

        when(attemptRepository.findTopByStudentUser_IdAndMockTest_IdAndStatusOrderByStartedAtDesc(
                STUDENT_ID, MOCK_TEST_ID, MockTestAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));
        when(questionRepository.findAllByMockTest_IdOrderBySortOrderAscIdAsc(MOCK_TEST_ID))
                .thenReturn(List.of(q1));
        when(answerRepository.findByAttempt_IdAndQuestionId(any(), any()))
                .thenReturn(Optional.empty());
        when(answerRepository.save(any(MockTestAttemptAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(attemptRepository.save(any(MockTestAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MockTestSubmitRequest request = new MockTestSubmitRequest();
        request.setTestId(MOCK_TEST_ID);
        // answer cho questionId 999 (không có trong test)
        request.setAnswers(List.of(buildAnswer(999L, "A")));

        MockTestAttemptResponse response = service.submitTest(MOCK_TEST_ID, request);

        assertThat(response.getScore()).isEqualTo(0);
        assertThat(response.getTotalQuestions()).isEqualTo(1); // maxScore from questions list, not from answers
    }

    @Test
    @DisplayName("submitTest: answers null → không ném, process rỗng")
    void submitTest_whenAnswersNull_shouldNotThrow() {
        loginAsStudent();
        MockTest mockTest = buildMockTest(CENTER_ID);
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber(STUDENT_PHONE);
        student.setRole(Role.STUDENT);
        student.setFullName("Test Student");

        MockTestAttempt attempt = buildInProgressAttempt(6L, mockTest, student);
        MockTestQuestion q1 = buildQuestion(101L, mockTest, "A", 1);

        when(attemptRepository.findTopByStudentUser_IdAndMockTest_IdAndStatusOrderByStartedAtDesc(
                STUDENT_ID, MOCK_TEST_ID, MockTestAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));
        when(questionRepository.findAllByMockTest_IdOrderBySortOrderAscIdAsc(MOCK_TEST_ID))
                .thenReturn(List.of(q1));
        when(answerRepository.findByAttempt_IdAndQuestionId(any(), any()))
                .thenReturn(Optional.empty());
        when(answerRepository.save(any(MockTestAttemptAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(attemptRepository.save(any(MockTestAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MockTestSubmitRequest request = new MockTestSubmitRequest();
        request.setTestId(MOCK_TEST_ID);
        request.setAnswers(null);

        MockTestAttemptResponse response = service.submitTest(MOCK_TEST_ID, request);

        assertThat(response.getScore()).isEqualTo(0);
    }

    @Test
    @DisplayName("submitTest: answer với questionId null → bị filter ra khỏi map")
    void submitTest_whenAnswerHasNullQuestionId_shouldBeFilteredOut() {
        loginAsStudent();
        MockTest mockTest = buildMockTest(CENTER_ID);
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber(STUDENT_PHONE);
        student.setRole(Role.STUDENT);
        student.setFullName("Test Student");

        MockTestAttempt attempt = buildInProgressAttempt(7L, mockTest, student);
        MockTestQuestion q1 = buildQuestion(101L, mockTest, "A", 1);

        when(attemptRepository.findTopByStudentUser_IdAndMockTest_IdAndStatusOrderByStartedAtDesc(
                STUDENT_ID, MOCK_TEST_ID, MockTestAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));
        when(questionRepository.findAllByMockTest_IdOrderBySortOrderAscIdAsc(MOCK_TEST_ID))
                .thenReturn(List.of(q1));
        when(answerRepository.findByAttempt_IdAndQuestionId(any(), any()))
                .thenReturn(Optional.empty());
        when(answerRepository.save(any(MockTestAttemptAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(attemptRepository.save(any(MockTestAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MockTestSubmitAnswerRequest badAnswer = new MockTestSubmitAnswerRequest();
        badAnswer.setQuestionId(null);
        badAnswer.setAnswer("A");

        MockTestSubmitRequest request = new MockTestSubmitRequest();
        request.setTestId(MOCK_TEST_ID);
        request.setAnswers(List.of(badAnswer));

        MockTestAttemptResponse response = service.submitTest(MOCK_TEST_ID, request);

        assertThat(response.getScore()).isEqualTo(0); // null questionId → bị filter → q1 không có answer
    }

    @Test
    @DisplayName("submitTest: completedAt được set khi submit")
    void submitTest_shouldSetCompletedAtTimestamp() {
        loginAsStudent();
        MockTest mockTest = buildMockTest(CENTER_ID);
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber(STUDENT_PHONE);
        student.setRole(Role.STUDENT);
        student.setFullName("Test Student");

        MockTestAttempt attempt = buildInProgressAttempt(8L, mockTest, student);
        MockTestQuestion q1 = buildQuestion(101L, mockTest, "A", 1);

        when(attemptRepository.findTopByStudentUser_IdAndMockTest_IdAndStatusOrderByStartedAtDesc(
                STUDENT_ID, MOCK_TEST_ID, MockTestAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));
        when(questionRepository.findAllByMockTest_IdOrderBySortOrderAscIdAsc(MOCK_TEST_ID))
                .thenReturn(List.of(q1));
        when(answerRepository.findByAttempt_IdAndQuestionId(any(), any()))
                .thenReturn(Optional.empty());
        when(answerRepository.save(any(MockTestAttemptAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(attemptRepository.save(any(MockTestAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MockTestSubmitRequest request = new MockTestSubmitRequest();
        request.setTestId(MOCK_TEST_ID);
        request.setAnswers(List.of(buildAnswer(101L, "A")));

        Instant before = Instant.now();
        MockTestAttemptResponse response = service.submitTest(MOCK_TEST_ID, request);
        Instant after = Instant.now();

        assertThat(response.getCompletedAt()).isNotNull();
        assertThat(response.getCompletedAt()).isBetween(before.minusSeconds(1), after.plusSeconds(1));
    }

    @Test
    @DisplayName("submitTest: status attempt được set COMPLETED và save được gọi")
    void submitTest_shouldUpdateAttemptStatusAndSave() {
        loginAsStudent();
        MockTest mockTest = buildMockTest(CENTER_ID);
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber(STUDENT_PHONE);
        student.setRole(Role.STUDENT);
        student.setFullName("Test Student");

        MockTestAttempt attempt = buildInProgressAttempt(9L, mockTest, student);
        MockTestQuestion q1 = buildQuestion(101L, mockTest, "A", 1);

        when(attemptRepository.findTopByStudentUser_IdAndMockTest_IdAndStatusOrderByStartedAtDesc(
                STUDENT_ID, MOCK_TEST_ID, MockTestAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));
        when(questionRepository.findAllByMockTest_IdOrderBySortOrderAscIdAsc(MOCK_TEST_ID))
                .thenReturn(List.of(q1));
        when(answerRepository.findByAttempt_IdAndQuestionId(any(), any()))
                .thenReturn(Optional.empty());
        when(answerRepository.save(any(MockTestAttemptAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<MockTestAttempt> attemptCaptor = ArgumentCaptor.forClass(MockTestAttempt.class);
        when(attemptRepository.save(attemptCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MockTestSubmitRequest request = new MockTestSubmitRequest();
        request.setTestId(MOCK_TEST_ID);
        request.setAnswers(List.of(buildAnswer(101L, "A")));

        service.submitTest(MOCK_TEST_ID, request);

        MockTestAttempt savedAttempt = attemptCaptor.getValue();
        assertThat(savedAttempt.getStatus()).isEqualTo(MockTestAttemptStatus.COMPLETED);
        assertThat(savedAttempt.getScore()).isEqualTo(1);
        assertThat(savedAttempt.getCorrectAnswers()).isEqualTo(1);
        assertThat(savedAttempt.getMaxScore()).isEqualTo(1);
        assertThat(savedAttempt.getTotalQuestions()).isEqualTo(1);
    }

    @Test
    @DisplayName("submitTest: mỗi câu hỏi được upsert 1 answer → save answer đúng số lần")
    void submitTest_shouldSaveAnswerForEachQuestion() {
        loginAsStudent();
        MockTest mockTest = buildMockTest(CENTER_ID);
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber(STUDENT_PHONE);
        student.setRole(Role.STUDENT);
        student.setFullName("Test Student");

        MockTestAttempt attempt = buildInProgressAttempt(10L, mockTest, student);
        MockTestQuestion q1 = buildQuestion(101L, mockTest, "A", 1);
        MockTestQuestion q2 = buildQuestion(102L, mockTest, "B", 2);
        MockTestQuestion q3 = buildQuestion(103L, mockTest, "C", 3);

        when(attemptRepository.findTopByStudentUser_IdAndMockTest_IdAndStatusOrderByStartedAtDesc(
                STUDENT_ID, MOCK_TEST_ID, MockTestAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));
        when(questionRepository.findAllByMockTest_IdOrderBySortOrderAscIdAsc(MOCK_TEST_ID))
                .thenReturn(List.of(q1, q2, q3));
        when(answerRepository.findByAttempt_IdAndQuestionId(any(), any()))
                .thenReturn(Optional.empty());
        when(answerRepository.save(any(MockTestAttemptAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(attemptRepository.save(any(MockTestAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MockTestSubmitRequest request = new MockTestSubmitRequest();
        request.setTestId(MOCK_TEST_ID);
        request.setAnswers(List.of(
                buildAnswer(101L, "A"),
                buildAnswer(102L, "A"), // wrong (correct=B)
                buildAnswer(103L, "C")  // correct
        ));

        service.submitTest(MOCK_TEST_ID, request);

        // each call to upsertAnswer() internally saves + explicit save after; see service line 376
        verify(answerRepository, times(6)).save(any(MockTestAttemptAnswer.class));
        verify(attemptRepository, times(1)).save(any(MockTestAttempt.class));
    }

    @Test
    @DisplayName("submitTest: existing answer (đã saveAnswer trước đó) được update, không tạo mới")
    void submitTest_whenExistingAnswer_shouldUpdateIt() {
        loginAsStudent();
        MockTest mockTest = buildMockTest(CENTER_ID);
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber(STUDENT_PHONE);
        student.setRole(Role.STUDENT);
        student.setFullName("Test Student");

        MockTestAttempt attempt = buildInProgressAttempt(11L, mockTest, student);
        MockTestQuestion q1 = buildQuestion(101L, mockTest, "A", 1);

        // Existing answer that was saved previously with wrong choice
        MockTestAttemptAnswer existingAnswer = MockTestAttemptAnswer.builder()
                .id(500L)
                .attempt(attempt)
                .questionId(101L)
                .questionText("Q1")
                .studentAnswer("B")
                .isCorrect(false)
                .correctAnswer("A")
                .build();

        when(attemptRepository.findTopByStudentUser_IdAndMockTest_IdAndStatusOrderByStartedAtDesc(
                STUDENT_ID, MOCK_TEST_ID, MockTestAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));
        when(questionRepository.findAllByMockTest_IdOrderBySortOrderAscIdAsc(MOCK_TEST_ID))
                .thenReturn(List.of(q1));
        when(answerRepository.findByAttempt_IdAndQuestionId(11L, 101L))
                .thenReturn(Optional.of(existingAnswer));
        when(answerRepository.save(any(MockTestAttemptAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(attemptRepository.save(any(MockTestAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MockTestSubmitRequest request = new MockTestSubmitRequest();
        request.setTestId(MOCK_TEST_ID);
        request.setAnswers(List.of(buildAnswer(101L, "A"))); // now correct

        MockTestAttemptResponse response = service.submitTest(MOCK_TEST_ID, request);

        // Score should reflect the correction
        assertThat(response.getScore()).isEqualTo(1);
        assertThat(response.getCorrectAnswers()).isEqualTo(1);

        // verify that existing answer was reused (same id 500L):
        // each loop iteration calls upsertAnswer (which saves once) + explicit save line 376 = 2 saves total
        verify(answerRepository, times(2)).save(any(MockTestAttemptAnswer.class));

        ArgumentCaptor<MockTestAttemptAnswer> captor = ArgumentCaptor.forClass(MockTestAttemptAnswer.class);
        verify(answerRepository, atLeastOnce()).save(captor.capture());
        MockTestAttemptAnswer savedAnswer = captor.getValue();
        assertThat(savedAnswer.getId()).isEqualTo(500L); // existing answer, not new
        assertThat(savedAnswer.getStudentAnswer()).isEqualTo("A");
        assertThat(savedAnswer.getIsCorrect()).isTrue();
    }
}