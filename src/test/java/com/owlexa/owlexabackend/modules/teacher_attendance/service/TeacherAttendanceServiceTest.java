package com.owlexa.owlexabackend.modules.teacher_attendance.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEvent;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventStatus;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventType;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleEventRepository;
import com.owlexa.owlexabackend.modules.teacher_attendance.dto.request.TeacherAttendanceMarkRequest;
import com.owlexa.owlexabackend.modules.teacher_attendance.dto.response.TeacherAttendanceResponse;
import com.owlexa.owlexabackend.modules.teacher_attendance.entity.TeacherAttendance;
import com.owlexa.owlexabackend.modules.teacher_attendance.entity.TeacherAttendanceStatus;
import com.owlexa.owlexabackend.modules.teacher_attendance.repository.TeacherAttendanceRepository;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherAttendanceServiceTest {

    @Mock private TeacherAttendanceRepository teacherAttendanceRepository;
    @Mock private ScheduleEventRepository scheduleEventRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;

    private TeacherAttendanceService service;

    private static final String OWNER_PHONE = "0900000000";
    private static final Long OWNER_ID = 1L;
    private static final Long CENTER_ID = 10L;
    private static final Long TEACHER_ID = 50L;
    private static final String TEACHER_PHONE = "0900000050";
    private static final Long ATTENDANCE_ID = 100L;
    private static final Long SCHEDULE_EVENT_ID_1 = 201L;
    private static final Long SCHEDULE_EVENT_ID_2 = 202L;

    @BeforeEach
    void setUp() {
        service = new TeacherAttendanceService(
                teacherAttendanceRepository, scheduleEventRepository, userRepository, membershipRepository);
        TenantContext.setCurrentTenantId(CENTER_ID);

        User owner = new User();
        owner.setId(OWNER_ID);
        owner.setPhoneNumber(OWNER_PHONE);
        owner.setRole(Role.OWNER);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(OWNER_PHONE, null, List.of()));

        lenient().when(userRepository.findByPhoneNumber(OWNER_PHONE)).thenReturn(Optional.of(owner));
        lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(OWNER_ID, CENTER_ID)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private User buildTeacher(Long id, String name) {
        User teacher = new User();
        teacher.setId(id);
        teacher.setPhoneNumber("09000000" + id);
        teacher.setFullName(name);
        teacher.setRole(Role.TEACHER);
        return teacher;
    }

    private Center buildCenter() {
        Center center = new Center();
        center.setId(CENTER_ID);
        return center;
    }

    private Class buildClass(String name) {
        Class clazz = new Class();
        clazz.setId(1L);
        clazz.setName(name);
        clazz.setCenter(buildCenter());
        return clazz;
    }

    private ScheduleEvent buildScheduleEvent(Long id, User teacher, LocalTime start, LocalTime end, LocalDate date) {
        return ScheduleEvent.builder()
                .id(id)
                .center(buildCenter())
                .clazz(buildClass("TOEIC 650+"))
                .teacherUser(teacher)
                .eventDate(date)
                .startTime(start)
                .endTime(end)
                .eventType(ScheduleEventType.LESSON)
                .status(ScheduleEventStatus.SCHEDULED)
                .build();
    }

    // ==================== SCHEDULE-DRIVEN FIND ALL TESTS ====================

    @Test
    @DisplayName("CASE 1: Teacher has no scheduled sessions on date → does not appear in attendance list")
    void findAll_whenNoSessionsOnDate_shouldReturnEmptyList() {
        LocalDate date = LocalDate.of(2026, 8, 10);
        when(scheduleEventRepository.findAllByCenter_IdAndEventDateAndStatusNotOrderByStartTimeAsc(
                CENTER_ID, date, ScheduleEventStatus.CANCELLED))
                .thenReturn(List.of());

        List<TeacherAttendanceResponse> responses = service.findAll(null, date, null, null);

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("CASE 2: Teacher has 1 session on date → returns 1 attendance obligation")
    void findAll_whenOneSession_shouldReturnOneObligation() {
        LocalDate date = LocalDate.of(2026, 8, 10);
        User teacher = buildTeacher(TEACHER_ID, "Teacher John");
        ScheduleEvent event = buildScheduleEvent(SCHEDULE_EVENT_ID_1, teacher, LocalTime.of(19, 45), LocalTime.of(21, 15), date);

        when(scheduleEventRepository.findAllByCenter_IdAndEventDateAndStatusNotOrderByStartTimeAsc(
                CENTER_ID, date, ScheduleEventStatus.CANCELLED))
                .thenReturn(List.of(event));
        when(teacherAttendanceRepository.findAllByCenter_IdAndScheduleEvent_IdIn(CENTER_ID, List.of(SCHEDULE_EVENT_ID_1)))
                .thenReturn(List.of());

        List<TeacherAttendanceResponse> responses = service.findAll(null, date, null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getScheduleEventId()).isEqualTo(SCHEDULE_EVENT_ID_1);
        assertThat(responses.get(0).getTeacherUserId()).isEqualTo(TEACHER_ID);
        assertThat(responses.get(0).getStatus()).isNull(); // Unmarked
    }

    @Test
    @DisplayName("CASE 3: Teacher has 2 sessions on same date → returns 2 independent attendance obligations")
    void findAll_whenTeacherHasTwoSessionsOnSameDate_shouldReturnTwoObligations() {
        LocalDate date = LocalDate.of(2026, 8, 10);
        User teacher = buildTeacher(TEACHER_ID, "Teacher John");

        ScheduleEvent event1 = buildScheduleEvent(SCHEDULE_EVENT_ID_1, teacher, LocalTime.of(8, 0), LocalTime.of(9, 30), date);
        ScheduleEvent event2 = buildScheduleEvent(SCHEDULE_EVENT_ID_2, teacher, LocalTime.of(19, 45), LocalTime.of(21, 15), date);

        when(scheduleEventRepository.findAllByCenter_IdAndEventDateAndStatusNotOrderByStartTimeAsc(
                CENTER_ID, date, ScheduleEventStatus.CANCELLED))
                .thenReturn(List.of(event1, event2));
        when(teacherAttendanceRepository.findAllByCenter_IdAndScheduleEvent_IdIn(CENTER_ID, List.of(SCHEDULE_EVENT_ID_1, SCHEDULE_EVENT_ID_2)))
                .thenReturn(List.of());

        List<TeacherAttendanceResponse> responses = service.findAll(null, date, null, null);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getScheduleEventId()).isEqualTo(SCHEDULE_EVENT_ID_1);
        assertThat(responses.get(1).getScheduleEventId()).isEqualTo(SCHEDULE_EVENT_ID_2);
    }

    @Test
    @DisplayName("CASE 7: Teacher is PRESENT in morning session and LATE in evening session → 2 status values stored independently")
    void mark_whenTwoSessionsSameDay_shouldStoreStatusesIndependently() {
        LocalDate date = LocalDate.of(2026, 8, 10);
        User teacher = buildTeacher(TEACHER_ID, "Teacher John");

        ScheduleEvent event1 = buildScheduleEvent(SCHEDULE_EVENT_ID_1, teacher, LocalTime.of(8, 0), LocalTime.of(9, 30), date);
        ScheduleEvent event2 = buildScheduleEvent(SCHEDULE_EVENT_ID_2, teacher, LocalTime.of(19, 45), LocalTime.of(21, 15), date);

        when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
        when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID)).thenReturn(true);
        when(scheduleEventRepository.findById(SCHEDULE_EVENT_ID_1)).thenReturn(Optional.of(event1));
        when(scheduleEventRepository.findById(SCHEDULE_EVENT_ID_2)).thenReturn(Optional.of(event2));

        when(teacherAttendanceRepository.findByScheduleEvent_IdAndTeacherUser_Id(SCHEDULE_EVENT_ID_1, TEACHER_ID))
                .thenReturn(Optional.empty());
        when(teacherAttendanceRepository.findByScheduleEvent_IdAndTeacherUser_Id(SCHEDULE_EVENT_ID_2, TEACHER_ID))
                .thenReturn(Optional.empty());

        when(teacherAttendanceRepository.save(any(TeacherAttendance.class))).thenAnswer(inv -> inv.getArgument(0));

        TeacherAttendanceMarkRequest.Item item1 = TeacherAttendanceMarkRequest.Item.builder()
                .scheduleEventId(SCHEDULE_EVENT_ID_1)
                .teacherUserId(TEACHER_ID)
                .status(TeacherAttendanceStatus.PRESENT)
                .build();

        TeacherAttendanceMarkRequest.Item item2 = TeacherAttendanceMarkRequest.Item.builder()
                .scheduleEventId(SCHEDULE_EVENT_ID_2)
                .teacherUserId(TEACHER_ID)
                .status(TeacherAttendanceStatus.LATE)
                .build();

        List<TeacherAttendanceResponse> responses = service.mark(
                TeacherAttendanceMarkRequest.builder()
                        .date(date)
                        .records(List.of(item1, item2))
                        .build());

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getStatus()).isEqualTo(TeacherAttendanceStatus.PRESENT);
        assertThat(responses.get(1).getStatus()).isEqualTo(TeacherAttendanceStatus.LATE);
        verify(teacherAttendanceRepository, times(2)).save(any(TeacherAttendance.class));
    }

    @Test
    @DisplayName("CASE 8: Unmarked session → status is null, not auto-marked ABSENT")
    void findAll_whenUnmarkedSession_statusShouldBeNull() {
        LocalDate date = LocalDate.of(2026, 8, 10);
        User teacher = buildTeacher(TEACHER_ID, "Teacher John");
        ScheduleEvent event = buildScheduleEvent(SCHEDULE_EVENT_ID_1, teacher, LocalTime.of(19, 45), LocalTime.of(21, 15), date);

        when(scheduleEventRepository.findAllByCenter_IdAndEventDateAndStatusNotOrderByStartTimeAsc(
                CENTER_ID, date, ScheduleEventStatus.CANCELLED))
                .thenReturn(List.of(event));
        when(teacherAttendanceRepository.findAllByCenter_IdAndScheduleEvent_IdIn(CENTER_ID, List.of(SCHEDULE_EVENT_ID_1)))
                .thenReturn(List.of());

        List<TeacherAttendanceResponse> responses = service.findAll(null, date, null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isNull();
    }

    @Test
    @DisplayName("mark: non-OWNER caller → AccessDeniedException")
    void mark_whenCallerIsNotOwner_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEACHER_PHONE, null, List.of()));
        User teacher = buildTeacher(TEACHER_ID, "Teacher John");
        when(userRepository.findByPhoneNumber(TEACHER_PHONE)).thenReturn(Optional.of(teacher));

        TeacherAttendanceMarkRequest.Item item = TeacherAttendanceMarkRequest.Item.builder()
                .teacherUserId(TEACHER_ID)
                .status(TeacherAttendanceStatus.PRESENT)
                .build();

        assertThatThrownBy(() -> service.mark(
                TeacherAttendanceMarkRequest.builder()
                        .date(LocalDate.of(2026, 7, 16))
                        .records(List.of(item))
                        .build()))
                .isInstanceOf(AccessDeniedException.class);
    }
}
