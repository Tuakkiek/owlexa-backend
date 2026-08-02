package com.owlexa.owlexabackend.modules.class_management.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.modules.assignment.repository.AssignmentRecipientRepository;
import com.owlexa.owlexabackend.modules.assignment.repository.AssignmentTargetRepository;
import com.owlexa.owlexabackend.modules.attendance.repository.AttendanceRepository;
import com.owlexa.owlexabackend.modules.class_management.dto.request.ClassRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.response.ClassResponse;
import com.owlexa.owlexabackend.modules.course.entity.Course;
import com.owlexa.owlexabackend.modules.course.repository.CourseRepository;
import com.owlexa.owlexabackend.modules.document.repository.StudentDocumentRepository;
import com.owlexa.owlexabackend.modules.payment.entity.TransactionStatus;
import com.owlexa.owlexabackend.modules.payment.repository.DiscountRepository;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.payment.repository.InstallmentRepository;
import com.owlexa.owlexabackend.modules.payment.repository.PaymentRepository;
import com.owlexa.owlexabackend.modules.teacher.dto.response.TeacherClassStudentsResponse;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleEventRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRecurringRuleRepository;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassServiceTest {

    @Mock private ClassRepository classRepository;
    @Mock private CenterRepository centerRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ScheduleEventRepository scheduleEventRepository;
    @Mock private ScheduleRecurringRuleRepository scheduleRecurringRuleRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private FeeRecordRepository feeRecordRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private InstallmentRepository installmentRepository;
    @Mock private DiscountRepository discountRepository;
    @Mock private StudentDocumentRepository studentDocumentRepository;
    @Mock private AssignmentRecipientRepository assignmentRecipientRepository;
    @Mock private AssignmentTargetRepository assignmentTargetRepository;

    private ClassService service;

    private static final String OWNER_PHONE = "0900000001";
    private static final String TEACHER_PHONE = "0900000002";
    private static final Long OWNER_ID = 1L;
    private static final Long TEACHER_ID = 2L;
    private static final Long CENTER_ID = 10L;
    private static final Long OTHER_CENTER_ID = 99L;
    private static final Long CLASS_ID = 50L;
    private static final Long COURSE_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new ClassService(
                classRepository, centerRepository, userRepository, membershipRepository,
                scheduleRepository, classEnrollmentRepository, courseRepository,
                scheduleEventRepository, scheduleRecurringRuleRepository, attendanceRepository,
                feeRecordRepository, paymentRepository, installmentRepository, discountRepository,
                studentDocumentRepository, assignmentRecipientRepository, assignmentTargetRepository
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

    private Class buildClass(Long id, Long centerId, String name, ClassStatus status) {
        Class clazz = new Class();
        clazz.setId(id);
        clazz.setName(name);
        clazz.setCenter(buildCenter(centerId));
        clazz.setMonthlyFee(1500000.0);
        clazz.setStatus(status);
        return clazz;
    }

    private Course buildCourse(Long id) {
        Course course = new Course();
        course.setId(id);
        course.setCode("VSTEP-B1");
        course.setName("VSTEP B1");
        course.setIsActive(true);
        return course;
    }

    private User buildTeacher(Long id, Long centerId) {
        User teacher = new User();
        teacher.setId(id);
        teacher.setPhoneNumber("09" + String.format("%08d", id));
        teacher.setFullName("Teacher " + id);
        teacher.setRole(Role.TEACHER);
        return teacher;
    }

    private ClassRequest buildCreateRequest() {
        return ClassRequest.builder()
                .name("VSTEP B1 Morning")
                .courseId(COURSE_ID)
                .monthlyFee(1500000.0)
                .build();
    }


    @Test
    @DisplayName("create: OWNER + tên chưa tồn tại + course hợp lệ → tạo class thành công")
    void create_whenValid_shouldCreateClass() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter(CENTER_ID)));
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(buildCourse(COURSE_ID)));
        when(classRepository.existsByNameAndCenter_Id("VSTEP B1 Morning", CENTER_ID)).thenReturn(false);
        when(classRepository.save(any(Class.class))).thenAnswer(invocation -> {
            Class c = invocation.getArgument(0);
            c.setId(CLASS_ID);
            return c;
        });

        ClassResponse response = service.create(buildCreateRequest());

        assertThat(response.getId()).isEqualTo(CLASS_ID);
        assertThat(response.getName()).isEqualTo("VSTEP B1 Morning");
        assertThat(response.getCenterId()).isEqualTo(CENTER_ID);
        assertThat(response.getStatus()).isEqualTo(ClassStatus.PLANNED);
        assertThat(response.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("create: ignores manual end date from request")
    void create_whenRequestHasEndDate_shouldIgnoreEndDate() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter(CENTER_ID)));
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(buildCourse(COURSE_ID)));
        when(classRepository.existsByNameAndCenter_Id("VSTEP B1 Morning", CENTER_ID)).thenReturn(false);
        when(classRepository.save(any(Class.class))).thenAnswer(invocation -> {
            Class c = invocation.getArgument(0);
            c.setId(CLASS_ID);
            return c;
        });

        ClassRequest request = buildCreateRequest();
        request.setStartDate(LocalDate.of(2026, 8, 1));
        request.setEndDate(LocalDate.of(2026, 8, 31));

        ClassResponse response = service.create(request);

        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(response.getEndDate()).isNull();
    }

    @Test
    @DisplayName("create: tên class đã tồn tại trong center → DuplicateResourceException")
    void create_whenNameAlreadyExistsInCenter_shouldThrowDuplicate() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter(CENTER_ID)));
        when(classRepository.existsByNameAndCenter_Id("VSTEP B1 Morning", CENTER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.create(buildCreateRequest()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("create: center không tồn tại → ResourceNotFoundException")
    void create_whenCenterNotFound_shouldThrowResourceNotFound() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(buildCreateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create: course không tồn tại → ResourceNotFoundException")
    void create_whenCourseNotFound_shouldThrowResourceNotFound() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter(CENTER_ID)));
        when(classRepository.existsByNameAndCenter_Id("VSTEP B1 Morning", CENTER_ID)).thenReturn(false);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(buildCreateRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Course not found");
    }

    @Test
    @DisplayName("create: caller không phải OWNER → AccessDeniedException")
    void create_whenCallerIsNotOwner_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEACHER_PHONE, null, List.of())
        );
        User teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setPhoneNumber(TEACHER_PHONE);
        teacher.setRole(Role.TEACHER);
        when(userRepository.findByPhoneNumber(TEACHER_PHONE)).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.create(buildCreateRequest()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only OWNER");
    }

    @Test
    @DisplayName("findAll: trả về tất cả class trong center hiện tại")
    void findAll_shouldReturnAllClassesInCenter() {
        when(classRepository.findAllByCenter_Id(CENTER_ID))
                .thenReturn(List.of(
                        buildClass(1L, CENTER_ID, "Class A", ClassStatus.ACTIVE),
                        buildClass(2L, CENTER_ID, "Class B", ClassStatus.FINISHED)
                ));

        List<ClassResponse> response = service.findAll();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getName()).isEqualTo("Class A");
        assertThat(response.get(1).getIsActive()).isFalse();
        assertThat(response.get(1).getStatus()).isEqualTo(ClassStatus.FINISHED);
    }

    @Test
    @DisplayName("findMyClassesAsTeacher: TEACHER có lịch dạy → trả về distinct class IDs")
    void findMyClassesAsTeacher_whenTeacherHasSchedules_shouldReturnClasses() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEACHER_PHONE, null, List.of())
        );
        User teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setPhoneNumber(TEACHER_PHONE);
        teacher.setRole(Role.TEACHER);
        when(userRepository.findByPhoneNumber(TEACHER_PHONE)).thenReturn(Optional.of(teacher));

        Schedule s1 = new Schedule();
        s1.setClazz(buildClass(1L, CENTER_ID, "Class A", ClassStatus.ACTIVE));
        Schedule s2 = new Schedule();
        s2.setClazz(buildClass(2L, CENTER_ID, "Class B", ClassStatus.ACTIVE));
        Schedule s3 = new Schedule();
        s3.setClazz(buildClass(1L, CENTER_ID, "Class A", ClassStatus.ACTIVE));

        when(scheduleRepository.findAllByTeacherUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID))
                .thenReturn(List.of(s1, s2, s3));
        when(classRepository.findById(1L)).thenReturn(Optional.of(buildClass(1L, CENTER_ID, "Class A", ClassStatus.ACTIVE)));
        when(classRepository.findById(2L)).thenReturn(Optional.of(buildClass(2L, CENTER_ID, "Class B", ClassStatus.ACTIVE)));

        List<ClassResponse> response = service.findMyClassesAsTeacher();

        assertThat(response).hasSize(2);
    }

    @Test
    @DisplayName("findMyClassesAsTeacher: caller không phải TEACHER → AccessDeniedException")
    void findMyClassesAsTeacher_whenCallerIsNotTeacher_shouldThrowAccessDenied() {
        assertThatThrownBy(() -> service.findMyClassesAsTeacher())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only TEACHER");
    }

    @Test
    @DisplayName("findMyClassesWithStudentsAsTeacher: TEACHER + có enrollment → trả về class kèm danh sách student")
    void findMyClassesWithStudentsAsTeacher_shouldReturnClassesWithStudents() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEACHER_PHONE, null, List.of())
        );
        User teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setPhoneNumber(TEACHER_PHONE);
        teacher.setRole(Role.TEACHER);
        when(userRepository.findByPhoneNumber(TEACHER_PHONE)).thenReturn(Optional.of(teacher));

        Schedule s = new Schedule();
        s.setClazz(buildClass(1L, CENTER_ID, "Class A", ClassStatus.ACTIVE));
        when(scheduleRepository.findAllByTeacherUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID))
                .thenReturn(List.of(s));
        when(classRepository.findById(1L)).thenReturn(Optional.of(buildClass(1L, CENTER_ID, "Class A", ClassStatus.ACTIVE)));

        ClassEnrollment e1 = new ClassEnrollment();
        User student1 = new User();
        student1.setId(100L);
        student1.setFullName("Student A");
        student1.setPhoneNumber("0900000100");
        e1.setStudentUser(student1);
        e1.setStatus(EnrollmentStatus.ACTIVE);

        when(classEnrollmentRepository.findAllByClazz_IdAndStatus(1L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(e1));

        List<TeacherClassStudentsResponse> response = service.findMyClassesWithStudentsAsTeacher();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getClassName()).isEqualTo("Class A");
        assertThat(response.get(0).getStudentCount()).isEqualTo(1L);
        assertThat(response.get(0).getStudents()).hasSize(1);
        assertThat(response.get(0).getStudents().get(0).getFullName()).isEqualTo("Student A");
    }

    @Test
    @DisplayName("findAllClassesWithStudentsForOwner: OWNER + chỉ trả về class active")
    void findAllClassesWithStudentsForOwner_shouldReturnOnlyActiveClasses() {
        when(classRepository.findAllByCenter_Id(CENTER_ID))
                .thenReturn(List.of(
                        buildClass(1L, CENTER_ID, "Class Active", ClassStatus.ACTIVE),
                        buildClass(2L, CENTER_ID, "Class Inactive", ClassStatus.FINISHED)
                ));
        when(classEnrollmentRepository.findAllByClazz_IdAndStatus(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ArrayList<>());

        List<TeacherClassStudentsResponse> response = service.findAllClassesWithStudentsForOwner();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getClassName()).isEqualTo("Class Active");
    }

    @Test
    @DisplayName("findAllClassesWithStudentsForOwner: caller không phải OWNER → AccessDeniedException")
    void findAllClassesWithStudentsForOwner_whenCallerIsNotOwner_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEACHER_PHONE, null, List.of())
        );
        User teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setPhoneNumber(TEACHER_PHONE);
        teacher.setRole(Role.TEACHER);
        when(userRepository.findByPhoneNumber(TEACHER_PHONE)).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.findAllClassesWithStudentsForOwner())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only OWNER");
    }

    @Test
    @DisplayName("update: class thuộc center khác → TenancyViolationException")
    void update_whenClassInOtherCenter_shouldThrowTenancyViolation() {
        Class existing = buildClass(CLASS_ID, OTHER_CENTER_ID, "Old Name", ClassStatus.ACTIVE);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(CLASS_ID, buildCreateRequest()))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("update: đổi tên + tên mới đã tồn tại trong center → DuplicateResourceException")
    void update_whenNewNameExistsInCenter_shouldThrowDuplicate() {
        Class existing = buildClass(CLASS_ID, CENTER_ID, "Old Name", ClassStatus.ACTIVE);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(existing));
        when(classRepository.existsByNameAndCenter_Id("VSTEP B1 Morning", CENTER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(CLASS_ID, buildCreateRequest()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("update: giữ nguyên tên (case-insensitive) → OK, không check duplicate")
    void update_whenKeepingSameName_caseInsensitive_shouldNotThrowDuplicate() {
        Class existing = buildClass(CLASS_ID, CENTER_ID, "VSTEP B1 Morning", ClassStatus.ACTIVE);
        existing.setStartDate(LocalDate.of(2026, 8, 1));
        existing.setEndDate(LocalDate.of(2026, 9, 30));
        existing.setTeacherUser(buildTeacher(TEACHER_ID, CENTER_ID));
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(existing));
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(buildCourse(COURSE_ID)));
        when(classRepository.save(any(Class.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClassRequest req = ClassRequest.builder()
                .name("vstep b1 morning")
                .courseId(COURSE_ID)
                .endDate(LocalDate.of(2026, 10, 31))
                .monthlyFee(2000000.0)
                .build();

        ClassResponse response = service.update(CLASS_ID, req);

        assertThat(response.getName()).isEqualTo("vstep b1 morning");
        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(response.getTeacherUserId()).isEqualTo(TEACHER_ID);
        assertThat(response.getTeacherName()).isEqualTo("Teacher " + TEACHER_ID);
    }

    @Test
    @DisplayName("delete: class thuộc center khác → TenancyViolationException")
    void delete_whenClassInOtherCenter_shouldThrowTenancyViolation() {
        Class existing = buildClass(CLASS_ID, OTHER_CENTER_ID, "Class A", ClassStatus.ACTIVE);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.delete(CLASS_ID))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("delete: class hợp lệ → xóa thành công")
    void delete_whenValid_shouldDeleteClass() {
        Class existing = buildClass(CLASS_ID, CENTER_ID, "Class A", ClassStatus.ACTIVE);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(existing));

        service.delete(CLASS_ID);

        org.mockito.Mockito.verify(paymentRepository).deleteByFeeRecord_Clazz_IdAndFeeRecord_Center_Id(CLASS_ID, CENTER_ID);
        org.mockito.Mockito.verify(installmentRepository).deleteByFeeRecord_Clazz_IdAndCenter_Id(CLASS_ID, CENTER_ID);
        org.mockito.Mockito.verify(discountRepository).deleteByFeeRecord_Clazz_IdAndCenter_Id(CLASS_ID, CENTER_ID);
        org.mockito.Mockito.verify(feeRecordRepository).deleteByClazz_IdAndCenter_Id(CLASS_ID, CENTER_ID);
        org.mockito.Mockito.verify(classEnrollmentRepository).deleteByClazz_IdAndCenter_Id(CLASS_ID, CENTER_ID);
        org.mockito.Mockito.verify(scheduleEventRepository).deleteByClazz_IdAndCenter_Id(CLASS_ID, CENTER_ID);
        org.mockito.Mockito.verify(scheduleRecurringRuleRepository).deleteByClazz_IdAndCenter_Id(CLASS_ID, CENTER_ID);
        org.mockito.Mockito.verify(scheduleRepository).deleteByClazz_IdAndCenter_Id(CLASS_ID, CENTER_ID);
        org.mockito.Mockito.verify(classRepository).delete(existing);
    }

    @Test
    @DisplayName("delete: class da co hoc phi duoc ghi nhan -> BusinessRuleException")
    void delete_whenClassHasSettledFees_shouldThrowBusinessRule() {
        Class existing = buildClass(CLASS_ID, CENTER_ID, "Class A", ClassStatus.ACTIVE);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(existing));
        when(feeRecordRepository.countSettledRecordsByClass(CLASS_ID, CENTER_ID)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(CLASS_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("hoc phi");

        org.mockito.Mockito.verify(classRepository, org.mockito.Mockito.never()).delete(any(Class.class));
    }

    @Test
    @DisplayName("delete: class không tồn tại → ResourceNotFoundException")
    void delete_whenClassNotFound_shouldThrowResourceNotFound() {
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(CLASS_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create: TenantContext null → BadRequestException")
    void create_whenTenantContextIsNull_shouldThrowBadRequest() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.create(buildCreateRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tenant context");
    }

}
