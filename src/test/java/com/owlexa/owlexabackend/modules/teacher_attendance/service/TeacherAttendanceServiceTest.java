package com.owlexa.owlexabackend.modules.teacher_attendance.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherAttendanceServiceTest {

    @Mock private TeacherAttendanceRepository teacherAttendanceRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;

    private TeacherAttendanceService service;

    private static final String OWNER_PHONE = "0900000000";
    private static final Long OWNER_ID = 1L;
    private static final Long CENTER_ID = 10L;
    private static final Long TEACHER_ID = 50L;
    private static final String TEACHER_PHONE = "0900000050";
    private static final Long ATTENDANCE_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new TeacherAttendanceService(
                teacherAttendanceRepository, userRepository, membershipRepository);
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

    private User buildTeacher() {
        User teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setPhoneNumber(TEACHER_PHONE);
        teacher.setFullName("Teacher John");
        teacher.setRole(Role.TEACHER);
        return teacher;
    }

    private TeacherAttendance buildAttendance() {
        Center center = new Center();
        center.setId(CENTER_ID);

        TeacherAttendance attendance = new TeacherAttendance();
        attendance.setId(ATTENDANCE_ID);
        attendance.setTeacherUser(buildTeacher());
        attendance.setCenter(center);
        attendance.setStatus(TeacherAttendanceStatus.PRESENT);
        attendance.setDate(LocalDate.of(2026, 7, 16));
        return attendance;
    }

    // ==================== MARK TESTS ====================

    @Test
    @DisplayName("mark: OWNER marks teacher attendance → creates records")
    void mark_whenValid_shouldCreateAttendance() {
        User teacher = buildTeacher();
        when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
        when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID)).thenReturn(true);
        when(teacherAttendanceRepository.findByTeacherUser_IdAndDate(TEACHER_ID, LocalDate.of(2026, 7, 16)))
                .thenReturn(Optional.empty());
        when(teacherAttendanceRepository.save(any(TeacherAttendance.class))).thenAnswer(inv -> {
            TeacherAttendance a = inv.getArgument(0);
            a.setId(ATTENDANCE_ID);
            return a;
        });

        TeacherAttendanceMarkRequest.Item item = TeacherAttendanceMarkRequest.Item.builder()
                .teacherUserId(TEACHER_ID)
                .status(TeacherAttendanceStatus.PRESENT)
                .note("on time")
                .build();

        List<TeacherAttendanceResponse> responses = service.mark(
                TeacherAttendanceMarkRequest.builder()
                        .date(LocalDate.of(2026, 7, 16))
                        .records(List.of(item))
                        .build());

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTeacherUserId()).isEqualTo(TEACHER_ID);
        assertThat(responses.get(0).getStatus()).isEqualTo(TeacherAttendanceStatus.PRESENT);
        verify(teacherAttendanceRepository).save(any(TeacherAttendance.class));
    }

    @Test
    @DisplayName("mark: existing attendance → update instead of duplicate")
    void mark_whenAttendanceExists_shouldUpdate() {
        User teacher = buildTeacher();
        when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
        when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID)).thenReturn(true);

        TeacherAttendance existing = buildAttendance();
        existing.setStatus(TeacherAttendanceStatus.ABSENT);
        when(teacherAttendanceRepository.findByTeacherUser_IdAndDate(TEACHER_ID, LocalDate.of(2026, 7, 16)))
                .thenReturn(Optional.of(existing));
        when(teacherAttendanceRepository.save(any(TeacherAttendance.class))).thenAnswer(inv -> inv.getArgument(0));

        TeacherAttendanceMarkRequest.Item item = TeacherAttendanceMarkRequest.Item.builder()
                .teacherUserId(TEACHER_ID)
                .status(TeacherAttendanceStatus.PRESENT)
                .build();

        List<TeacherAttendanceResponse> responses = service.mark(
                TeacherAttendanceMarkRequest.builder()
                        .date(LocalDate.of(2026, 7, 16))
                        .records(List.of(item))
                        .build());

        assertThat(responses).hasSize(1);
        assertThat(existing.getStatus()).isEqualTo(TeacherAttendanceStatus.PRESENT);
        assertThat(existing.getStatus()).isNotEqualTo(TeacherAttendanceStatus.ABSENT);
    }

    @Test
    @DisplayName("mark: user is not TEACHER role → BadRequestException")
    void mark_whenUserIsNotTeacher_shouldThrowBadRequest() {
        User notTeacher = buildTeacher();
        notTeacher.setRole(Role.STUDENT);
        when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(notTeacher));

        TeacherAttendanceMarkRequest.Item item = TeacherAttendanceMarkRequest.Item.builder()
                .teacherUserId(TEACHER_ID)
                .status(TeacherAttendanceStatus.PRESENT)
                .build();

        assertThatThrownBy(() -> service.mark(
                TeacherAttendanceMarkRequest.builder()
                        .date(LocalDate.of(2026, 7, 16))
                        .records(List.of(item))
                        .build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a TEACHER");
    }

    @Test
    @DisplayName("mark: teacher not member of center → BadRequestException")
    void mark_whenTeacherNotCenterMember_shouldThrowBadRequest() {
        User teacher = buildTeacher();
        when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
        when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID)).thenReturn(false);

        TeacherAttendanceMarkRequest.Item item = TeacherAttendanceMarkRequest.Item.builder()
                .teacherUserId(TEACHER_ID)
                .status(TeacherAttendanceStatus.PRESENT)
                .build();

        assertThatThrownBy(() -> service.mark(
                TeacherAttendanceMarkRequest.builder()
                        .date(LocalDate.of(2026, 7, 16))
                        .records(List.of(item))
                        .build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a member");
    }

    @Test
    @DisplayName("mark: non-OWNER caller → AccessDeniedException")
    void mark_whenCallerIsNotOwner_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEACHER_PHONE, null, List.of()));
        User teacher = buildTeacher();
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
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only OWNER");
    }

    @Test
    @DisplayName("mark: teacher not found → ResourceNotFoundException")
    void mark_whenTeacherNotFound_shouldThrowResourceNotFound() {
        when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.empty());

        TeacherAttendanceMarkRequest.Item item = TeacherAttendanceMarkRequest.Item.builder()
                .teacherUserId(TEACHER_ID)
                .status(TeacherAttendanceStatus.PRESENT)
                .build();

        assertThatThrownBy(() -> service.mark(
                TeacherAttendanceMarkRequest.builder()
                        .date(LocalDate.of(2026, 7, 16))
                        .records(List.of(item))
                        .build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("update: OWNER updates attendance status → success")
    void update_whenValid_shouldUpdate() {
        TeacherAttendance attendance = buildAttendance();
        when(teacherAttendanceRepository.findById(ATTENDANCE_ID)).thenReturn(Optional.of(attendance));
        when(teacherAttendanceRepository.save(any(TeacherAttendance.class))).thenAnswer(inv -> inv.getArgument(0));

        TeacherAttendanceResponse response = service.update(ATTENDANCE_ID, TeacherAttendanceStatus.LATE, "15 min late");

        assertThat(response.getStatus()).isEqualTo(TeacherAttendanceStatus.LATE);
        assertThat(response.getNote()).isEqualTo("15 min late");
    }

    @Test
    @DisplayName("update: non-OWNER → AccessDeniedException")
    void update_whenCallerIsNotOwner_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEACHER_PHONE, null, List.of()));
        User teacher = buildTeacher();
        when(userRepository.findByPhoneNumber(TEACHER_PHONE)).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.update(ATTENDANCE_ID, TeacherAttendanceStatus.LATE, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only OWNER");
    }

    @Test
    @DisplayName("update: attendance not found → ResourceNotFoundException")
    void update_whenNotFound_shouldThrowResourceNotFound() {
        when(teacherAttendanceRepository.findById(ATTENDANCE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(ATTENDANCE_ID, TeacherAttendanceStatus.LATE, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("delete: OWNER deletes attendance → success")
    void delete_whenValid_shouldDelete() {
        TeacherAttendance attendance = buildAttendance();
        when(teacherAttendanceRepository.findById(ATTENDANCE_ID)).thenReturn(Optional.of(attendance));

        service.delete(ATTENDANCE_ID);

        verify(teacherAttendanceRepository).delete(attendance);
    }

    @Test
    @DisplayName("delete: non-OWNER → AccessDeniedException")
    void delete_whenCallerIsNotOwner_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEACHER_PHONE, null, List.of()));
        User teacher = buildTeacher();
        when(userRepository.findByPhoneNumber(TEACHER_PHONE)).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.delete(ATTENDANCE_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only OWNER");
    }

    // ==================== FIND TESTS ====================

    @Test
    @DisplayName("findAll: filters by teacherId + date → returns records")
    void findAll_byTeacherAndDate_shouldReturnRecords() {
        when(teacherAttendanceRepository.findByTeacherUser_IdAndDate(TEACHER_ID, LocalDate.of(2026, 7, 16)))
                .thenReturn(Optional.of(buildAttendance()));

        List<TeacherAttendanceResponse> responses = service.findAll(TEACHER_ID, LocalDate.of(2026, 7, 16), null, null);

        assertThat(responses).hasSize(1);
    }

    @Test
    @DisplayName("findAll: filters by center + date (all teachers) → returns records")
    void findAll_byCenterAndDate_shouldReturnRecords() {
        when(teacherAttendanceRepository.findAllByCenter_IdAndDate(CENTER_ID, LocalDate.of(2026, 7, 16)))
                .thenReturn(List.of(buildAttendance()));

        List<TeacherAttendanceResponse> responses = service.findAll(null, LocalDate.of(2026, 7, 16), null, null);

        assertThat(responses).hasSize(1);
    }

    @Test
    @DisplayName("findAll: no filters → returns today's records")
    void findAll_noFilters_shouldReturnTodayRecords() {
        when(teacherAttendanceRepository.findAllByCenter_IdAndDate(CENTER_ID, LocalDate.now()))
                .thenReturn(List.of());

        List<TeacherAttendanceResponse> responses = service.findAll(null, null, null, null);

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("findAll: non-OWNER → AccessDeniedException")
    void findAll_whenCallerIsNotOwner_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEACHER_PHONE, null, List.of()));
        User teacher = buildTeacher();
        when(userRepository.findByPhoneNumber(TEACHER_PHONE)).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.findAll(null, null, null, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("findById: returns single record")
    void findById_whenValid_shouldReturnRecord() {
        when(teacherAttendanceRepository.findById(ATTENDANCE_ID)).thenReturn(Optional.of(buildAttendance()));

        TeacherAttendanceResponse response = service.findById(ATTENDANCE_ID);

        assertThat(response.getId()).isEqualTo(ATTENDANCE_ID);
    }

    // ==================== BATCH MARK TESTS ====================

    @Test
    @DisplayName("mark: batch marks multiple teachers → creates all")
    void mark_whenBatchMultipleTeachers_shouldCreateAll() {
        User teacher1 = buildTeacher();
        User teacher2 = new User();
        teacher2.setId(60L);
        teacher2.setPhoneNumber("0900000060");
        teacher2.setFullName("Teacher Jane");
        teacher2.setRole(Role.TEACHER);

        when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher1));
        when(userRepository.findById(60L)).thenReturn(Optional.of(teacher2));
        when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID)).thenReturn(true);
        when(membershipRepository.existsByUser_IdAndCenter_Id(60L, CENTER_ID)).thenReturn(true);
        when(teacherAttendanceRepository.findByTeacherUser_IdAndDate(any(), any()))
                .thenReturn(Optional.empty());
        when(teacherAttendanceRepository.save(any(TeacherAttendance.class))).thenAnswer(inv -> {
            TeacherAttendance a = inv.getArgument(0);
            a.setId(a.getTeacherUser().getId());
            return a;
        });

        TeacherAttendanceMarkRequest.Item item1 = TeacherAttendanceMarkRequest.Item.builder()
                .teacherUserId(TEACHER_ID).status(TeacherAttendanceStatus.PRESENT).build();
        TeacherAttendanceMarkRequest.Item item2 = TeacherAttendanceMarkRequest.Item.builder()
                .teacherUserId(60L).status(TeacherAttendanceStatus.LEAVE).note("Sick leave").build();

        List<TeacherAttendanceResponse> responses = service.mark(
                TeacherAttendanceMarkRequest.builder()
                        .date(LocalDate.of(2026, 7, 16))
                        .records(List.of(item1, item2))
                        .build());

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getStatus()).isEqualTo(TeacherAttendanceStatus.PRESENT);
        assertThat(responses.get(1).getStatus()).isEqualTo(TeacherAttendanceStatus.LEAVE);
        verify(teacherAttendanceRepository, times(2)).save(any(TeacherAttendance.class));
    }

    @Test
    @DisplayName("mark: OWNER not member of center → AccessDeniedException")
    void mark_whenOwnerNotCenterMember_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner2", null, List.of()));
        User owner2 = new User();
        owner2.setId(99L);
        owner2.setPhoneNumber("owner2");
        owner2.setRole(Role.OWNER);
        when(userRepository.findByPhoneNumber("owner2")).thenReturn(Optional.of(owner2));
        when(membershipRepository.existsByUser_IdAndCenter_Id(99L, CENTER_ID)).thenReturn(false);

        TeacherAttendanceMarkRequest.Item item = TeacherAttendanceMarkRequest.Item.builder()
                .teacherUserId(TEACHER_ID).status(TeacherAttendanceStatus.PRESENT).build();

        assertThatThrownBy(() -> service.mark(
                TeacherAttendanceMarkRequest.builder()
                        .date(LocalDate.of(2026, 7, 16))
                        .records(List.of(item))
                        .build()))
                .isInstanceOf(AccessDeniedException.class);
    }
}
