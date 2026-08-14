package com.owlexa.owlexabackend.modules.enrollment.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleRecurringRule;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleRepeatType;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleEventRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRecurringRuleRepository;
import com.owlexa.owlexabackend.modules.course.entity.Course;
import com.owlexa.owlexabackend.modules.enrollment.dto.request.EnrollmentRequest;
import com.owlexa.owlexabackend.modules.enrollment.dto.response.EnrollmentResponse;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
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

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private ClassRepository classRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private FeeRecordRepository feeRecordRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private ScheduleEventRepository scheduleEventRepository;
    @Mock private ScheduleRecurringRuleRepository scheduleRecurringRuleRepository;
    @Mock private com.owlexa.owlexabackend.modules.payment.repository.AuditLogRepository auditLogRepository;
    @Mock private com.owlexa.owlexabackend.modules.attendance.repository.AttendanceRepository attendanceRepository;
    @Mock private com.owlexa.owlexabackend.modules.assignment.repository.AssignmentRecipientRepository assignmentRecipientRepository;

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
                membershipRepository, feeRecordRepository, scheduleRepository,
                scheduleEventRepository, scheduleRecurringRuleRepository,
                auditLogRepository, attendanceRepository, assignmentRecipientRepository
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
        lenient().when(classRepository.findByIdForEnrollmentUpdate(any()))
                .thenAnswer(invocation -> classRepository.findById(invocation.getArgument(0)));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private Class buildClass(Long id, Long centerId) {
        Center center = new Center();
        center.setId(centerId);
        Class clazz = new Class();
        clazz.setId(id);
        clazz.setName("Class A");
        clazz.setCenter(center);
        clazz.setMonthlyFee(1500000.0);
        clazz.setStatus(com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus.ACTIVE);
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
        clazz.setMonthlyFee(1500000.0);

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

    private ScheduleRecurringRule buildRule(Long classId, String days, LocalDate startDate,
                                            LocalDate endDate, LocalTime startTime, LocalTime endTime) {
        ScheduleRecurringRule rule = new ScheduleRecurringRule();
        rule.setClazz(buildClass(classId, CENTER_ID));
        rule.setCenter(buildClass(classId, CENTER_ID).getCenter());
        rule.setRepeatType(ScheduleRepeatType.WEEKLY);
        rule.setDaysOfWeek(days);
        rule.setStartDate(startDate);
        rule.setEndDate(endDate);
        rule.setStartTime(startTime);
        rule.setEndTime(endTime);
        rule.setType(ScheduleType.THEORY_CLASS);
        rule.setIsActive(true);
        return rule;
    }

    private void stubEnrollmentFeeCreation() {
        when(classEnrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> {
            ClassEnrollment enrollment = invocation.getArgument(0);
            enrollment.setId(999L);
            return enrollment;
        });
        when(feeRecordRepository.findByStudentUser_IdAndClazz_IdAndMonth(
                org.mockito.ArgumentMatchers.eq(STUDENT_ID),
                org.mockito.ArgumentMatchers.eq(CLASS_ID),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());
        when(feeRecordRepository.save(any(FeeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("enroll: OWNER + class có chỗ + student mới → tạo enrollment ACTIVE + sinh FeeRecord")
    void enroll_whenValid_shouldCreateActiveEnrollmentAndFeeRecord() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent(STUDENT_ID)));
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID)).thenReturn(Optional.empty());
        when(classEnrollmentRepository.countByClazz_IdAndStatusIn(CLASS_ID,
                List.of(EnrollmentStatus.PENDING, EnrollmentStatus.ACTIVE, EnrollmentStatus.SUSPENDED))).thenReturn(5L);
        when(classEnrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> {
            ClassEnrollment e = invocation.getArgument(0);
            e.setId(999L);
            return e;
        });
        // FeeRecord does not exist yet → should be created
        when(feeRecordRepository.findByStudentUser_IdAndClazz_IdAndMonth(
                org.mockito.ArgumentMatchers.eq(STUDENT_ID),
                org.mockito.ArgumentMatchers.eq(CLASS_ID),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());
        when(feeRecordRepository.save(any(FeeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnrollmentResponse response = service.enroll(CLASS_ID, buildEnrollRequest());

        assertThat(response.getClassId()).isEqualTo(CLASS_ID);
        assertThat(response.getStudentUserId()).isEqualTo(STUDENT_ID);
        assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(response.getCenterId()).isEqualTo(CENTER_ID);
        org.mockito.Mockito.verify(feeRecordRepository).save(any(FeeRecord.class));
    }

    @Test
    @DisplayName("enroll: monthly fee trống nhưng course có học phí mặc định → vẫn sinh FeeRecord")
    void enroll_whenClassFeeMissing_shouldUseCourseDefaultFee() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
        clazz.setMonthlyFee(null);
        clazz.setCourse(Course.builder().defaultMonthlyFee(1750000.0).build());
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent(STUDENT_ID)));
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.empty());
        when(classEnrollmentRepository.countByClazz_IdAndStatusIn(eq(CLASS_ID), any())).thenReturn(0L);
        when(classEnrollmentRepository.save(any(ClassEnrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(feeRecordRepository.findByStudentUser_IdAndClazz_IdAndMonth(
                eq(STUDENT_ID), eq(CLASS_ID), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        when(feeRecordRepository.save(any(FeeRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.enroll(CLASS_ID, buildEnrollRequest());

        org.mockito.Mockito.verify(feeRecordRepository).save(org.mockito.ArgumentMatchers.argThat(fee ->
                fee.getAmount().compareTo(java.math.BigDecimal.valueOf(1750000.0)) == 0
                        && fee.getStatus() == FeeStatus.UNPAID));
    }

    @Test
    @DisplayName("enroll: lịch lặp cùng ngày và giao giờ bị từ chối")
    void enroll_whenRecurringSchedulesOverlap_shouldThrowStudentConflict() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
        ClassEnrollment existing = buildEnrollment(2L, STUDENT_ID, EnrollmentStatus.ACTIVE);
        existing.setClazz(buildClass(60L, CENTER_ID));

        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent(STUDENT_ID)));
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID)).thenReturn(Optional.empty());
        when(classEnrollmentRepository.countByClazz_IdAndStatusIn(eq(CLASS_ID), any())).thenReturn(0L);
        when(classEnrollmentRepository.findAllByStudentUser_IdAndCenter_IdAndStatusIn(
                eq(STUDENT_ID), eq(CENTER_ID), any())).thenReturn(List.of(existing));
        when(scheduleRepository.findAllByClazz_IdAndCenter_Id(any(), eq(CENTER_ID))).thenReturn(List.of());
        when(scheduleEventRepository.findAllByClazz_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(any(), eq(CENTER_ID)))
                .thenReturn(List.of());
        when(scheduleRecurringRuleRepository.findAllByClazz_IdAndCenter_IdOrderByStartDateAscStartTimeAsc(CLASS_ID, CENTER_ID))
                .thenReturn(List.of(buildRule(CLASS_ID, "2,4,6", LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 9, 30), LocalTime.of(18, 0), LocalTime.of(20, 0))));
        when(scheduleRecurringRuleRepository.findAllByClazz_IdAndCenter_IdOrderByStartDateAscStartTimeAsc(60L, CENTER_ID))
                .thenReturn(List.of(buildRule(60L, "2,4,6", LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 9, 30), LocalTime.of(19, 0), LocalTime.of(21, 0))));

        assertThatThrownBy(() -> service.enroll(CLASS_ID, buildEnrollRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("lịch học lớp khác");
    }

    @Test
    @DisplayName("enroll: lịch lặp đã hết hạn không bị coi là trùng")
    void enroll_whenExistingRecurringScheduleIsOutsideTargetDateRange_shouldSucceed() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
        ClassEnrollment existing = buildEnrollment(2L, STUDENT_ID, EnrollmentStatus.ACTIVE);
        existing.setClazz(buildClass(60L, CENTER_ID));

        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent(STUDENT_ID)));
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID)).thenReturn(Optional.empty());
        when(classEnrollmentRepository.countByClazz_IdAndStatusIn(eq(CLASS_ID), any())).thenReturn(0L);
        when(classEnrollmentRepository.findAllByStudentUser_IdAndCenter_IdAndStatusIn(
                eq(STUDENT_ID), eq(CENTER_ID), any())).thenReturn(List.of(existing));
        when(scheduleRepository.findAllByClazz_IdAndCenter_Id(any(), eq(CENTER_ID))).thenReturn(List.of());
        when(scheduleEventRepository.findAllByClazz_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(any(), eq(CENTER_ID)))
                .thenReturn(List.of());
        when(scheduleRecurringRuleRepository.findAllByClazz_IdAndCenter_IdOrderByStartDateAscStartTimeAsc(CLASS_ID, CENTER_ID))
                .thenReturn(List.of(buildRule(CLASS_ID, "2,4,6", LocalDate.of(2026, 8, 20),
                        LocalDate.of(2026, 9, 30), LocalTime.of(18, 0), LocalTime.of(20, 0))));
        when(scheduleRecurringRuleRepository.findAllByClazz_IdAndCenter_IdOrderByStartDateAscStartTimeAsc(60L, CENTER_ID))
                .thenReturn(List.of(buildRule(60L, "2,4,6", LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 8, 19), LocalTime.of(19, 0), LocalTime.of(21, 0))));
        stubEnrollmentFeeCreation();

        assertThat(service.enroll(CLASS_ID, buildEnrollRequest()).getStatus())
                .isEqualTo(EnrollmentStatus.ACTIVE);
    }

    @Test
    @DisplayName("enroll: khôi phục enrollment DROPPED khi lớp chưa có sức chứa phòng")
    void enroll_whenRestoringDroppedEnrollmentWithoutRoomCapacity_shouldSucceed() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent(STUDENT_ID)));

        ClassEnrollment dropped = buildEnrollment(1L, STUDENT_ID, EnrollmentStatus.DROPPED);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(dropped));
        when(classEnrollmentRepository.countByClazz_IdAndStatusIn(CLASS_ID,
                List.of(EnrollmentStatus.PENDING, EnrollmentStatus.ACTIVE, EnrollmentStatus.SUSPENDED)))
                .thenReturn(0L);
        when(classEnrollmentRepository.save(any(ClassEnrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(feeRecordRepository.findByStudentUser_IdAndClazz_IdAndMonth(
                org.mockito.ArgumentMatchers.eq(STUDENT_ID),
                org.mockito.ArgumentMatchers.eq(CLASS_ID),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(new FeeRecord()));

        EnrollmentResponse response = service.enroll(CLASS_ID, buildEnrollRequest());

        assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(dropped.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    }

    @Test
    @DisplayName("enroll: khôi phục enrollment DROPPED → mở lại FeeRecord CANCELLED chưa thu")
    void enroll_whenRestoringDroppedEnrollment_shouldReactivateCancelledUnpaidFee() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent(STUDENT_ID)));

        ClassEnrollment dropped = buildEnrollment(1L, STUDENT_ID, EnrollmentStatus.DROPPED);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(dropped));
        when(classEnrollmentRepository.countByClazz_IdAndStatusIn(CLASS_ID,
                List.of(EnrollmentStatus.PENDING, EnrollmentStatus.ACTIVE, EnrollmentStatus.SUSPENDED)))
                .thenReturn(0L);
        when(classEnrollmentRepository.save(any(ClassEnrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FeeRecord cancelledFee = FeeRecord.builder()
                .status(FeeStatus.CANCELLED)
                .amount(java.math.BigDecimal.valueOf(1500000L))
                .paidAmount(java.math.BigDecimal.ZERO)
                .build();
        when(feeRecordRepository.findByStudentUser_IdAndClazz_IdAndMonth(
                eq(STUDENT_ID), eq(CLASS_ID), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(cancelledFee));
        when(feeRecordRepository.save(any(FeeRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EnrollmentResponse response = service.enroll(CLASS_ID, buildEnrollRequest());

        assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(cancelledFee.getStatus()).isEqualTo(FeeStatus.UNPAID);
        assertThat(cancelledFee.getPaidAmount()).isEqualByComparingTo(java.math.BigDecimal.ZERO);
        assertThat(cancelledFee.getAmount()).isEqualByComparingTo(java.math.BigDecimal.valueOf(1500000L));
        org.mockito.Mockito.verify(feeRecordRepository).save(cancelledFee);
    }

    @Test
    @DisplayName("enroll: khôi phục DROPPED → không mở lại học phí CANCELLED của tháng cũ")
    void enroll_whenRestoringDroppedEnrollment_shouldKeepPreviousMonthCancelledFeesClosed() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent(STUDENT_ID)));

        ClassEnrollment dropped = buildEnrollment(1L, STUDENT_ID, EnrollmentStatus.DROPPED);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(dropped));
        when(classEnrollmentRepository.countByClazz_IdAndStatusIn(eq(CLASS_ID), any()))
                .thenReturn(0L);
        when(classEnrollmentRepository.save(any(ClassEnrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FeeRecord oldCancelledFee = FeeRecord.builder()
                .status(FeeStatus.CANCELLED)
                .amount(java.math.BigDecimal.valueOf(1500000L))
                .paidAmount(java.math.BigDecimal.ZERO)
                .month("2026-07")
                .build();
        when(feeRecordRepository.findByStudentUser_IdAndClazz_IdAndMonth(
                eq(STUDENT_ID), eq(CLASS_ID), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());
        when(feeRecordRepository.save(any(FeeRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.enroll(CLASS_ID, buildEnrollRequest());

        assertThat(oldCancelledFee.getStatus()).isEqualTo(FeeStatus.CANCELLED);
        org.mockito.Mockito.verify(feeRecordRepository, org.mockito.Mockito.never()).save(oldCancelledFee);
    }

    @Test
    @DisplayName("enroll: class thuộc center khác → TenancyViolationException")
    void enroll_whenClassInOtherCenter_shouldThrowTenancyViolation() {
        Class clazz = buildClass(CLASS_ID, OTHER_CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));

        assertThatThrownBy(() -> service.enroll(CLASS_ID, buildEnrollRequest()))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("enroll: user không phải STUDENT → BadRequestException")
    void enroll_whenUserIsNotStudent_shouldThrowBadRequest() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        User teacher = buildStudent(STUDENT_ID);
        teacher.setRole(Role.TEACHER);
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.enroll(CLASS_ID, buildEnrollRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Người dùng không phải là học sinh");
    }

    @Test
    @DisplayName("enroll: student đã ACTIVE rồi → trả lại enrollment hiện tại")
    void enroll_whenStudentAlreadyActive_shouldReturnExistingEnrollment() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent(STUDENT_ID)));
        ClassEnrollment enrollment = buildEnrollment(1L, STUDENT_ID, EnrollmentStatus.ACTIVE);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID)).thenReturn(Optional.of(enrollment));

        EnrollmentResponse response = service.enroll(CLASS_ID, buildEnrollRequest());

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        org.mockito.Mockito.verify(classEnrollmentRepository, org.mockito.Mockito.never())
                .save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("enroll: enrollment DROPPED cũ → khôi phục row, không insert trùng natural key")
    void enroll_whenDroppedHistoryExists_shouldRestoreExistingRow() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
        ClassEnrollment droppedHistory = buildEnrollment(1L, STUDENT_ID, EnrollmentStatus.DROPPED);

        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent(STUDENT_ID)));
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(droppedHistory));
        when(classEnrollmentRepository.countByClazz_IdAndStatusIn(eq(CLASS_ID), any())).thenReturn(0L);
        when(classEnrollmentRepository.save(any(ClassEnrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(feeRecordRepository.findByStudentUser_IdAndClazz_IdAndMonth(
                eq(STUDENT_ID), eq(CLASS_ID), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());

        EnrollmentResponse response = service.enroll(CLASS_ID, buildEnrollRequest());

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(droppedHistory.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        org.mockito.Mockito.verify(classEnrollmentRepository).save(droppedHistory);
    }

    @Test
    @DisplayName("enroll: class đầy → BusinessRuleException")
    void enroll_whenClassIsFull_shouldThrowBusinessRule() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent(STUDENT_ID)));
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID)).thenReturn(Optional.empty());
        when(classEnrollmentRepository.countByClazz_IdAndStatusIn(CLASS_ID,
                List.of(EnrollmentStatus.PENDING, EnrollmentStatus.ACTIVE, EnrollmentStatus.SUSPENDED))).thenReturn(20L);
        when(scheduleEventRepository.findMinRoomCapacityByClass(CLASS_ID, CENTER_ID)).thenReturn(20);

        assertThatThrownBy(() -> service.enroll(CLASS_ID, buildEnrollRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Lớp học đã đạt sức chứa phòng học");
    }

    @Test
    @DisplayName("approve: PENDING → ACTIVE + tạo FeeRecord")
    void approve_whenPending_shouldActivateAndGenerateFeeRecord() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        ClassEnrollment enrollment = buildEnrollment(1L, STUDENT_ID, EnrollmentStatus.PENDING);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(enrollment));
        when(classEnrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(feeRecordRepository.findByStudentUser_IdAndClazz_IdAndMonth(org.mockito.ArgumentMatchers.eq(STUDENT_ID), org.mockito.ArgumentMatchers.eq(CLASS_ID), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());
        when(feeRecordRepository.save(any(FeeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnrollmentResponse response = service.approve(CLASS_ID, STUDENT_ID);

        assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        org.mockito.Mockito.verify(feeRecordRepository).save(any(FeeRecord.class));
    }

    @Test
    @DisplayName("approve: enrollment không phải PENDING → BusinessRuleException")
    void approve_whenNotPending_shouldThrowBusinessRule() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        ClassEnrollment enrollment = buildEnrollment(1L, STUDENT_ID, EnrollmentStatus.ACTIVE);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> service.approve(CLASS_ID, STUDENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Chỉ có thể duyệt các yêu cầu đăng ký đang chờ xử lý");
    }

    @Test
    @DisplayName("approve: FeeRecord đã tồn tại → không tạo duplicate")
    void approve_whenFeeRecordAlreadyExists_shouldNotCreateDuplicate() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        ClassEnrollment enrollment = buildEnrollment(1L, STUDENT_ID, EnrollmentStatus.PENDING);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(enrollment));
        when(classEnrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(feeRecordRepository.findByStudentUser_IdAndClazz_IdAndMonth(org.mockito.ArgumentMatchers.eq(STUDENT_ID), org.mockito.ArgumentMatchers.eq(CLASS_ID), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(new FeeRecord()));

        EnrollmentResponse response = service.approve(CLASS_ID, STUDENT_ID);

        assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        org.mockito.Mockito.verify(feeRecordRepository, org.mockito.Mockito.never()).save(any(FeeRecord.class));
    }

    @Test
    @DisplayName("reject: PENDING → DROPPED")
    void reject_whenPending_shouldMarkDropped() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        ClassEnrollment enrollment = buildEnrollment(1L, STUDENT_ID, EnrollmentStatus.PENDING);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(enrollment));

        service.reject(CLASS_ID, STUDENT_ID);

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
    }

    @Test
    @DisplayName("reject: enrollment không phải PENDING → BusinessRuleException")
    void reject_whenNotPending_shouldThrowBusinessRule() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        ClassEnrollment enrollment = buildEnrollment(1L, STUDENT_ID, EnrollmentStatus.ACTIVE);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> service.reject(CLASS_ID, STUDENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Chỉ có thể từ chối các yêu cầu đăng ký đang chờ xử lý");
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
                .hasMessageContaining("Chỉ chủ trung tâm");
    }

    @Test
    @DisplayName("findAllByClass: trả về danh sách enrollment PENDING + ACTIVE + SUSPENDED")
    void findAllByClass_shouldReturnPendingActiveAndSuspendedEnrollments() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(classEnrollmentRepository.findAllByClazz_IdAndStatusIn(CLASS_ID,
                List.of(EnrollmentStatus.PENDING, EnrollmentStatus.ACTIVE, EnrollmentStatus.SUSPENDED)))
                .thenReturn(List.of(
                        buildEnrollment(1L, STUDENT_ID, EnrollmentStatus.PENDING),
                        buildEnrollment(2L, 101L, EnrollmentStatus.ACTIVE),
                        buildEnrollment(3L, 102L, EnrollmentStatus.SUSPENDED)
                ));

        List<EnrollmentResponse> response = service.findAllByClass(CLASS_ID);

        assertThat(response).hasSize(3);
    }

    @Test
    @DisplayName("drop: enrollment tồn tại + ACTIVE → set status = DROPPED")
    void drop_whenEnrollmentActive_shouldMarkDropped() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        ClassEnrollment enrollment = buildEnrollment(1L, STUDENT_ID, EnrollmentStatus.ACTIVE);
        var recipient = new com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipient();
        recipient.setStatus(com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipientStatus.ASSIGNED);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(enrollment));
        when(classEnrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assignmentRecipientRepository.findAllByStudentUser_IdAndClazz_IdAndSourceTypeAndStatus(
                eq(STUDENT_ID),
                eq(CLASS_ID),
                eq(com.owlexa.owlexabackend.modules.assignment.entity.AssignmentTargetType.CLASS),
                eq(com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipientStatus.ASSIGNED)))
                .thenReturn(List.of(recipient));

        service.drop(CLASS_ID, STUDENT_ID);

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
        assertThat(recipient.getStatus())
                .isEqualTo(com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipientStatus.REVOKED);
        org.mockito.Mockito.verify(attendanceRepository)
                .deleteLearningHistoryByStudentAndClass(STUDENT_ID, CLASS_ID, CENTER_ID);
    }

    @Test
    @DisplayName("drop: enrollment đã DROPPED → no-op (return im lặng)")
    void drop_whenEnrollmentAlreadyDropped_shouldBeNoOp() {
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
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
        Class clazz = buildClass(CLASS_ID, CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.drop(CLASS_ID, STUDENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("enroll: TenantContext null → BadRequestException")
    void enroll_whenTenantContextIsNull_shouldThrowBadRequest() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.enroll(CLASS_ID, buildEnrollRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Không xác định được trung tâm hiện tại");
    }
}

