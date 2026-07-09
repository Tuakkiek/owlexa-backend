package com.owlexa.owlexabackend.modules.essay.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.essay.dto.request.EssayRubricRequest;
import com.owlexa.owlexabackend.modules.essay.dto.request.EssaySubmitRequest;
import com.owlexa.owlexabackend.modules.essay.dto.response.EssayGradingResultResponse;
import com.owlexa.owlexabackend.modules.essay.dto.response.EssayRubricResponse;
import com.owlexa.owlexabackend.modules.essay.dto.response.EssaySubmissionResponse;
import com.owlexa.owlexabackend.modules.essay.entity.EssayGradingResult;
import com.owlexa.owlexabackend.modules.essay.entity.EssayRubric;
import com.owlexa.owlexabackend.modules.essay.entity.EssaySubmission;
import com.owlexa.owlexabackend.modules.essay.entity.EssaySubmissionStatus;
import com.owlexa.owlexabackend.modules.essay.repository.EssayGradingResultRepository;
import com.owlexa.owlexabackend.modules.essay.repository.EssayRubricRepository;
import com.owlexa.owlexabackend.modules.essay.repository.EssaySubmissionRepository;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EssayServiceTest {

    @Mock private EssayRubricRepository essayRubricRepository;
    @Mock private EssaySubmissionRepository essaySubmissionRepository;
    @Mock private EssayGradingResultRepository essayGradingResultRepository;
    @Mock private ClassRepository classRepository;
    @Mock private CenterRepository centerRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private ScheduleRepository scheduleRepository;

    private EssayService service;

    private static final String TEACHER_PHONE = "0900000001";
    private static final String STUDENT_PHONE = "0900000003";
    private static final Long TEACHER_ID = 2L;
    private static final Long STUDENT_ID = 100L;
    private static final Long CENTER_ID = 10L;
    private static final Long OTHER_CENTER_ID = 99L;
    private static final Long CLASS_ID = 50L;
    private static final Long RUBRIC_ID = 500L;
    private static final Long ESSAY_ID = 1000L;

    @BeforeEach
    void setUp() {
        service = new EssayService(
                essayRubricRepository, essaySubmissionRepository, essayGradingResultRepository,
                classRepository, centerRepository, userRepository, membershipRepository,
                classEnrollmentRepository, scheduleRepository
        );
        TenantContext.setCurrentTenantId(CENTER_ID);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEACHER_PHONE, null, List.of())
        );

        User teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setPhoneNumber(TEACHER_PHONE);
        teacher.setRole(Role.TEACHER);
        lenient().when(userRepository.findByPhoneNumber(TEACHER_PHONE)).thenReturn(Optional.of(teacher));
        lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID)).thenReturn(true);
        lenient().when(scheduleRepository.findAllByTeacherUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID))
                .thenReturn(List.of(mockSchedule(CENTER_ID, CLASS_ID)));
    }

    private com.owlexa.owlexabackend.modules.class_management.entity.Schedule mockSchedule(Long centerId, Long classId) {
        com.owlexa.owlexabackend.modules.class_management.entity.Schedule s =
                new com.owlexa.owlexabackend.modules.class_management.entity.Schedule();
        Center center = new Center();
        center.setId(centerId);
        s.setCenter(center);
        Class clazz = new Class();
        clazz.setId(classId);
        clazz.setCenter(center);
        s.setClazz(clazz);
        return s;
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

    private Class buildClass(Long centerId) {
        Center center = buildCenter(centerId);
        Class clazz = new Class();
        clazz.setId(CLASS_ID);
        clazz.setName("Class A");
        clazz.setCenter(center);
        return clazz;
    }

    private User buildStudent(Long id) {
        User student = new User();
        student.setId(id);
        student.setPhoneNumber("09" + String.format("%08d", id));
        student.setFullName("Student " + id);
        student.setRole(Role.STUDENT);
        return student;
    }

    private EssayRubricRequest buildRubricRequest() {
        EssayRubricRequest req = new EssayRubricRequest();
        req.setClassId(CLASS_ID);
        req.setTitle("VSTEP Writing Task 1");
        req.setDescription("Write a 200-word essay");
        req.setMaxScore(10.0);
        req.setCriteria(List.of());
        return req;
    }

    private EssayRubric buildRubric(Long centerId) {
        EssayRubric rubric = new EssayRubric();
        rubric.setId(RUBRIC_ID);
        rubric.setCenter(buildCenter(centerId));
        rubric.setClazz(buildClass(centerId));
        rubric.setTitle("Test Rubric");
        rubric.setMaxScore(10.0);
        rubric.setActive(true);
        User creator = new User();
        creator.setId(TEACHER_ID);
        rubric.setCreatedByUser(creator);
        return rubric;
    }

    private EssaySubmission buildSubmission(Long id) {
        EssaySubmission submission = new EssaySubmission();
        submission.setId(id);
        submission.setStudentUser(buildStudent(STUDENT_ID));
        submission.setClazz(buildClass(CENTER_ID));
        submission.setRubric(buildRubric(CENTER_ID));
        submission.setCenter(buildCenter(CENTER_ID));
        submission.setContent("My essay");
        submission.setStatus(EssaySubmissionStatus.GRADED);
        submission.setSubmittedAt(Instant.now());
        return submission;
    }

    @Test
    @DisplayName("createRubric: TEACHER + class hợp lệ + teacher dạy class → tạo rubric")
    void createRubric_whenValid_shouldCreateRubric() {
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(buildClass(CENTER_ID)));
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter(CENTER_ID)));
        when(essayRubricRepository.save(any(EssayRubric.class))).thenAnswer(invocation -> {
            EssayRubric r = invocation.getArgument(0);
            r.setId(RUBRIC_ID);
            return r;
        });

        EssayRubricResponse response = service.createRubric(buildRubricRequest());

        assertThat(response.getId()).isEqualTo(RUBRIC_ID);
    }

    @Test
    @DisplayName("createRubric: class thuộc center khác → TenancyViolationException")
    void createRubric_whenClassInOtherCenter_shouldThrowTenancyViolation() {
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(buildClass(OTHER_CENTER_ID)));

        assertThatThrownBy(() -> service.createRubric(buildRubricRequest()))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("createRubric: caller không phải TEACHER → AccessDeniedException")
    void createRubric_whenCallerIsStudent_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(STUDENT_PHONE, null, List.of())
        );
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber(STUDENT_PHONE);
        student.setRole(Role.STUDENT);
        when(userRepository.findByPhoneNumber(STUDENT_PHONE)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> service.createRubric(buildRubricRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("submitEssay: STUDENT ACTIVE trong class → tạo submission")
    void submitEssay_whenStudentIsActive_shouldCreateSubmission() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(STUDENT_PHONE, null, List.of())
        );
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber(STUDENT_PHONE);
        student.setRole(Role.STUDENT);
        when(userRepository.findByPhoneNumber(STUDENT_PHONE)).thenReturn(Optional.of(student));
        when(membershipRepository.existsByUser_IdAndCenter_Id(STUDENT_ID, CENTER_ID)).thenReturn(true);

        EssayRubric rubric = buildRubric(CENTER_ID);
        when(essayRubricRepository.findById(RUBRIC_ID)).thenReturn(Optional.of(rubric));
        when(classEnrollmentRepository.existsByClazz_IdAndStudentUser_IdAndStatus(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(true);
        when(essaySubmissionRepository.save(any(EssaySubmission.class))).thenAnswer(invocation -> {
            EssaySubmission s = invocation.getArgument(0);
            s.setId(ESSAY_ID);
            s.setClazz(rubric.getClazz());
            s.setRubric(rubric);
            s.setCenter(rubric.getCenter());
            s.setStudentUser(student);
            return s;
        });

        EssaySubmitRequest req = new EssaySubmitRequest();
        req.setRubricId(RUBRIC_ID);
        req.setContent("My essay content here...");

        EssaySubmissionResponse response = service.submitEssay(req);

        assertThat(response.getId()).isEqualTo(ESSAY_ID);
        assertThat(response.getContent()).isEqualTo("My essay content here...");
    }

    @Test
    @DisplayName("submitEssay: rubric thuộc center khác → TenancyViolationException")
    void submitEssay_whenRubricInOtherCenter_shouldThrowTenancyViolation() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(STUDENT_PHONE, null, List.of())
        );
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber(STUDENT_PHONE);
        student.setRole(Role.STUDENT);
        when(userRepository.findByPhoneNumber(STUDENT_PHONE)).thenReturn(Optional.of(student));
        when(membershipRepository.existsByUser_IdAndCenter_Id(STUDENT_ID, CENTER_ID)).thenReturn(true);

        EssayRubric rubric = buildRubric(OTHER_CENTER_ID);
        when(essayRubricRepository.findById(RUBRIC_ID)).thenReturn(Optional.of(rubric));

        EssaySubmitRequest req = new EssaySubmitRequest();
        req.setRubricId(RUBRIC_ID);
        req.setContent("Content");

        assertThatThrownBy(() -> service.submitEssay(req))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("submitEssay: STUDENT không ACTIVE trong class → BusinessRuleException")
    void submitEssay_whenStudentNotActive_shouldThrowBusinessRule() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(STUDENT_PHONE, null, List.of())
        );
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber(STUDENT_PHONE);
        student.setRole(Role.STUDENT);
        when(userRepository.findByPhoneNumber(STUDENT_PHONE)).thenReturn(Optional.of(student));
        when(membershipRepository.existsByUser_IdAndCenter_Id(STUDENT_ID, CENTER_ID)).thenReturn(true);

        EssayRubric rubric = buildRubric(CENTER_ID);
        when(essayRubricRepository.findById(RUBRIC_ID)).thenReturn(Optional.of(rubric));
        when(classEnrollmentRepository.existsByClazz_IdAndStudentUser_IdAndStatus(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(false);

        EssaySubmitRequest req = new EssaySubmitRequest();
        req.setRubricId(RUBRIC_ID);
        req.setContent("Content");

        assertThatThrownBy(() -> service.submitEssay(req))
                .isInstanceOf(com.owlexa.owlexabackend.common.exception.BusinessRuleException.class);
    }

    @Test
    @DisplayName("findMyEssays: STUDENT → trả về list essays")
    void findMyEssays_shouldReturnEssays() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(STUDENT_PHONE, null, List.of())
        );
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber(STUDENT_PHONE);
        student.setRole(Role.STUDENT);
        when(userRepository.findByPhoneNumber(STUDENT_PHONE)).thenReturn(Optional.of(student));
        when(membershipRepository.existsByUser_IdAndCenter_Id(STUDENT_ID, CENTER_ID)).thenReturn(true);

        EssaySubmission s1 = buildSubmission(ESSAY_ID);
        EssaySubmission s2 = buildSubmission(ESSAY_ID + 1);
        EssaySubmission s3 = buildSubmission(ESSAY_ID + 2);
        when(essaySubmissionRepository.findAllByStudentUser_IdAndCenter_IdOrderByCreatedAtDesc(
                STUDENT_ID, CENTER_ID)).thenReturn(List.of(s1, s2, s3));

        List<EssaySubmissionResponse> response = service.findMyEssays();

        assertThat(response).hasSize(3);
    }

    @Test
    @DisplayName("getGradingResult: essay tồn tại nhưng không có grading → trả về null")
    void getGradingResult_whenNoGrading_shouldReturnNull() {
        EssaySubmission essay = buildSubmission(ESSAY_ID);
        when(essaySubmissionRepository.findById(ESSAY_ID)).thenReturn(Optional.of(essay));
        when(essayGradingResultRepository.findBySubmission_Id(ESSAY_ID))
                .thenReturn(Optional.empty());

        EssayGradingResultResponse response = service.getGradingResult(ESSAY_ID);

        assertThat(response).isNull();
    }

    @Test
    @DisplayName("getGradingResult: essay + grading tồn tại → trả về result")
    void getGradingResult_whenExists_shouldReturnResult() {
        EssaySubmission essay = buildSubmission(ESSAY_ID);
        EssayGradingResult grading = new EssayGradingResult();
        grading.setId(1L);
        grading.setSubmission(essay);
        grading.setTotalScore(8.5);
        grading.setMaxScore(10.0);
        grading.setFeedback("Good structure");

        when(essaySubmissionRepository.findById(ESSAY_ID)).thenReturn(Optional.of(essay));
        when(essayGradingResultRepository.findBySubmission_Id(ESSAY_ID)).thenReturn(Optional.of(grading));

        EssayGradingResultResponse response = service.getGradingResult(ESSAY_ID);

        assertThat(response.getTotalScore()).isEqualTo(8.5);
        assertThat(response.getFeedback()).isEqualTo("Good structure");
    }

    @Test
    @DisplayName("getGradingResult: essay không tồn tại → ResourceNotFoundException")
    void getGradingResult_whenEssayNotFound_shouldThrowResourceNotFound() {
        when(essaySubmissionRepository.findById(ESSAY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getGradingResult(ESSAY_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createRubric: TenantContext null → BadRequestException")
    void createRubric_whenTenantContextIsNull_shouldThrowBadRequest() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.createRubric(buildRubricRequest()))
                .isInstanceOf(BadRequestException.class);
    }
}