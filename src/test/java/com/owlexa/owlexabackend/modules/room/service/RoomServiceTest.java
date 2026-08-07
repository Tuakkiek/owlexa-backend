package com.owlexa.owlexabackend.modules.room.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEvent;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventStatus;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventType;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleRecurringRule;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleEventRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRecurringRuleRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.room.dto.request.RoomRequest;
import com.owlexa.owlexabackend.modules.room.dto.response.RoomDeleteValidationResponse;
import com.owlexa.owlexabackend.modules.room.dto.response.RoomDependencyDto;
import com.owlexa.owlexabackend.modules.room.dto.response.RoomResponse;
import com.owlexa.owlexabackend.modules.room.dto.response.RoomScheduleSummaryResponse;
import com.owlexa.owlexabackend.modules.room.entity.Room;
import com.owlexa.owlexabackend.modules.room.repository.RoomRepository;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock private RoomRepository roomRepository;
    @Mock private CenterRepository centerRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private ScheduleRecurringRuleRepository scheduleRecurringRuleRepository;
    @Mock private ScheduleEventRepository scheduleEventRepository;

    private RoomService service;

    private static final String OWNER_PHONE = "0900000001";
    private static final Long OWNER_ID = 1L;
    private static final Long CENTER_ID = 10L;
    private static final Long ROOM_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new RoomService(
                roomRepository,
                centerRepository,
                userRepository,
                membershipRepository,
                scheduleRepository,
                scheduleRecurringRuleRepository,
                scheduleEventRepository
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

    @Test
    @DisplayName("create: valid request creates room")
    void create_whenValid_shouldCreateRoom() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter(CENTER_ID)));
        when(roomRepository.existsByCodeAndCenter_Id("P201", CENTER_ID)).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            room.setId(ROOM_ID);
            return room;
        });

        RoomResponse response = service.create(buildRequest());

        assertThat(response.getId()).isEqualTo(ROOM_ID);
        assertThat(response.getCode()).isEqualTo("P201");
        assertThat(response.getCenterId()).isEqualTo(CENTER_ID);
        assertThat(response.getUsageCount()).isZero();
        assertThat(response.getIsInUse()).isFalse();
    }

    @Test
    @DisplayName("create: duplicate code throws")
    void create_whenDuplicateCode_shouldThrowDuplicate() {
        when(roomRepository.existsByCodeAndCenter_Id("P201", CENTER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.create(buildRequest()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("create: non owner throws")
    void create_whenNotOwner_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("0900000002", null, List.of())
        );
        User teacher = new User();
        teacher.setId(2L);
        teacher.setPhoneNumber("0900000002");
        teacher.setRole(Role.TEACHER);
        when(userRepository.findByPhoneNumber("0900000002")).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.create(buildRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("findAll: returns active rooms with usage status")
    void findAll_shouldReturnActiveRoomsWithUsageStatus() {
        Room room = buildRoom(ROOM_ID, CENTER_ID, "P201", "Room 201");
        when(roomRepository.findAllByCenter_IdAndIsActiveTrue(CENTER_ID)).thenReturn(List.of(room));
        when(scheduleEventRepository.findAllByRoom_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(ROOM_ID, CENTER_ID))
                .thenReturn(List.of(ScheduleEvent.builder()
                        .id(700L)
                        .clazz(Class.builder().name("VSTEP B1").build())
                        .eventDate(LocalDate.of(2026, 8, 10))
                        .startTime(LocalTime.of(19, 45))
                        .endTime(LocalTime.of(21, 15))
                        .eventType(ScheduleEventType.LESSON)
                        .status(ScheduleEventStatus.SCHEDULED)
                        .build()));

        List<RoomResponse> response = service.findAll();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getIsInUse()).isTrue();
        assertThat(response.get(0).getUsageCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findById: room exists in center")
    void findById_whenExists_shouldReturnRoom() {
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID))
                .thenReturn(Optional.of(buildRoom(ROOM_ID, CENTER_ID, "P201", "Room 201")));

        RoomResponse response = service.findById(ROOM_ID);

        assertThat(response.getId()).isEqualTo(ROOM_ID);
        assertThat(response.getCode()).isEqualTo("P201");
    }

    @Test
    @DisplayName("update: valid request updates room")
    void update_whenValid_shouldUpdateRoom() {
        Room existing = buildRoom(ROOM_ID, CENTER_ID, "P201", "Room 201");
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID)).thenReturn(Optional.of(existing));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomResponse response = service.update(ROOM_ID, RoomRequest.builder()
                .code("P201")
                .name("Room 201 Updated")
                .capacity(35)
                .build());

        assertThat(response.getName()).isEqualTo("Room 201 Updated");
        assertThat(response.getCapacity()).isEqualTo(35);
    }

    @Test
    @DisplayName("delete: unused room deletes")
    void delete_whenUnused_shouldDelete() {
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID))
                .thenReturn(Optional.of(buildRoom(ROOM_ID, CENTER_ID, "P201", "Room 201")));

        service.delete(ROOM_ID);
    }

    @Test
    @DisplayName("delete: missing room throws")
    void delete_whenNotFound_shouldThrowResourceNotFound() {
        when(roomRepository.findByIdAndCenter_Id(999L, CENTER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("schedule summary includes only visible legacy schedules and generated events")
    void getScheduleSummary_shouldReturnVisibleUsageSources() {
        Room room = buildRoom(ROOM_ID, CENTER_ID, "P201", "Room 201");
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID)).thenReturn(Optional.of(room));

        Class clazz = Class.builder().name("IELTS 6.5").build();
        User teacher = buildTeacher();
        when(scheduleRepository.findAllByRoom_IdAndCenter_Id(ROOM_ID, CENTER_ID))
                .thenReturn(List.of(Schedule.builder()
                        .id(500L)
                        .clazz(clazz)
                        .teacherUser(teacher)
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .startTime(LocalTime.of(8, 0))
                        .endTime(LocalTime.of(10, 0))
                        .type(ScheduleType.THEORY_CLASS)
                        .build()));
        when(scheduleEventRepository.findAllByRoom_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(ROOM_ID, CENTER_ID))
                .thenReturn(List.of(ScheduleEvent.builder()
                        .id(700L)
                        .clazz(clazz)
                        .teacherUser(teacher)
                        .eventDate(LocalDate.of(2026, 8, 10))
                        .startTime(LocalTime.of(8, 0))
                        .endTime(LocalTime.of(9, 30))
                        .eventType(ScheduleEventType.EXAM)
                        .status(ScheduleEventStatus.SCHEDULED)
                        .build()));

        List<RoomScheduleSummaryResponse> summary = service.getScheduleSummary(ROOM_ID);

        assertThat(summary).hasSize(2);
        assertThat(summary).extracting(RoomScheduleSummaryResponse::getSource)
                .containsExactly("LEGACY", "EVENT");
        assertThat(summary).extracting(RoomScheduleSummaryResponse::getClassName)
                .containsOnly("IELTS 6.5");
        assertThat(summary).extracting(RoomScheduleSummaryResponse::getTeacherName)
                .containsOnly("David Nguyen");
    }

    @Test
    @DisplayName("schedule summary always hides recurring rules")
    void getScheduleSummary_whenRoomHasRule_shouldHideRuleSource() {
        Room room = buildRoom(ROOM_ID, CENTER_ID, "P201", "Room 201");
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID)).thenReturn(Optional.of(room));

        Class clazz = Class.builder().name("TOEIC 650+ T8-2026").build();
        User teacher = buildTeacher();
        ScheduleRecurringRule rule = ScheduleRecurringRule.builder()
                .id(600L)
                .clazz(clazz)
                .teacherUser(teacher)
                .daysOfWeek("1")
                .startTime(LocalTime.of(19, 45))
                .endTime(LocalTime.of(21, 15))
                .type(ScheduleType.THEORY_CLASS)
                .isActive(true)
                .build();
        when(scheduleEventRepository.findAllByRoom_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(ROOM_ID, CENTER_ID))
                .thenReturn(List.of(ScheduleEvent.builder()
                        .id(700L)
                        .clazz(clazz)
                        .recurringRule(rule)
                        .teacherUser(teacher)
                        .eventDate(LocalDate.of(2026, 7, 27))
                        .startTime(LocalTime.of(19, 45))
                        .endTime(LocalTime.of(21, 15))
                        .eventType(ScheduleEventType.LESSON)
                        .status(ScheduleEventStatus.SCHEDULED)
                        .build()));

        List<RoomScheduleSummaryResponse> summary = service.getScheduleSummary(ROOM_ID);

        assertThat(summary).hasSize(1);
        assertThat(summary.get(0).getSource()).isEqualTo("EVENT");
        assertThat(summary.get(0).getEventDate()).isEqualTo("2026-07-27");
    }

    @Test
    @DisplayName("validateDelete: recurring rule usage blocks delete")
    void validateDelete_whenHasScheduleRules_shouldReturnCannotDelete() {
        Room room = buildRoom(ROOM_ID, CENTER_ID, "P201", "Room 201");
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID)).thenReturn(Optional.of(room));
        when(scheduleRecurringRuleRepository.findAllByRoom_IdAndCenter_IdOrderByStartDateAscStartTimeAsc(ROOM_ID, CENTER_ID))
                .thenReturn(List.of(ScheduleRecurringRule.builder()
                        .id(600L)
                        .clazz(Class.builder().name("VSTEP B1").build())
                        .daysOfWeek("1,3,5")
                        .startTime(LocalTime.of(19, 45))
                        .endTime(LocalTime.of(21, 15))
                        .type(ScheduleType.THEORY_CLASS)
                        .build()));

        RoomDeleteValidationResponse validation = service.validateDelete(ROOM_ID);

        assertThat(validation.isCanDelete()).isFalse();
        assertThat(validation.getDependencies()).hasSize(3);
        assertThat(validation.getDependencies()).extracting(RoomDependencyDto::getSource)
                .containsOnly("RULE");
    }

    @Test
    @DisplayName("delete: rule usage throws")
    void delete_whenHasScheduleRules_shouldThrowBusinessRuleException() {
        Room room = buildRoom(ROOM_ID, CENTER_ID, "P201", "Room 201");
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID)).thenReturn(Optional.of(room));
        when(scheduleRecurringRuleRepository.existsByRoom_IdAndCenter_Id(ROOM_ID, CENTER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(ROOM_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("dang duoc su dung trong lich hoc");
    }

    private Center buildCenter(Long id) {
        Center center = new Center();
        center.setId(id);
        return center;
    }

    private Room buildRoom(Long id, Long centerId, String code, String name) {
        Room room = new Room();
        room.setId(id);
        room.setCode(code);
        room.setName(name);
        room.setCapacity(30);
        room.setCenter(buildCenter(centerId));
        room.setIsActive(true);
        return room;
    }

    private RoomRequest buildRequest() {
        return RoomRequest.builder()
                .code("P201")
                .name("Room 201")
                .capacity(30)
                .build();
    }

    private User buildTeacher() {
        User teacher = new User();
        teacher.setFullName("David Nguyen");
        teacher.setRole(Role.TEACHER);
        return teacher;
    }
}
