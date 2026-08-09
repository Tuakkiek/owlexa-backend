package com.owlexa.owlexabackend.modules.class_management.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.modules.class_management.dto.request.ScheduleEventRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.request.ScheduleRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.request.ScheduleRuleRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.response.ScheduleEventResponse;
import com.owlexa.owlexabackend.modules.class_management.dto.response.ScheduleResponse;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEvent;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventStatus;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventType;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleRecurringRule;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleRepeatType;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleEventRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRecurringRuleRepository;
import com.owlexa.owlexabackend.modules.course.entity.Course;
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
class ScheduleServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ClassRepository classRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private ScheduleRecurringRuleRepository scheduleRecurringRuleRepository;
    @Mock private ScheduleEventRepository scheduleEventRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private com.owlexa.owlexabackend.modules.class_management.repository.TeachingTimeSlotRepository timeSlotRepository;

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
        var classLifecycleValidator = new com.owlexa.owlexabackend.modules.class_management.service.validation.ClassLifecycleValidator();
        var timeRangeValidator = new com.owlexa.owlexabackend.modules.class_management.service.validation.TimeRangeValidator();
        var roomConflictValidator = new com.owlexa.owlexabackend.modules.class_management.service.validation.RoomConflictValidator(scheduleRepository);
        var teacherConflictValidator = new com.owlexa.owlexabackend.modules.class_management.service.validation.TeacherConflictValidator(scheduleRepository);
        var studentConflictValidator = new com.owlexa.owlexabackend.modules.class_management.service.validation.StudentConflictValidator(classEnrollmentRepository, scheduleRepository);

        service = new ScheduleService(
                userRepository, classRepository, membershipRepository,
                scheduleRepository, scheduleRecurringRuleRepository, scheduleEventRepository,
                classEnrollmentRepository, roomRepository, timeSlotRepository,
                classLifecycleValidator, timeRangeValidator, roomConflictValidator,
                teacherConflictValidator, studentConflictValidator
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

    private Class buildClassWithCourse(Long centerId, int sessionCount) {
        Class clazz = buildClass(centerId);
        Course course = new Course();
        course.setId(300L);
        course.setCode("VSTEP-B1");
        course.setName("VSTEP B1");
        course.setDefaultSessionCount(sessionCount);
        clazz.setCourse(course);
        clazz.setTeacherUser(buildTeacher(SCHEDULE_TEACHER_ID));
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
        schedule.setType(com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType.THEORY_CLASS);
        return schedule;
    }

    private ScheduleEvent buildScheduleEvent(Long centerId) {
        Center center = new Center();
        center.setId(centerId);
        ScheduleEvent event = new ScheduleEvent();
        event.setId(SCHEDULE_ID);
        event.setCenter(center);
        event.setClazz(buildClass(centerId));
        event.setTeacherUser(buildTeacher(SCHEDULE_TEACHER_ID));
        event.setRoom(buildRoom(ROOM_ID, centerId));
        event.setEventDate(LocalDate.of(2026, 8, 3));
        event.setStartTime(LocalTime.of(8, 0));
        event.setEndTime(LocalTime.of(10, 0));
        event.setLessonNumber(1);
        event.setEventType(ScheduleEventType.LESSON);
        event.setStatus(ScheduleEventStatus.SCHEDULED);
        event.setTitle("Class A");
        return event;
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
        lenient().when(scheduleRepository.findOverlappingTeacherSchedules(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(scheduleRepository.findOverlappingRoomSchedules(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule s = invocation.getArgument(0);
            s.setId(SCHEDULE_ID);
            return s;
        });

        ScheduleResponse response = service.create(CLASS_ID, buildCreateRequest());

        assertThat(response.getId()).isEqualTo(SCHEDULE_ID);
        assertThat(response.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(response.getRoomName()).isEqualTo("Room " + ROOM_ID);
        assertThat(response.getType()).isEqualTo(com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType.THEORY_CLASS);
    }

    @Test
    @DisplayName("updateType: schedule type THEORY_CLASS → set CANCELLED")
    void createRule_shouldCalculateEndDateFromCourseSessionCount() {
        Class clazz = buildClassWithCourse(CENTER_ID, 4);
        Room room = buildRoom(ROOM_ID, CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID)).thenReturn(Optional.of(room));
        when(userRepository.findById(SCHEDULE_TEACHER_ID)).thenReturn(Optional.of(buildTeacher(SCHEDULE_TEACHER_ID)));
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(SCHEDULE_TEACHER_ID, CENTER_ID, Role.TEACHER))
                .thenReturn(Optional.of(new com.owlexa.owlexabackend.modules.user.entity.Membership()));
        when(scheduleRecurringRuleRepository.save(any(ScheduleRecurringRule.class))).thenAnswer(invocation -> {
            ScheduleRecurringRule rule = invocation.getArgument(0);
            rule.setId(700L);
            return rule;
        });
        when(scheduleEventRepository.findAllByRecurringRule_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(700L, CENTER_ID))
                .thenReturn(List.of());

        com.owlexa.owlexabackend.modules.class_management.entity.TeachingTimeSlot timeSlot = com.owlexa.owlexabackend.modules.class_management.entity.TeachingTimeSlot.builder()
                .id(100L)
                .center(clazz.getCenter())
                .name("Ca tối 1")
                .period(com.owlexa.owlexabackend.modules.class_management.entity.TimeSlotPeriod.EVENING)
                .startTime(LocalTime.of(19, 45))
                .endTime(LocalTime.of(21, 15))
                .isActive(true)
                .build();
        when(timeSlotRepository.findByIdAndCenter_Id(100L, CENTER_ID)).thenReturn(Optional.of(timeSlot));

        var response = service.createRule(CLASS_ID, ScheduleRuleRequest.builder()
                .teacherUserId(SCHEDULE_TEACHER_ID)
                .roomId(ROOM_ID)
                .daysOfWeek(List.of(1, 3, 5))
                .startDate(LocalDate.of(2026, 8, 3))
                .timeSlotId(100L)
                .type(ScheduleType.THEORY_CLASS)
                .build());

        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2026, 8, 10));
    }

    @Test
    @DisplayName("createRule: active recurring rule with same teacher and time should conflict")
    void createRule_whenTeacherHasActiveRule_shouldThrowBusinessRule() {
        Class clazz = buildClassWithCourse(CENTER_ID, 4);
        Class otherClazz = buildClassWithCourse(CENTER_ID, 4);
        otherClazz.setId(60L);
        Room room = buildRoom(ROOM_ID, CENTER_ID);
        ScheduleRecurringRule existingRule = ScheduleRecurringRule.builder()
                .id(701L)
                .center(clazz.getCenter())
                .clazz(otherClazz)
                .teacherUser(buildTeacher(SCHEDULE_TEACHER_ID))
                .room(buildRoom(11L, CENTER_ID))
                .repeatType(ScheduleRepeatType.WEEKLY)
                .daysOfWeek("1")
                .startDate(LocalDate.of(2026, 8, 3))
                .endDate(LocalDate.of(2026, 8, 31))
                .startTime(LocalTime.of(19, 30))
                .endTime(LocalTime.of(21, 0))
                .type(ScheduleType.THEORY_CLASS)
                .isActive(true)
                .build();
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID)).thenReturn(Optional.of(room));
        when(userRepository.findById(SCHEDULE_TEACHER_ID)).thenReturn(Optional.of(buildTeacher(SCHEDULE_TEACHER_ID)));
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(SCHEDULE_TEACHER_ID, CENTER_ID, Role.TEACHER))
                .thenReturn(Optional.of(new com.owlexa.owlexabackend.modules.user.entity.Membership()));
        when(scheduleRecurringRuleRepository.findAllByCenter_IdAndIsActiveTrue(CENTER_ID))
                .thenReturn(List.of(existingRule));

        com.owlexa.owlexabackend.modules.class_management.entity.TeachingTimeSlot timeSlot = com.owlexa.owlexabackend.modules.class_management.entity.TeachingTimeSlot.builder()
                .id(100L)
                .center(clazz.getCenter())
                .name("Ca tối 1")
                .period(com.owlexa.owlexabackend.modules.class_management.entity.TimeSlotPeriod.EVENING)
                .startTime(LocalTime.of(19, 45))
                .endTime(LocalTime.of(21, 15))
                .isActive(true)
                .build();
        when(timeSlotRepository.findByIdAndCenter_Id(100L, CENTER_ID)).thenReturn(Optional.of(timeSlot));

        assertThatThrownBy(() -> service.createRule(CLASS_ID, ScheduleRuleRequest.builder()
                .teacherUserId(SCHEDULE_TEACHER_ID)
                .roomId(ROOM_ID)
                .daysOfWeek(List.of(1))
                .startDate(LocalDate.of(2026, 8, 3))
                .timeSlotId(100L)
                .type(ScheduleType.THEORY_CLASS)
                .build()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Giáo viên");
    }

    @Test
    @DisplayName("generateEvents: stop at course session count")
    void generateEvents_shouldStopAtCourseSessionCount() {
        Class clazz = buildClassWithCourse(CENTER_ID, 3);
        Room room = buildRoom(ROOM_ID, CENTER_ID);
        ScheduleRecurringRule rule = ScheduleRecurringRule.builder()
                .id(700L)
                .center(clazz.getCenter())
                .clazz(clazz)
                .teacherUser(buildTeacher(SCHEDULE_TEACHER_ID))
                .room(room)
                .repeatType(ScheduleRepeatType.WEEKLY)
                .daysOfWeek("1,3,5")
                .startDate(LocalDate.of(2026, 8, 3))
                .endDate(LocalDate.of(2026, 8, 7))
                .startTime(LocalTime.of(19, 45))
                .endTime(LocalTime.of(21, 15))
                .type(ScheduleType.THEORY_CLASS)
                .isActive(true)
                .build();
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(scheduleRecurringRuleRepository.findById(700L)).thenReturn(Optional.of(rule));
        when(scheduleEventRepository.existsByRecurringRule_IdAndEventDateAndCenter_Id(700L, LocalDate.of(2026, 8, 3), CENTER_ID)).thenReturn(false);
        when(scheduleEventRepository.existsByRecurringRule_IdAndEventDateAndCenter_Id(700L, LocalDate.of(2026, 8, 5), CENTER_ID)).thenReturn(false);
        when(scheduleEventRepository.existsByRecurringRule_IdAndEventDateAndCenter_Id(700L, LocalDate.of(2026, 8, 7), CENTER_ID)).thenReturn(false);
        when(scheduleEventRepository.save(any(ScheduleEvent.class))).thenAnswer(invocation -> {
            ScheduleEvent event = invocation.getArgument(0);
            event.setId(1000L + event.getLessonNumber());
            return event;
        });

        List<ScheduleEventResponse> responses = service.generateEvents(CLASS_ID, 700L);

        assertThat(responses).hasSize(3);
        assertThat(responses).extracting(ScheduleEventResponse::getLessonNumber).containsExactly(1, 2, 3);
        assertThat(responses).extracting(ScheduleEventResponse::getEventDate)
                .containsExactly(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 7));
    }

    @Test
    @DisplayName("createEvent: one-off event overrides generated lesson")
    void createEvent_whenOverlappingGeneratedLesson_shouldOverrideLesson() {
        Class clazz = buildClassWithCourse(CENTER_ID, 24);
        Room room = buildRoom(ROOM_ID, CENTER_ID);
        ScheduleRecurringRule rule = ScheduleRecurringRule.builder()
                .id(700L)
                .center(clazz.getCenter())
                .clazz(clazz)
                .build();
        ScheduleEvent existingLesson = buildScheduleEvent(CENTER_ID);
        existingLesson.setRecurringRule(rule);
        existingLesson.setClazz(clazz);
        existingLesson.setRoom(room);
        existingLesson.setLessonNumber(24);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(scheduleEventRepository.findOverlappingClassEvents(
                CENTER_ID,
                CLASS_ID,
                LocalDate.of(2026, 8, 3),
                LocalTime.of(8, 0),
                LocalTime.of(10, 0),
                ScheduleEventStatus.CANCELLED,
                null
        )).thenReturn(List.of(existingLesson));
        when(scheduleEventRepository.save(any(ScheduleEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleEventResponse response = service.createEvent(CLASS_ID, ScheduleEventRequest.builder()
                .eventDate(LocalDate.of(2026, 8, 3))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .eventType(ScheduleEventType.EXAM)
                .title("Kiem tra cuoi khoa")
                .build());

        assertThat(response.getId()).isEqualTo(SCHEDULE_ID);
        assertThat(response.getRecurringRuleId()).isEqualTo(700L);
        assertThat(response.getLessonNumber()).isEqualTo(24);
        assertThat(response.getEventType()).isEqualTo(ScheduleEventType.EXAM);
        assertThat(response.getTitle()).isEqualTo("Kiem tra cuoi khoa");
    }

    @Test
    @DisplayName("createEvent: different class using same room and time should conflict")
    void createEvent_whenRoomIsBookedByAnotherEvent_shouldThrowBusinessRule() {
        Class clazz = buildClassWithCourse(CENTER_ID, 24);
        Room room = buildRoom(ROOM_ID, CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID)).thenReturn(Optional.of(room));
        when(scheduleEventRepository.findOverlappingRoomEvents(
                CENTER_ID,
                ROOM_ID,
                LocalDate.of(2026, 8, 3),
                LocalTime.of(8, 0),
                LocalTime.of(10, 0),
                ScheduleEventStatus.CANCELLED,
                null
        )).thenReturn(List.of(buildScheduleEvent(CENTER_ID)));

        assertThatThrownBy(() -> service.createEvent(CLASS_ID, ScheduleEventRequest.builder()
                .roomId(ROOM_ID)
                .eventDate(LocalDate.of(2026, 8, 3))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .eventType(ScheduleEventType.EXAM)
                .build()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Phòng");
    }

    @Test
    @DisplayName("createEvent: teacher teaching another class at same time should conflict")
    void createEvent_whenTeacherIsTeachingAnotherEvent_shouldThrowBusinessRule() {
        Class clazz = buildClassWithCourse(CENTER_ID, 24);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(scheduleEventRepository.findOverlappingTeacherEvents(
                CENTER_ID,
                SCHEDULE_TEACHER_ID,
                LocalDate.of(2026, 8, 3),
                LocalTime.of(8, 0),
                LocalTime.of(10, 0),
                ScheduleEventStatus.CANCELLED,
                null
        )).thenReturn(List.of(buildScheduleEvent(CENTER_ID)));

        assertThatThrownBy(() -> service.createEvent(CLASS_ID, ScheduleEventRequest.builder()
                .eventDate(LocalDate.of(2026, 8, 3))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .eventType(ScheduleEventType.EXAM)
                .build()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Giáo viên");
    }

    @Test
    @DisplayName("updateEvent: edits only one event and keeps conflict validation")
    void updateEvent_whenValid_shouldUpdateSingleEvent() {
        Class clazz = buildClassWithCourse(CENTER_ID, 24);
        ScheduleEvent event = buildScheduleEvent(CENTER_ID);
        event.setClazz(clazz);
        User replacementTeacher = buildTeacher(201L);
        Room replacementRoom = buildRoom(11L, CENTER_ID);

        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(scheduleEventRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(event));
        when(userRepository.findById(201L)).thenReturn(Optional.of(replacementTeacher));
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(201L, CENTER_ID, Role.TEACHER))
                .thenReturn(Optional.of(new com.owlexa.owlexabackend.modules.user.entity.Membership()));
        when(roomRepository.findByIdAndCenter_Id(11L, CENTER_ID)).thenReturn(Optional.of(replacementRoom));
        when(scheduleRepository.findAllByClazz_IdAndCenter_Id(CLASS_ID, CENTER_ID)).thenReturn(List.of());
        when(scheduleEventRepository.findOverlappingClassEvents(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(scheduleEventRepository.findOverlappingRoomEvents(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(scheduleEventRepository.findOverlappingTeacherEvents(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(scheduleRecurringRuleRepository.findAllByCenter_IdAndIsActiveTrue(CENTER_ID)).thenReturn(List.of());
        when(classEnrollmentRepository.findAllByClazz_IdAndStatusIn(any(), any())).thenReturn(List.of());
        when(scheduleEventRepository.save(any(ScheduleEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleEventResponse response = service.updateEvent(CLASS_ID, SCHEDULE_ID, ScheduleEventRequest.builder()
                .teacherUserId(201L)
                .roomId(11L)
                .eventDate(LocalDate.of(2026, 8, 4))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .eventType(ScheduleEventType.EXAM)
                .status(ScheduleEventStatus.MOVED)
                .title("Doi lich dot xuat")
                .note("Doi giao vien va phong")
                .build());

        assertThat(response.getId()).isEqualTo(SCHEDULE_ID);
        assertThat(response.getTeacherUserId()).isEqualTo(201L);
        assertThat(response.getRoomId()).isEqualTo(11L);
        assertThat(response.getEventDate()).isEqualTo(LocalDate.of(2026, 8, 4));
        assertThat(response.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.getEndTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(response.getEventType()).isEqualTo(ScheduleEventType.EXAM);
        assertThat(response.getStatus()).isEqualTo(ScheduleEventStatus.MOVED);
    }

    @Test
    @DisplayName("updateEvent: room conflict should block single-event edit")
    void updateEvent_whenRoomConflict_shouldThrowBusinessRule() {
        Class clazz = buildClassWithCourse(CENTER_ID, 24);
        ScheduleEvent event = buildScheduleEvent(CENTER_ID);
        event.setClazz(clazz);
        Room room = buildRoom(ROOM_ID, CENTER_ID);

        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(scheduleEventRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(event));
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID)).thenReturn(Optional.of(room));
        when(scheduleRepository.findAllByClazz_IdAndCenter_Id(CLASS_ID, CENTER_ID)).thenReturn(List.of());
        when(scheduleEventRepository.findOverlappingClassEvents(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(scheduleEventRepository.findOverlappingRoomEvents(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(buildScheduleEvent(CENTER_ID)));

        assertThatThrownBy(() -> service.updateEvent(CLASS_ID, SCHEDULE_ID, ScheduleEventRequest.builder()
                .roomId(ROOM_ID)
                .eventDate(LocalDate.of(2026, 8, 4))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .eventType(ScheduleEventType.LESSON)
                .status(ScheduleEventStatus.SCHEDULED)
                .build()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Phòng");
    }

    @Test
    @DisplayName("updateType: schedule type THEORY_CLASS -> set CANCELLED")
    void updateType_shouldUpdateType() {
        Schedule existing = buildSchedule(CENTER_ID);
        existing.setType(com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType.THEORY_CLASS);
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(existing));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleResponse response = service.updateType(SCHEDULE_ID, com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType.CANCELLED);

        assertThat(existing.getType()).isEqualTo(com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType.CANCELLED);
        assertThat(response.getType()).isEqualTo(com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType.CANCELLED);
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
                .hasMessageContaining("không phải là giáo viên");
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
                .hasMessageContaining("không thuộc trung tâm hiện tại");
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
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Giờ bắt đầu phải trước giờ kết thúc")
                .extracting("code")
                .isEqualTo("INVALID_TIME_RANGE");
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
        when(scheduleEventRepository.findAllByClazz_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(CLASS_ID, CENTER_ID))
                .thenReturn(List.of(buildScheduleEvent(CENTER_ID), buildScheduleEvent(CENTER_ID)));

        List<ScheduleResponse> response = service.findAllByClass(CLASS_ID);

        assertThat(response).hasSize(2);
        assertThat(response).allSatisfy(schedule -> assertThat(schedule.getSource()).isEqualTo("EVENT"));
    }

    @Test
    @DisplayName("findAllByTeacher: trả về tất cả schedule của teacher")
    void findAllByTeacher_shouldReturnSchedules() {
        when(userRepository.findById(SCHEDULE_TEACHER_ID)).thenReturn(Optional.of(buildTeacher(SCHEDULE_TEACHER_ID)));
        when(scheduleEventRepository.findAllByTeacherUser_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(SCHEDULE_TEACHER_ID, CENTER_ID))
                .thenReturn(List.of(buildScheduleEvent(CENTER_ID)));

        List<ScheduleResponse> response = service.findAllByTeacher(SCHEDULE_TEACHER_ID);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getSource()).isEqualTo("EVENT");
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
                .hasMessageContaining("không thuộc trung tâm hiện tại");
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
        lenient().when(scheduleRepository.findOverlappingTeacherSchedules(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(scheduleRepository.findOverlappingRoomSchedules(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
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
    @DisplayName("create: teacher overlap → BusinessRuleException")
    void create_whenTeacherOverlap_shouldThrowBusinessRule() {
        Class clazz = buildClass(CENTER_ID);
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(userRepository.findById(SCHEDULE_TEACHER_ID)).thenReturn(Optional.of(buildTeacher(SCHEDULE_TEACHER_ID)));
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(SCHEDULE_TEACHER_ID, CENTER_ID, Role.TEACHER))
                .thenReturn(Optional.of(new com.owlexa.owlexabackend.modules.user.entity.Membership()));
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID)).thenReturn(Optional.of(buildRoom(ROOM_ID, CENTER_ID)));
        Schedule conflict = new Schedule();
        conflict.setDayOfWeek(DayOfWeek.MONDAY);
        conflict.setStartTime(LocalTime.of(8, 0));
        conflict.setEndTime(LocalTime.of(10, 0));
        conflict.setTeacherUser(buildTeacher(SCHEDULE_TEACHER_ID));
        when(scheduleRepository.findOverlappingTeacherSchedules(any(), any(), any(), any(), any(), any())).thenReturn(List.of(conflict));

        assertThatThrownBy(() -> service.create(CLASS_ID, buildCreateRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Giáo viên Teacher 200 đã có lớp khác vào thời gian này.");
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
        Schedule conflict = new Schedule();
        conflict.setDayOfWeek(DayOfWeek.MONDAY);
        conflict.setStartTime(LocalTime.of(8, 0));
        conflict.setEndTime(LocalTime.of(10, 0));
        conflict.setRoom(buildRoom(ROOM_ID, CENTER_ID));
        when(scheduleRepository.findOverlappingRoomSchedules(any(), any(), any(), any(), any(), any())).thenReturn(List.of(conflict));

        assertThatThrownBy(() -> service.create(CLASS_ID, buildCreateRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Phòng Room 10 đã có lịch vào Thứ Hai từ 08:00 đến 10:00.");
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
        Schedule conflict = new Schedule();
        conflict.setDayOfWeek(DayOfWeek.MONDAY);
        conflict.setStartTime(LocalTime.of(8, 0));
        conflict.setEndTime(LocalTime.of(10, 0));
        conflict.setTeacherUser(buildTeacher(SCHEDULE_TEACHER_ID));
        when(scheduleRepository.findOverlappingTeacherSchedules(any(), any(), any(), any(), any(), any())).thenReturn(List.of(conflict));

        assertThatThrownBy(() -> service.update(CLASS_ID, SCHEDULE_ID, buildCreateRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Giáo viên Teacher 200 đã có lớp khác vào thời gian này.");
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
