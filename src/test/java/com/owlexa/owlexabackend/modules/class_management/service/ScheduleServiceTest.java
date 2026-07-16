package com.owlexa.owlexabackend.modules.class_management.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.modules.class_management.dto.request.ScheduleRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.response.ScheduleResponse;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.room.entity.Room;
import com.owlexa.owlexabackend.modules.room.repository.RoomRepository;
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

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ClassRepository classRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private RoomRepository roomRepository;

    private ScheduleService service;

    private static final String OWNER_PHONE = "0900000001";
    private static final String TEACHER_PHONE = "0900000002";
    private static final Long OWNER_ID = 1L;
    private static final Long TEACHER_ID = 2L;
    private static final Long CENTER_ID = 10L;
    private static final Long OTHER_CENTER_ID = 99L;
    private static final Long CLASS_ID = 50L;
    private static final Long SCHEDULE_ID = 500L;
    private static final Long SCHEDULE_TEACHER_ID = 200L;
    private static final Long ROOM_ID = 10L;

    @BeforeEach
    void setUp() {
        service = new ScheduleService(
                userRepository, classRepository, membershipRepository,
                scheduleRepository, classEnrollmentRepository, roomRepository
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

    private Class buildClass(Long centerId) {
        Center center = new Center();
        center.setId(centerId);
        Class clazz = new Class();
        clazz.setId(CLASS_ID);
        clazz.setName("Class A");
        clazz.setCenter(center);
        return clazz;
    }

    private User buildTeacher(Long id) {
        User teacher = new User();
        teacher.setId(id);
        teacher.setPhoneNumber("09" + String.format("%08d", id));
        teacher.setFullName("Teacher " + id);
        teacher.setRole(Role.TEACHER);
        return teacher;
    }

    private Room buildRoom(Long id, Long centerId) {
        Center center = new Center();
        center.setId(centerId);
        Room room = new Room();
        room.setId(id);
        room.setCode("R" + id);
        room.setName("Room " + id);
        room.setCenter(center);
        room.setIsActive(true);
        return room;
    }

    private Schedule buildSchedule(Long centerId) {
        Center center = new Center();
        center.setId(centerId);
        Schedule schedule = new Schedule();
        schedule.setId(SCHEDULE_ID);
        schedule.setCenter(center);
        schedule.setClazz(buildClass(centerId));
        schedule.setTeacherUser(buildTeacher(SCHEDULE_TEACHER_ID));
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(10, 0));
        schedule.setRoom(buildRoom(ROOM_ID, centerId));
        schedule.setActive(true);
        return schedule;
    }

    private ScheduleRequest buildCreateRequest() {
        return ScheduleRequest.builder()
                .teacherUserId(SCHEDULE_TEACHER_ID)
                .roomId(ROOM_ID)
                .dayOfWeek(1)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .build();
    }

    @Test
    @DisplayName("create: OWNER + teacher thuộc center + room hợp lệ + không trùng lịch → tạo schedule")
    void create_whenValid_shouldCreateSchedule() {
        Class clazz = buildClass(CENTER_ID);
        Room room = buildRoom(ROOM_ID, CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID)).thenReturn(Optional.of(room));
        when(userRepository.findById(SCHEDULE_TEACHER_ID)).thenReturn(Optional.of(buildTeacher(SCHEDULE_TEACHER_ID)));
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(SCHEDULE_TEACHER_ID, CENTER_ID, Role.TEACHER))
                .thenReturn(Optional.of(new com.owlexa.owlexabackend.modules.user.entity.Membership()));
        when(scheduleRepository.existsByClazz_IdAndDayOfWeekAndStartTimeAndCenter_Id(
                any(), any(), any(), any()))
                .thenReturn(false);
        when(scheduleRepository.countOverlappingTeacherSchedules(any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(scheduleRepository.countOverlappingRoomSchedules(any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule s = invocation.getArgument(0);
            s.setId(SCHEDULE_ID);
            return s;
        });

        ScheduleResponse response = service.create(CLASS_ID, buildCreateRequest());

        assertThat(response.getId()).isEqualTo(SCHEDULE_ID);
        assertThat(response.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(response.getRoomName()).isEqualTo("Room " + ROOM_ID);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("create: class thuộc center khác → TenancyViolationException")
    void create_whenClassInOtherCenter_shouldThrowTenancyViolation() {
        Class clazz = buildClass(OTHER_CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));

        assertThatThrownBy(() -> service.create(CLASS_ID, buildCreateRequest()))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("create: user không phải TEACHER → BadRequestException")
    void create_whenUserIsNotTeacher_shouldThrowBadRequest() {
        Class clazz = buildClass(CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        User notTeacher = buildTeacher(SCHEDULE_TEACHER_ID);
        notTeacher.setRole(Role.STUDENT);
        when(userRepository.findById(SCHEDULE_TEACHER_ID)).thenReturn(Optional.of(notTeacher));

        assertThatThrownBy(() -> service.create(CLASS_ID, buildCreateRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a TEACHER");
    }

    @Test
    @DisplayName("create: teacher không thuộc center → BadRequestException")
    void create_whenTeacherNotInCenter_shouldThrowBadRequest() {
        Class clazz = buildClass(CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(SCHEDULE_TEACHER_ID)).thenReturn(Optional.of(buildTeacher(SCHEDULE_TEACHER_ID)));
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(SCHEDULE_TEACHER_ID, CENTER_ID, Role.TEACHER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(CLASS_ID, buildCreateRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not member of this center");
    }

    @Test
    @DisplayName("create: startTime >= endTime → BadRequestException")
    void create_whenStartTimeNotBeforeEndTime_shouldThrowBadRequest() {
        Class clazz = buildClass(CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(SCHEDULE_TEACHER_ID)).thenReturn(Optional.of(buildTeacher(SCHEDULE_TEACHER_ID)));
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(SCHEDULE_TEACHER_ID, CENTER_ID, Role.TEACHER))
                .thenReturn(Optional.of(new com.owlexa.owlexabackend.modules.user.entity.Membership()));
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID)).thenReturn(Optional.of(buildRoom(ROOM_ID, CENTER_ID)));

        ScheduleRequest bad = ScheduleRequest.builder()
                .teacherUserId(SCHEDULE_TEACHER_ID)
                .roomId(ROOM_ID)
                .dayOfWeek(1)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(8, 0))
                .build();

        assertThatThrownBy(() -> service.create(CLASS_ID, bad))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("startTime must be before endTime");
    }

    @Test
    @DisplayName("create: schedule đã tồn tại cho class cùng day + time → DuplicateResourceException")
    void create_whenScheduleConflict_shouldThrowDuplicate() {
        Class clazz = buildClass(CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(SCHEDULE_TEACHER_ID)).thenReturn(Optional.of(buildTeacher(SCHEDULE_TEACHER_ID)));
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(SCHEDULE_TEACHER_ID, CENTER_ID, Role.TEACHER))
                .thenReturn(Optional.of(new com.owlexa.owlexabackend.modules.user.entity.Membership()));
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID)).thenReturn(Optional.of(buildRoom(ROOM_ID, CENTER_ID)));
        when(scheduleRepository.existsByClazz_IdAndDayOfWeekAndStartTimeAndCenter_Id(
                any(), any(), any(), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(CLASS_ID, buildCreateRequest()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("findAllByClass: trả về tất cả schedule trong class")
    void findAllByClass_shouldReturnSchedules() {
        Class clazz = buildClass(CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(scheduleRepository.findAllByClazz_IdAndCenter_Id(CLASS_ID, CENTER_ID))
                .thenReturn(List.of(buildSchedule(CENTER_ID), buildSchedule(CENTER_ID)));

        List<ScheduleResponse> response = service.findAllByClass(CLASS_ID);

        assertThat(response).hasSize(2);
    }

    @Test
    @DisplayName("findAllByTeacher: trả về tất cả schedule của teacher")
    void findAllByTeacher_shouldReturnSchedules() {
        when(userRepository.findById(SCHEDULE_TEACHER_ID)).thenReturn(Optional.of(buildTeacher(SCHEDULE_TEACHER_ID)));
        when(scheduleRepository.findAllByTeacherUser_IdAndCenter_Id(SCHEDULE_TEACHER_ID, CENTER_ID))
                .thenReturn(List.of(buildSchedule(CENTER_ID)));

        List<ScheduleResponse> response = service.findAllByTeacher(SCHEDULE_TEACHER_ID);

        assertThat(response).hasSize(1);
    }

    @Test
    @DisplayName("findMySchedules: caller không phải TEACHER → AccessDeniedException")
    void findMySchedules_whenCallerIsNotTeacher_shouldThrowAccessDenied() {
        assertThatThrownBy(() -> service.findMySchedules())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only TEACHER");
    }

    @Test
    @DisplayName("findAllForOwner: caller không phải OWNER → AccessDeniedException")
    void findAllForOwner_whenCallerIsNotOwner_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEACHER_PHONE, null, List.of())
        );
        User teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setPhoneNumber(TEACHER_PHONE);
        teacher.setRole(Role.TEACHER);
        when(userRepository.findByPhoneNumber(TEACHER_PHONE)).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.findAllForOwner())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only OWNER");
    }

    @Test
    @DisplayName("update: schedule thuộc center khác → TenancyViolationException")
    void update_whenScheduleInOtherCenter_shouldThrowTenancyViolation() {
        Schedule existing = buildSchedule(OTHER_CENTER_ID);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(CLASS_ID, SCHEDULE_ID, buildCreateRequest()))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("update: teacher mới không thuộc center → BadRequestException")
    void update_whenNewTeacherNotInCenter_shouldThrowBadRequest() {
        Schedule existing = buildSchedule(CENTER_ID);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(existing));
        when(userRepository.findById(SCHEDULE_TEACHER_ID)).thenReturn(Optional.of(buildTeacher(SCHEDULE_TEACHER_ID)));
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(SCHEDULE_TEACHER_ID, CENTER_ID, Role.TEACHER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(CLASS_ID, SCHEDULE_ID, buildCreateRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a member of this center");
    }

    @Test
    @DisplayName("update: hợp lệ → cập nhật schedule")
    void update_whenValid_shouldUpdateSchedule() {
        Schedule existing = buildSchedule(CENTER_ID);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(existing));
        when(userRepository.findById(SCHEDULE_TEACHER_ID)).thenReturn(Optional.of(buildTeacher(SCHEDULE_TEACHER_ID)));
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(SCHEDULE_TEACHER_ID, CENTER_ID, Role.TEACHER))
                .thenReturn(Optional.of(new com.owlexa.owlexabackend.modules.user.entity.Membership()));
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID)).thenReturn(Optional.of(buildRoom(ROOM_ID, CENTER_ID)));
        when(scheduleRepository.countOverlappingTeacherSchedules(any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(scheduleRepository.countOverlappingRoomSchedules(any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleRequest req = ScheduleRequest.builder()
                .teacherUserId(SCHEDULE_TEACHER_ID)
                .roomId(ROOM_ID)
                .dayOfWeek(2)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .build();

        ScheduleResponse response = service.update(CLASS_ID, SCHEDULE_ID, req);

        assertThat(response.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(response.getRoomName()).isEqualTo("Room " + ROOM_ID);
    }

    @Test
    @DisplayName("delete: schedule thuộc center khác → TenancyViolationException")
    void delete_whenScheduleInOtherCenter_shouldThrowTenancyViolation() {
        Schedule existing = buildSchedule(OTHER_CENTER_ID);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.delete(SCHEDULE_ID))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("delete: hợp lệ → xóa")
    void delete_whenValid_shouldDeleteSchedule() {
        Schedule existing = buildSchedule(CENTER_ID);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(existing));

        service.delete(SCHEDULE_ID);

        org.mockito.Mockito.verify(scheduleRepository).delete(existing);
    }

    @Test
    @DisplayName("toggleActive: schedule active → set inactive")
    void toggleActive_shouldFlipActive() {
        Schedule existing = buildSchedule(CENTER_ID);
        existing.setActive(true);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(existing));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleResponse response = service.toggleActive(SCHEDULE_ID);

        assertThat(existing.isActive()).isFalse();
        assertThat(response.isActive()).isFalse();
    }

    @Test
    @DisplayName("create: teacher overlap → BusinessRuleException")
    void create_whenTeacherOverlap_shouldThrowBusinessRule() {
        Class clazz = buildClass(CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(SCHEDULE_TEACHER_ID)).thenReturn(Optional.of(buildTeacher(SCHEDULE_TEACHER_ID)));
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(SCHEDULE_TEACHER_ID, CENTER_ID, Role.TEACHER))
                .thenReturn(Optional.of(new com.owlexa.owlexabackend.modules.user.entity.Membership()));
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID)).thenReturn(Optional.of(buildRoom(ROOM_ID, CENTER_ID)));
        when(scheduleRepository.existsByClazz_IdAndDayOfWeekAndStartTimeAndCenter_Id(any(), any(), any(), any())).thenReturn(false);
        when(scheduleRepository.countOverlappingTeacherSchedules(any(), any(), any(), any(), any(), any())).thenReturn(1L);

        assertThatThrownBy(() -> service.create(CLASS_ID, buildCreateRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Teacher has an overlapping schedule");
    }

    @Test
    @DisplayName("create: room overlap → BusinessRuleException")
    void create_whenRoomOverlap_shouldThrowBusinessRule() {
        Class clazz = buildClass(CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(SCHEDULE_TEACHER_ID)).thenReturn(Optional.of(buildTeacher(SCHEDULE_TEACHER_ID)));
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(SCHEDULE_TEACHER_ID, CENTER_ID, Role.TEACHER))
                .thenReturn(Optional.of(new com.owlexa.owlexabackend.modules.user.entity.Membership()));
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID)).thenReturn(Optional.of(buildRoom(ROOM_ID, CENTER_ID)));
        when(scheduleRepository.existsByClazz_IdAndDayOfWeekAndStartTimeAndCenter_Id(any(), any(), any(), any())).thenReturn(false);
        when(scheduleRepository.countOverlappingTeacherSchedules(any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(scheduleRepository.countOverlappingRoomSchedules(any(), any(), any(), any(), any(), any())).thenReturn(1L);

        assertThatThrownBy(() -> service.create(CLASS_ID, buildCreateRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Room is already booked");
    }

    @Test
    @DisplayName("update: teacher overlap (exclude self) → BusinessRuleException")
    void update_whenTeacherOverlap_shouldThrowBusinessRule() {
        Schedule existing = buildSchedule(CENTER_ID);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(existing));
        when(userRepository.findById(SCHEDULE_TEACHER_ID)).thenReturn(Optional.of(buildTeacher(SCHEDULE_TEACHER_ID)));
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(SCHEDULE_TEACHER_ID, CENTER_ID, Role.TEACHER))
                .thenReturn(Optional.of(new com.owlexa.owlexabackend.modules.user.entity.Membership()));
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID)).thenReturn(Optional.of(buildRoom(ROOM_ID, CENTER_ID)));
        when(scheduleRepository.countOverlappingTeacherSchedules(any(), any(), any(), any(), any(), any())).thenReturn(1L);

        assertThatThrownBy(() -> service.update(CLASS_ID, SCHEDULE_ID, buildCreateRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Teacher has an overlapping schedule");
    }

    @Test
    @DisplayName("create: TenantContext null → BadRequestException")
    void create_whenTenantContextIsNull_shouldThrowBadRequest() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.create(CLASS_ID, buildCreateRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tenant context");
    }
}