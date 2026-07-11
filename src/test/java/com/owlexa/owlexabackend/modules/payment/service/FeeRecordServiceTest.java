package com.owlexa.owlexabackend.modules.payment.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.payment.dto.request.FeeRecordGenerateRequest;
import com.owlexa.owlexabackend.modules.payment.dto.response.FeeRecordResponse;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
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

import java.math.BigDecimal;
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
class FeeRecordServiceTest {

    @Mock private FeeRecordRepository feeRecordRepository;
    @Mock private ClassRepository classRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;

    private FeeRecordService service;

    private static final String OWNER_PHONE = "0900000001";
    private static final Long OWNER_ID = 1L;
    private static final Long CENTER_ID = 10L;
    private static final Long OTHER_CENTER_ID = 99L;
    private static final Long CLASS_ID = 50L;

    @BeforeEach
    void setUp() {
        service = new FeeRecordService(
                feeRecordRepository, classRepository, classEnrollmentRepository,
                userRepository, membershipRepository
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

    private Class buildClass(Long centerId, Double monthlyFee) {
        Center center = new Center();
        center.setId(centerId);

        Class clazz = new Class();
        clazz.setId(CLASS_ID);
        clazz.setName("VSTEP B1");
        clazz.setCenter(center);
        clazz.setMonthlyFee(monthlyFee);
        clazz.setMaxStudents(20);
        return clazz;
    }

    private ClassEnrollment buildEnrollment(Long studentId) {
        User student = new User();
        student.setId(studentId);
        student.setPhoneNumber("09" + String.format("%08d", studentId));
        student.setFullName("Student " + studentId);

        Class clazz = new Class();
        clazz.setId(CLASS_ID);
        clazz.setName("VSTEP B1");

        ClassEnrollment e = new ClassEnrollment();
        e.setStudentUser(student);
        e.setClazz(clazz);
        e.setStatus(EnrollmentStatus.ACTIVE);
        return e;
    }

    private FeeRecordGenerateRequest buildGenerateRequest() {
        return FeeRecordGenerateRequest.builder()
                .month("2026-07")
                .dueDate(LocalDate.of(2026, 7, 31))
                .build();
    }

    @Test
    @DisplayName("generateForClass: OWNER + class có enrollment → tạo fee records cho mỗi student")
    void generateForClass_whenValid_shouldCreateFeeRecordsForEachStudent() {
        Class clazz = buildClass(CENTER_ID, 1500000.0);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(feeRecordRepository.existsByClazz_IdAndMonth(CLASS_ID, "2026-07")).thenReturn(false);
        when(classEnrollmentRepository.findAllByClazz_IdAndStatus(CLASS_ID, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(buildEnrollment(100L), buildEnrollment(101L)));
        when(feeRecordRepository.saveAll(any())).thenAnswer(invocation -> {
            List<FeeRecord> records = invocation.getArgument(0);
            long id = 1L;
            for (FeeRecord r : records) {
                r.setId(id++);
            }
            return records;
        });

        List<FeeRecordResponse> responses = service.generateForClass(CLASS_ID, buildGenerateRequest());

        assertThat(responses).hasSize(2);
        assertThat(responses).allSatisfy(r -> {
            assertThat(r.getAmount()).isEqualByComparingTo(new BigDecimal("1500000"));
            assertThat(r.getStatus()).isEqualTo(FeeStatus.UNPAID);
            assertThat(r.getMonth()).isEqualTo("2026-07");
        });
    }

    @Test
    @DisplayName("generateForClass: class thuộc center khác → TenancyViolationException")
    void generateForClass_whenClassInOtherCenter_shouldThrowTenancyViolation() {
        Class clazz = buildClass(OTHER_CENTER_ID, 1500000.0);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));

        assertThatThrownBy(() -> service.generateForClass(CLASS_ID, buildGenerateRequest()))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("generateForClass: class không tồn tại → ResourceNotFoundException")
    void generateForClass_whenClassNotFound_shouldThrowResourceNotFound() {
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateForClass(CLASS_ID, buildGenerateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("generateForClass: month đã có fee → DuplicateResourceException")
    void generateForClass_whenFeeAlreadyExistsForMonth_shouldThrowDuplicate() {
        Class clazz = buildClass(CENTER_ID, 1500000.0);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(feeRecordRepository.existsByClazz_IdAndMonth(CLASS_ID, "2026-07")).thenReturn(true);

        assertThatThrownBy(() -> service.generateForClass(CLASS_ID, buildGenerateRequest()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("generateForClass: month format sai → BadRequestException")
    void generateForClass_whenMonthFormatInvalid_shouldThrowBadRequest() {
        Class clazz = buildClass(CENTER_ID, 1500000.0);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));

        FeeRecordGenerateRequest bad = FeeRecordGenerateRequest.builder()
                .month("2026/07")
                .dueDate(LocalDate.now())
                .build();

        assertThatThrownBy(() -> service.generateForClass(CLASS_ID, bad))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("YYYY-MM");
    }

    @Test
    @DisplayName("generateForClass: class không có student active → BadRequestException")
    void generateForClass_whenNoActiveEnrollments_shouldThrowBadRequest() {
        Class clazz = buildClass(CENTER_ID, 1500000.0);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(feeRecordRepository.existsByClazz_IdAndMonth(CLASS_ID, "2026-07")).thenReturn(false);
        when(classEnrollmentRepository.findAllByClazz_IdAndStatus(CLASS_ID, EnrollmentStatus.ACTIVE))
                .thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> service.generateForClass(CLASS_ID, buildGenerateRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no active students");
    }

    @Test
    @DisplayName("generateForClass: caller không phải OWNER → AccessDeniedException")
    void generateForClass_whenCallerIsNotOwner_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("teacher-1", null, List.of())
        );
        User teacher = new User();
        teacher.setId(2L);
        teacher.setPhoneNumber("teacher-1");
        teacher.setRole(Role.TEACHER);
        when(userRepository.findByPhoneNumber("teacher-1")).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.generateForClass(CLASS_ID, buildGenerateRequest()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only OWNER");
    }

    @Test
    @DisplayName("findAllByClass: class thuộc center khác → TenancyViolationException")
    void findAllByClass_whenClassInOtherCenter_shouldThrowTenancyViolation() {
        Class clazz = buildClass(OTHER_CENTER_ID, 1500000.0);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));

        assertThatThrownBy(() -> service.findAllByClass(CLASS_ID, "2026-07"))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("findMyFees: trả về fee của student hiện tại")
    void findMyFees_shouldReturnCurrentStudentFees() {
        User student = new User();
        student.setId(100L);
        student.setPhoneNumber(OWNER_PHONE);
        student.setRole(Role.STUDENT);
        when(userRepository.findByPhoneNumber(OWNER_PHONE)).thenReturn(Optional.of(student));
        when(feeRecordRepository.findAllByStudentUser_IdOrderByCreatedAtDesc(100L))
                .thenReturn(new ArrayList<>());

        List<FeeRecordResponse> responses = service.findMyFees();

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("findAllOverdue: query với status=UNPAID và dueDate < today")
    void findAllOverdue_shouldQueryWithUnpaidAndPastDueDate() {
        when(feeRecordRepository.findAllByCenter_IdAndStatusAndDueDateBefore(
                org.mockito.ArgumentMatchers.eq(CENTER_ID),
                org.mockito.ArgumentMatchers.eq(FeeStatus.UNPAID),
                org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(new ArrayList<>());

        List<FeeRecordResponse> responses = service.findAllOverdue();

        assertThat(responses).isEmpty();
        org.mockito.Mockito.verify(feeRecordRepository)
                .findAllByCenter_IdAndStatusAndDueDateBefore(
                        org.mockito.ArgumentMatchers.eq(CENTER_ID),
                        org.mockito.ArgumentMatchers.eq(FeeStatus.UNPAID),
                        org.mockito.ArgumentMatchers.any(LocalDate.class)
                );
    }

    @Test
    @DisplayName("generateForClass: TenantContext null → BadRequestException")
    void generateForClass_whenTenantContextIsNull_shouldThrowBadRequest() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.generateForClass(CLASS_ID, buildGenerateRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tenant context");
    }
}