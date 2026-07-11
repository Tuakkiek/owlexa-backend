package com.owlexa.owlexabackend.modules.enrollment.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.enrollment.dto.request.EnrollmentRequest;
import com.owlexa.owlexabackend.modules.enrollment.dto.response.EnrollmentResponse;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private ClassRepository classRepository;
    @Mock private UserRepository userRepository;
    @Mock private CenterRepository centerRepository;
    @Mock private MembershipRepository membershipRepository;

    private EnrollmentService service;

    private static final String OWNER_PHONE = "0900000001";
    private static final Long OWNER_ID = 1L;
    private static final Long CENTER_ID = 10L;
    private static final Long OTHER_CENTER_ID = 99L;
    private static final Long CLASS_ID = 50L;
    private static final Long STUDENT_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new EnrollmentService(
                classEnrollmentRepository, classRepository, userRepository,
                centerRepository, membershipRepository
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

    private Class buildClass(Long id, Long centerId, int maxStudents) {
        Center center = new Center();
        center.setId(centerId);
        Class clazz = new Class();
        clazz.setId(id);
        clazz.setName("Class A");
        clazz.setCenter(center);
        clazz.setMaxStudents(maxStudents);
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

    private ClassEnrollment buildEnrollment(Long id, Long studentId, EnrollmentStatus status) {
        Center center = new Center();
        center.setId(CENTER_ID);

        Class clazz = new Class();
        clazz.setId(CLASS_ID);
        clazz.setName("Class A");
        clazz.setCenter(center);

        User student = buildStudent(studentId);

        User enrolledBy = new User();
        enrolledBy.setId(OWNER_ID);

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setId(id);
        enrollment.setClazz(clazz);
        enrollment.setCenter(center);
        enrollment.setStudentUser(student);
        enrollment.setEnrolledByUser(enrolledBy);
        enrollment.setStatus(status);
        return enrollment;
    }

    private EnrollmentRequest buildEnrollRequest() {
        return EnrollmentRequest.builder().studentId(STUDENT_ID).build();
    }

    @Test
    @DisplayName("enroll: OWNER + class có chỗ + student mới → tạo enrollment ACTIVE")
    void enroll_whenValid_shouldCreateActiveEnrollment() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID, 20);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent(STUDENT_ID)));
        when(classEnrollmentRepository.existsByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID)).thenReturn(false);
        when(classEnrollmentRepository.countByClazz_IdAndStatus(CLASS_ID, EnrollmentStatus.ACTIVE)).thenReturn(5L);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID)).thenReturn(Optional.empty());
        when(classEnrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> {
            ClassEnrollment e = invocation.getArgument(0);
            e.setId(999L);
            return e;
        });

        EnrollmentResponse response = service.enroll(CLASS_ID, buildEnrollRequest());

        assertThat(response.getClassId()).isEqualTo(CLASS_ID);
        assertThat(response.getStudentUserId()).isEqualTo(STUDENT_ID);
        assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(response.getCenterId()).isEqualTo(CENTER_ID);
    }

    @Test
    @DisplayName("enroll: class thuộc center khác → TenancyViolationException")
    void enroll_whenClassInOtherCenter_shouldThrowTenancyViolation() {
        Class clazz = buildClass(CLASS_ID, OTHER_CENTER_ID, 20);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));

        assertThatThrownBy(() -> service.enroll(CLASS_ID, buildEnrollRequest()))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("enroll: user không phải STUDENT → BadRequestException")
    void enroll_whenUserIsNotStudent_shouldThrowBadRequest() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID, 20);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        User teacher = buildStudent(STUDENT_ID);
        teacher.setRole(Role.TEACHER);
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.enroll(CLASS_ID, buildEnrollRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a student");
    }

    @Test
    @DisplayName("enroll: student đã enroll rồi → DuplicateResourceException")
    void enroll_whenStudentAlreadyEnrolled_shouldThrowDuplicate() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID, 20);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent(STUDENT_ID)));
        when(classEnrollmentRepository.existsByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.enroll(CLASS_ID, buildEnrollRequest()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("enroll: class đầy → BusinessRuleException")
    void enroll_whenClassIsFull_shouldThrowBusinessRule() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID, 20);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent(STUDENT_ID)));
        when(classEnrollmentRepository.existsByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID)).thenReturn(false);
        when(classEnrollmentRepository.countByClazz_IdAndStatus(CLASS_ID, EnrollmentStatus.ACTIVE)).thenReturn(20L);

        assertThatThrownBy(() -> service.enroll(CLASS_ID, buildEnrollRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Class is full");
    }

    @Test
    @DisplayName("enroll: enrollment cũ tồn tại status=DROPPED → reactivate thành ACTIVE")
    void enroll_whenExistingDroppedEnrollment_shouldReactivate() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID, 20);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent(STUDENT_ID)));
        when(classEnrollmentRepository.existsByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID)).thenReturn(false);
        when(classEnrollmentRepository.countByClazz_IdAndStatus(CLASS_ID, EnrollmentStatus.ACTIVE)).thenReturn(5L);

        ClassEnrollment existing = new ClassEnrollment();
        existing.setId(1L);
        existing.setClazz(clazz);
        existing.setCenter(clazz.getCenter());
        existing.setStudentUser(buildStudent(STUDENT_ID));
        existing.setStatus(EnrollmentStatus.DROPPED);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(existing));
        when(classEnrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnrollmentResponse response = service.enroll(CLASS_ID, buildEnrollRequest());

        assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(existing.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    }

    @Test
    @DisplayName("enroll: enrollment cũ tồn tại status=ACTIVE → DuplicateResourceException (race condition)")
    void enroll_whenExistingActiveEnrollment_shouldThrowDuplicate() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID, 20);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent(STUDENT_ID)));
        when(classEnrollmentRepository.existsByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID)).thenReturn(false);
        when(classEnrollmentRepository.countByClazz_IdAndStatus(CLASS_ID, EnrollmentStatus.ACTIVE)).thenReturn(5L);

        ClassEnrollment existing = new ClassEnrollment();
        existing.setId(1L);
        existing.setClazz(clazz);
        existing.setCenter(clazz.getCenter());
        existing.setStudentUser(buildStudent(STUDENT_ID));
        existing.setStatus(EnrollmentStatus.ACTIVE);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.enroll(CLASS_ID, buildEnrollRequest()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("enroll: caller không phải OWNER → AccessDeniedException")
    void enroll_whenCallerIsNotOwner_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("teacher-x", null, List.of())
        );
        User teacher = new User();
        teacher.setId(99L);
        teacher.setPhoneNumber("teacher-x");
        teacher.setRole(Role.TEACHER);
        when(userRepository.findByPhoneNumber("teacher-x")).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.enroll(CLASS_ID, buildEnrollRequest()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only OWNER");
    }

    @Test
    @DisplayName("findAllByClass: trả về danh sách enrollment ACTIVE trong class")
    void findAllByClass_shouldReturnActiveEnrollments() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID, 20);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(classEnrollmentRepository.findAllByClazz_IdAndStatus(CLASS_ID, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(
                        buildEnrollment(1L, STUDENT_ID, EnrollmentStatus.ACTIVE),
                        buildEnrollment(2L, 101L, EnrollmentStatus.ACTIVE)
                ));

        List<EnrollmentResponse> response = service.findAllByClass(CLASS_ID);

        assertThat(response).hasSize(2);
    }

    @Test
    @DisplayName("drop: enrollment tồn tại + ACTIVE → set status = DROPPED")
    void drop_whenEnrollmentActive_shouldMarkDropped() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID, 20);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        ClassEnrollment enrollment = buildEnrollment(1L, STUDENT_ID, EnrollmentStatus.ACTIVE);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(enrollment));
        when(classEnrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.drop(CLASS_ID, STUDENT_ID);

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
    }

    @Test
    @DisplayName("drop: enrollment đã DROPPED → no-op (return im lặng)")
    void drop_whenEnrollmentAlreadyDropped_shouldBeNoOp() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID, 20);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        ClassEnrollment enrollment = buildEnrollment(1L, STUDENT_ID, EnrollmentStatus.DROPPED);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(enrollment));

        service.drop(CLASS_ID, STUDENT_ID);

        org.mockito.Mockito.verify(classEnrollmentRepository, org.mockito.Mockito.never())
                .save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("drop: enrollment không tồn tại → ResourceNotFoundException")
    void drop_whenEnrollmentNotFound_shouldThrowResourceNotFound() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID, 20);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.drop(CLASS_ID, STUDENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("remove: class thuộc center khác → TenancyViolationException")
    void remove_whenClassInOtherCenter_shouldThrowTenancyViolation() {
        Class clazz = buildClass(CLASS_ID, OTHER_CENTER_ID, 20);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));

        assertThatThrownBy(() -> service.remove(CLASS_ID, STUDENT_ID))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("remove: enrollment hợp lệ → xóa")
    void remove_whenValid_shouldDeleteEnrollment() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID, 20);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        ClassEnrollment enrollment = buildEnrollment(1L, STUDENT_ID, EnrollmentStatus.ACTIVE);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(enrollment));

        service.remove(CLASS_ID, STUDENT_ID);

        org.mockito.Mockito.verify(classEnrollmentRepository).delete(enrollment);
    }

    @Test
    @DisplayName("enroll: TenantContext null → BadRequestException")
    void enroll_whenTenantContextIsNull_shouldThrowBadRequest() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.enroll(CLASS_ID, buildEnrollRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tenant context");
    }
}