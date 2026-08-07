package com.owlexa.owlexabackend.modules.attendance.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.modules.attendance.dto.request.AttendanceMarkRequest;
import com.owlexa.owlexabackend.modules.attendance.dto.response.AttendanceResponse;
import com.owlexa.owlexabackend.modules.attendance.dto.response.AttendanceStatsResponse;
import com.owlexa.owlexabackend.modules.attendance.entity.Attendance;
import com.owlexa.owlexabackend.modules.attendance.entity.AttendanceStatus;
import com.owlexa.owlexabackend.modules.attendance.repository.AttendanceRepository;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleEventRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private ScheduleEventRepository scheduleEventRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private UserRepository userRepository;
    @Mock private FeeRecordRepository feeRecordRepository;

    private AttendanceService service;

    private static final String TEACHER_PHONE = "0900000001";
    private static final Long TEACHER_ID = 1L;
    private static final Long OTHER_TEACHER_ID = 2L;
    private static final Long CENTER_ID = 10L;
    private static final Long OTHER_CENTER_ID = 99L;
    private static final Long SCHEDULE_ID = 50L;
    private static final Long CLASS_ID = 200L;
    private static final Long STUDENT_ID = 100L;

    private User teacher;

    @BeforeEach
    void setUp() {
        service = new AttendanceService(
                attendanceRepository, scheduleRepository, scheduleEventRepository, classEnrollmentRepository,
                membershipRepository, userRepository, feeRecordRepository
        );
        TenantContext.setCurrentTenantId(CENTER_ID);

        teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setPhoneNumber(TEACHER_PHONE);
        teacher.setFullName("Teacher One");
        teacher.setRole(Role.TEACHER);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEACHER_PHONE, null, List.of())
        );

        lenient().when(userRepository.findByPhoneNumber(TEACHER_PHONE)).thenReturn(Optional.of(teacher));
        lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private Schedule buildSchedule(Long centerId) {
        Center center = new Center();
        center.setId(centerId);

        Class clazz = new Class();
        clazz.setId(CLASS_ID);
        clazz.setName("Class A");
        clazz.setCenter(center);
        clazz.setStatus(com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus.ACTIVE);

        Schedule schedule = new Schedule();
        schedule.setId(SCHEDULE_ID);
        schedule.setCenter(center);
        schedule.setClazz(clazz);
        schedule.setTeacherUser(teacher); // assigned to the current teacher
        return schedule;
    }

    private Schedule buildScheduleWithTeacher(Long centerId, User assignedTeacher) {
        Center center = new Center();
        center.setId(centerId);

        Class clazz = new Class();
        clazz.setId(CLASS_ID);
        clazz.setName("Class A");
        clazz.setCenter(center);
        clazz.setStatus(com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus.ACTIVE);

        Schedule schedule = new Schedule();
        schedule.setId(SCHEDULE_ID);
        schedule.setCenter(center);
        schedule.setClazz(clazz);
        schedule.setTeacherUser(assignedTeacher);
        return schedule;
    }

    private User buildStudent(Long id) {
        User student = new User();
        student.setId(id);
        student.setPhoneNumber("09" + String.format("%08d", id));
        student.setFullName("Student " + id);
        student.setRole(Role.STUDENT);
        return student;
    }

    private Attendance buildAttendance(Long studentId) {
        Center center = new Center();
        center.setId(CENTER_ID);

        Class clazz = new Class();
        clazz.setId(CLASS_ID);
        clazz.setName("Class A");
        clazz.setCenter(center);

        Schedule schedule = new Schedule();
        schedule.setId(SCHEDULE_ID);
        schedule.setCenter(center);
        schedule.setClazz(clazz);

        Attendance attendance = new Attendance();
        attendance.setSchedule(schedule);
        attendance.setStudentUser(buildStudent(studentId));
        attendance.setCenter(center);
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendance.setDate(LocalDate.of(2026, 7, 10));
        return attendance;
    }

    private AttendanceMarkRequest buildMarkRequest(List<AttendanceMarkRequest.Item> items) {
        return AttendanceMarkRequest.builder()
                .date(LocalDate.of(2026, 7, 10))
                .records(items)
                .build();
    }

    @Test
    @DisplayName("mark: TEACHER + schedule hợp lệ + student ACTIVE → tạo attendance mới")
    void mark_whenValid_shouldCreateAttendance() {
        Schedule schedule = buildSchedule(CENTER_ID);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent(STUDENT_ID)));
        when(classEnrollmentRepository.existsByClazz_IdAndStudentUser_IdAndStatus(
                CLASS_ID, STUDENT_ID, EnrollmentStatus.ACTIVE)).thenReturn(true);
        when(feeRecordRepository.existsByStudentUser_IdAndClazz_IdAndStatusAndDueDateBefore(
                org.mockito.ArgumentMatchers.eq(STUDENT_ID), org.mockito.ArgumentMatchers.eq(CLASS_ID),
                org.mockito.ArgumentMatchers.eq(FeeStatus.UNPAID), org.mockito.ArgumentMatchers.any()))
                .thenReturn(false);
        when(attendanceRepository.findBySchedule_IdAndStudentUser_IdAndDate(
                SCHEDULE_ID, STUDENT_ID, LocalDate.of(2026, 7, 10))).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance a = invocation.getArgument(0);
            a.setId(1L);
            return a;
        });

        AttendanceMarkRequest.Item item = AttendanceMarkRequest.Item.builder()
                .studentUserId(STUDENT_ID)
                .status(AttendanceStatus.PRESENT)
                .note("on time")
                .build();

        List<AttendanceResponse> response = service.mark(SCHEDULE_ID, buildMarkRequest(List.of(item)));

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getStudentUserId()).isEqualTo(STUDENT_ID);
        assertThat(response.get(0).getStatus()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    @DisplayName("mark: schedule thuộc center khác → TenancyViolationException")
    void mark_whenScheduleInOtherCenter_shouldThrowTenancyViolation() {
        Schedule schedule = buildSchedule(OTHER_CENTER_ID);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));

        AttendanceMarkRequest.Item item = AttendanceMarkRequest.Item.builder()
                .studentUserId(STUDENT_ID)
                .status(AttendanceStatus.PRESENT)
                .build();

        assertThatThrownBy(() -> service.mark(SCHEDULE_ID, buildMarkRequest(List.of(item))))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("mark: user không phải STUDENT → BadRequestException")
    void mark_whenUserIsNotStudent_shouldThrowBadRequest() {
        Schedule schedule = buildSchedule(CENTER_ID);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        User notStudent = buildStudent(STUDENT_ID);
        notStudent.setRole(Role.TEACHER);
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(notStudent));

        AttendanceMarkRequest.Item item = AttendanceMarkRequest.Item.builder()
                .studentUserId(STUDENT_ID)
                .status(AttendanceStatus.PRESENT)
                .build();

        assertThatThrownBy(() -> service.mark(SCHEDULE_ID, buildMarkRequest(List.of(item))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("mark: student không ACTIVE trong class → BusinessRuleException")
    void mark_whenStudentNotActiveInClass_shouldThrowBusinessRule() {
        Schedule schedule = buildSchedule(CENTER_ID);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent(STUDENT_ID)));
        when(classEnrollmentRepository.existsByClazz_IdAndStudentUser_IdAndStatus(
                CLASS_ID, STUDENT_ID, EnrollmentStatus.ACTIVE)).thenReturn(false);

        AttendanceMarkRequest.Item item = AttendanceMarkRequest.Item.builder()
                .studentUserId(STUDENT_ID)
                .status(AttendanceStatus.PRESENT)
                .build();

        assertThatThrownBy(() -> service.mark(SCHEDULE_ID, buildMarkRequest(List.of(item))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("mark: attendance đã tồn tại → update status, không tạo mới")
    void mark_whenAttendanceExists_shouldUpdateStatus() {
        Schedule schedule = buildSchedule(CENTER_ID);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent(STUDENT_ID)));
        when(classEnrollmentRepository.existsByClazz_IdAndStudentUser_IdAndStatus(
                CLASS_ID, STUDENT_ID, EnrollmentStatus.ACTIVE)).thenReturn(true);

        Attendance existing = new Attendance();
        existing.setSchedule(schedule);
        existing.setStudentUser(buildStudent(STUDENT_ID));
        existing.setCenter(schedule.getCenter());
        existing.setStatus(AttendanceStatus.ABSENT);
        when(attendanceRepository.findBySchedule_IdAndStudentUser_IdAndDate(
                SCHEDULE_ID, STUDENT_ID, LocalDate.of(2026, 7, 10))).thenReturn(Optional.of(existing));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceMarkRequest.Item item = AttendanceMarkRequest.Item.builder()
                .studentUserId(STUDENT_ID)
                .status(AttendanceStatus.PRESENT)
                .build();

        List<AttendanceResponse> response = service.mark(SCHEDULE_ID, buildMarkRequest(List.of(item)));

        assertThat(response).hasSize(1);
        assertThat(existing.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    @DisplayName("mark: caller là STUDENT → AccessDeniedException")
    void mark_whenCallerIsStudent_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student-x", null, List.of())
        );
        User student = new User();
        student.setId(99L);
        student.setPhoneNumber("student-x");
        student.setRole(Role.STUDENT);
        when(userRepository.findByPhoneNumber("student-x")).thenReturn(Optional.of(student));

        Schedule schedule = buildSchedule(CENTER_ID);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));

        AttendanceMarkRequest.Item item = AttendanceMarkRequest.Item.builder()
                .studentUserId(STUDENT_ID)
                .status(AttendanceStatus.PRESENT)
                .build();

        assertThatThrownBy(() -> service.mark(SCHEDULE_ID, buildMarkRequest(List.of(item))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("mark: OWNER không được phép mark attendance nữa → AccessDeniedException")
    void mark_whenCallerIsOwner_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner-x", null, List.of())
        );
        User owner = new User();
        owner.setId(1L);
        owner.setPhoneNumber("owner-x");
        owner.setRole(Role.OWNER);
        when(userRepository.findByPhoneNumber("owner-x")).thenReturn(Optional.of(owner));

        Schedule schedule = buildSchedule(CENTER_ID);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));

        AttendanceMarkRequest.Item item = AttendanceMarkRequest.Item.builder()
                .studentUserId(STUDENT_ID)
                .status(AttendanceStatus.PRESENT)
                .build();

        assertThatThrownBy(() -> service.mark(SCHEDULE_ID, buildMarkRequest(List.of(item))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("mark: teacher không phải giáo viên được phân công của schedule → AccessDeniedException")
    void mark_whenTeacherNotAssignedToSchedule_shouldThrowAccessDenied() {
        User otherTeacher = new User();
        otherTeacher.setId(OTHER_TEACHER_ID);
        otherTeacher.setPhoneNumber("0900000002");
        otherTeacher.setFullName("Other Teacher");
        otherTeacher.setRole(Role.TEACHER);

        User assignedTeacher = new User();
        assignedTeacher.setId(999L);
        assignedTeacher.setPhoneNumber("0999999999");
        assignedTeacher.setFullName("Assigned Teacher");
        assignedTeacher.setRole(Role.TEACHER);

        Schedule schedule = buildScheduleWithTeacher(CENTER_ID, assignedTeacher);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));

        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(otherTeacher.getPhoneNumber(), null, List.of())
        );
        when(userRepository.findByPhoneNumber(otherTeacher.getPhoneNumber())).thenReturn(Optional.of(otherTeacher));
        when(membershipRepository.existsByUser_IdAndCenter_Id(OTHER_TEACHER_ID, CENTER_ID)).thenReturn(true);

        AttendanceMarkRequest.Item item = AttendanceMarkRequest.Item.builder()
                .studentUserId(STUDENT_ID)
                .status(AttendanceStatus.PRESENT)
                .build();

        assertThatThrownBy(() -> service.mark(SCHEDULE_ID, buildMarkRequest(List.of(item))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("mark: schedule không có teacher được phân công → BusinessRuleException")
    void mark_whenScheduleHasNoTeacher_shouldThrowBusinessRule() {
        Schedule schedule = buildScheduleWithTeacher(CENTER_ID, null);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));

        AttendanceMarkRequest.Item item = AttendanceMarkRequest.Item.builder()
                .studentUserId(STUDENT_ID)
                .status(AttendanceStatus.PRESENT)
                .build();

        assertThatThrownBy(() -> service.mark(SCHEDULE_ID, buildMarkRequest(List.of(item))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("findAllBySchedule: trả về attendance theo schedule + date")
    void findAllBySchedule_shouldReturnAttendanceList() {
        Schedule schedule = buildSchedule(CENTER_ID);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        when(attendanceRepository.findAllBySchedule_IdAndDate(SCHEDULE_ID, LocalDate.of(2026, 7, 10)))
                .thenReturn(List.of(buildAttendance(100L), buildAttendance(101L)));

        List<AttendanceResponse> response = service.findAllBySchedule(SCHEDULE_ID, LocalDate.of(2026, 7, 10));

        assertThat(response).hasSize(2);
    }

    @Test
    @DisplayName("findAllBySchedule: schedule thuộc center khác → TenancyViolationException")
    void findAllBySchedule_whenScheduleInOtherCenter_shouldThrowTenancyViolation() {
        Schedule schedule = buildSchedule(OTHER_CENTER_ID);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> service.findAllBySchedule(SCHEDULE_ID, LocalDate.now()))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("findMyAttendancesAsStudent: trả về attendance của chính student đó")
    void findMyAttendancesAsStudent_shouldReturnOwnAttendance() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student-x", null, List.of())
        );
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber("student-x");
        student.setRole(Role.STUDENT);
        when(userRepository.findByPhoneNumber("student-x")).thenReturn(Optional.of(student));
        when(attendanceRepository.findByStudentUser_IdAndSchedule_Clazz_IdAndDate(
                STUDENT_ID, CLASS_ID, LocalDate.of(2026, 7, 10)))
                .thenReturn(List.of(buildAttendance(STUDENT_ID)));

        List<AttendanceResponse> response = service.findMyAttendancesAsStudent(CLASS_ID, LocalDate.of(2026, 7, 10));

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getStudentUserId()).isEqualTo(STUDENT_ID);
    }

    @Test
    @DisplayName("findMyAttendancesAsStudent: non-STUDENT gọi → AccessDeniedException")
    void findMyAttendancesAsStudent_whenCallerIsNotStudent_shouldThrowAccessDenied() {
        // Current auth context is TEACHER
        assertThatThrownBy(() -> service.findMyAttendancesAsStudent(CLASS_ID, LocalDate.now()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("findMyAttendancesAsStudent: không có classId → trả về attendance hôm nay")
    void findMyAttendancesAsStudent_withoutClassId_shouldReturnTodayAttendance() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student-x", null, List.of())
        );
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber("student-x");
        student.setRole(Role.STUDENT);
        when(userRepository.findByPhoneNumber("student-x")).thenReturn(Optional.of(student));
        when(attendanceRepository.findByStudentUser_IdAndDate(STUDENT_ID, LocalDate.now()))
                .thenReturn(List.of());

        List<AttendanceResponse> response = service.findMyAttendancesAsStudent(null, null);

        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("getStats: OWNER lấy thống kê attendance → trả về AttendanceStatsResponse")
    void getStats_shouldReturnStatistics() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner-x", null, List.of())
        );
        User owner = new User();
        owner.setId(1L);
        owner.setPhoneNumber("owner-x");
        owner.setRole(Role.OWNER);
        when(userRepository.findByPhoneNumber("owner-x")).thenReturn(Optional.of(owner));
        when(membershipRepository.existsByUser_IdAndCenter_Id(1L, CENTER_ID)).thenReturn(true);
        when(attendanceRepository.countByClassAndDateRangeGroupByStatus(
                CLASS_ID, LocalDate.now().minusMonths(1), LocalDate.now(), CENTER_ID))
                .thenReturn(List.of(
                        new Object[]{AttendanceStatus.PRESENT, 10L},
                        new Object[]{AttendanceStatus.ABSENT, 3L},
                        new Object[]{AttendanceStatus.LATE, 2L}
                ));

        AttendanceStatsResponse response = service.getStats(CLASS_ID, null, null);

        assertThat(response.getClassId()).isEqualTo(CLASS_ID);
        assertThat(response.getStatusCounts()).hasSize(3);
        assertThat(response.getStatusPercentages()).hasSize(3);
    }

    @Test
    @DisplayName("getStats: non-OWNER gọi → AccessDeniedException")
    void getStats_whenCallerIsNotOwner_shouldThrowAccessDenied() {
        // Current auth context is TEACHER
        assertThatThrownBy(() -> service.getStats(CLASS_ID, null, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("mark: schedule không tồn tại → ResourceNotFoundException")
    void mark_whenScheduleNotFound_shouldThrowResourceNotFound() {
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.empty());

        AttendanceMarkRequest.Item item = AttendanceMarkRequest.Item.builder()
                .studentUserId(STUDENT_ID)
                .status(AttendanceStatus.PRESENT)
                .build();

        assertThatThrownBy(() -> service.mark(SCHEDULE_ID, buildMarkRequest(List.of(item))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("mark: student có unpaid overdue fee → BusinessRuleException")
    void mark_whenStudentHasUnpaidOverdueFee_shouldThrowBusinessRule() {
        Schedule schedule = buildSchedule(CENTER_ID);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent(STUDENT_ID)));
        when(classEnrollmentRepository.existsByClazz_IdAndStudentUser_IdAndStatus(
                CLASS_ID, STUDENT_ID, EnrollmentStatus.ACTIVE)).thenReturn(true);
        when(feeRecordRepository.existsByStudentUser_IdAndClazz_IdAndStatusAndDueDateBefore(
                org.mockito.ArgumentMatchers.eq(STUDENT_ID), org.mockito.ArgumentMatchers.eq(CLASS_ID),
                org.mockito.ArgumentMatchers.eq(FeeStatus.UNPAID), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);

        AttendanceMarkRequest.Item item = AttendanceMarkRequest.Item.builder()
                .studentUserId(STUDENT_ID)
                .status(AttendanceStatus.PRESENT)
                .build();

        assertThatThrownBy(() -> service.mark(SCHEDULE_ID, buildMarkRequest(List.of(item))))
                .isInstanceOf(BusinessRuleException.class);
    }
}
