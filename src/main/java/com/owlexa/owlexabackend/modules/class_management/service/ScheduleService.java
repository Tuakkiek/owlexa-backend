package com.owlexa.owlexabackend.modules.class_management.service;
import com.owlexa.owlexabackend.modules.class_management.dto.request.ScheduleRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.request.ScheduleEventRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.request.ScheduleRuleRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.response.ScheduleEventResponse;
import com.owlexa.owlexabackend.modules.class_management.dto.response.ScheduleResponse;
import com.owlexa.owlexabackend.modules.class_management.dto.response.ScheduleRuleResponse;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEvent;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventStatus;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventType;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleRecurringRule;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleRepeatType;
import com.owlexa.owlexabackend.modules.room.entity.Room;
import com.owlexa.owlexabackend.modules.room.repository.RoomRepository;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleEventRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRecurringRuleRepository;
import com.owlexa.owlexabackend.modules.class_management.service.validation.ClassLifecycleValidator;
import com.owlexa.owlexabackend.modules.class_management.service.validation.TimeRangeValidator;
import com.owlexa.owlexabackend.modules.class_management.service.validation.RoomConflictValidator;
import com.owlexa.owlexabackend.modules.class_management.service.validation.TeacherConflictValidator;
import com.owlexa.owlexabackend.modules.class_management.service.validation.StudentConflictValidator;
import com.owlexa.owlexabackend.modules.class_management.service.validation.ScheduleValidationContext;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final MembershipRepository membershipRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleRecurringRuleRepository scheduleRecurringRuleRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final RoomRepository roomRepository;

    private final ClassLifecycleValidator classLifecycleValidator;
    private final TimeRangeValidator timeRangeValidator;
    private final RoomConflictValidator roomConflictValidator;
    private final TeacherConflictValidator teacherConflictValidator;
    private final StudentConflictValidator studentConflictValidator;

    @Autowired
    public ScheduleService(
            UserRepository userRepository,
            ClassRepository classRepository,
            MembershipRepository membershipRepository,
            ScheduleRepository scheduleRepository,
            ScheduleRecurringRuleRepository scheduleRecurringRuleRepository,
            ScheduleEventRepository scheduleEventRepository,
            ClassEnrollmentRepository classEnrollmentRepository,
            RoomRepository roomRepository,
            ClassLifecycleValidator classLifecycleValidator,
            TimeRangeValidator timeRangeValidator,
            RoomConflictValidator roomConflictValidator,
            TeacherConflictValidator teacherConflictValidator,
            StudentConflictValidator studentConflictValidator
    ) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.membershipRepository = membershipRepository;
        this.scheduleRepository = scheduleRepository;
        this.scheduleRecurringRuleRepository = scheduleRecurringRuleRepository;
        this.scheduleEventRepository = scheduleEventRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
        this.roomRepository = roomRepository;
        this.classLifecycleValidator = classLifecycleValidator;
        this.timeRangeValidator = timeRangeValidator;
        this.roomConflictValidator = roomConflictValidator;
        this.teacherConflictValidator = teacherConflictValidator;
        this.studentConflictValidator = studentConflictValidator;
    }

    ScheduleService(
            UserRepository userRepository,
            ClassRepository classRepository,
            MembershipRepository membershipRepository,
            ScheduleRepository scheduleRepository,
            ClassEnrollmentRepository classEnrollmentRepository,
            RoomRepository roomRepository,
            ClassLifecycleValidator classLifecycleValidator,
            TimeRangeValidator timeRangeValidator,
            RoomConflictValidator roomConflictValidator,
            TeacherConflictValidator teacherConflictValidator,
            StudentConflictValidator studentConflictValidator
    ) {
        this(
                userRepository,
                classRepository,
                membershipRepository,
                scheduleRepository,
                null,
                null,
                classEnrollmentRepository,
                roomRepository,
                classLifecycleValidator,
                timeRangeValidator,
                roomConflictValidator,
                teacherConflictValidator,
                studentConflictValidator
        );
    }

    // Create
    @Transactional
    public ScheduleResponse create(Long classId, ScheduleRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));

        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Lớp học này không thuộc trung tâm hiện tại.");
        }

        User teacher = userRepository.findById(request.getTeacherUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo viên với ID: " + request.getTeacherUserId()));

        if (teacher.getRole() != Role.TEACHER) {
            throw new BadRequestException("Người dùng được chọn không phải là giáo viên.");
        }

        boolean teacherInCenter = membershipRepository
                .findByUser_IdAndCenter_IdAndUserRole(teacher.getId(), centerId, Role.TEACHER)
                .isPresent();

        if(!teacherInCenter) {
            throw new BadRequestException("Giáo viên không thuộc trung tâm hiện tại.");
        }

        Room room = roomRepository.findByIdAndCenter_Id(request.getRoomId(), centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng học với ID: " + request.getRoomId()));

        if (scheduleRepository.existsByClazz_IdAndDayOfWeekAndStartTimeAndCenter_Id(
                classId,
                request.getDayOfWeek() == null ? null : DayOfWeek.of(request.getDayOfWeek()),
                request.getStartTime(),
                centerId
        )) {
            throw new DuplicateResourceException("Lớp này đã có lịch học vào thời gian này.");
        }

        ScheduleValidationContext validationContext = ScheduleValidationContext.builder()
                .scheduleId(null)
                .clazz(clazz)
                .room(room)
                .teacher(teacher)
                .dayOfWeek(request.getDayOfWeek() == null ? null : DayOfWeek.of(request.getDayOfWeek()))
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .centerId(centerId)
                .build();

        timeRangeValidator.validate(validationContext);
        classLifecycleValidator.validate(validationContext);
        roomConflictValidator.validate(validationContext);
        teacherConflictValidator.validate(validationContext);
        studentConflictValidator.validate(validationContext);

        Schedule schedule = Schedule.builder()
                .clazz(clazz)
                .center(clazz.getCenter())
                .teacherUser(teacher)
                .room(room)
                .dayOfWeek(request.getDayOfWeek() == null ? null : DayOfWeek.of(request.getDayOfWeek()))
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .type(request.getType() != null ? request.getType() : ScheduleType.THEORY_CLASS)
                .build();

        schedule = scheduleRepository.save(schedule);

        return toResponse(schedule);
    }

    // Find all by class
    @Transactional(readOnly = true)
    public List<ScheduleResponse> findAllByClass(long classId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterAndMembership(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));

        if(!clazz.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Lớp học này không thuộc trung tâm hiện tại.");
        }

        if (scheduleEventRepository == null) {
            return List.of();
        }
        return sortScheduleResponses(scheduleEventRepository.findAllByClazz_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(classId, centerId)
                .stream()
                .filter(this::isVisibleScheduleEvent)
                .map(this::toScheduleResponse)
                .toList());
    }

    // Find all by teacher
    @Transactional(readOnly = true)
    public List<ScheduleResponse> findAllByTeacher(Long teacherUserId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterAndMembership(currentUser, centerId);

        User teacher = userRepository.findById(teacherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo viên với ID: " + teacherUserId));

        if (!teacher.getRole().equals(Role.TEACHER)) {
            throw new BadRequestException("Người dùng được chọn không phải là giáo viên.");
        }

        if (scheduleEventRepository == null) {
            return List.of();
        }
        return sortScheduleResponses(scheduleEventRepository.findAllByTeacherUser_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(teacherUserId, centerId)
                .stream()
                .filter(this::isVisibleScheduleEvent)
                .map(this::toScheduleResponse)
                .toList());
    }

    // Find my schedules — for logged-in TEACHER
    @Transactional(readOnly = true)
    public List<ScheduleResponse> findMySchedules() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("Only TEACHER can access their own schedules");
        }

        if (scheduleEventRepository == null) {
            return List.of();
        }
        return sortScheduleResponses(scheduleEventRepository.findAllByTeacherUser_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(currentUser.getId(), centerId)
                .stream()
                .filter(this::isVisibleScheduleEvent)
                .map(this::toScheduleResponse)
                .toList());
    }

    // Find all active schedules in center — for OWNER attendance overview
    @Transactional(readOnly = true)
    public List<ScheduleResponse> findAllForOwner() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can access all schedules");
        }

        if (scheduleEventRepository == null) {
            return List.of();
        }
        return sortScheduleResponses(scheduleEventRepository.findAllByCenter_IdOrderByEventDateAscStartTimeAsc(centerId)
                .stream()
                .filter(e -> e.getStatus() != ScheduleEventStatus.CANCELLED)
                .map(this::toScheduleResponse)
                .toList());
    }

    // Find my schedules — for logged-in STUDENT (via enrolled classes)
    @Transactional(readOnly = true)
    public List<ScheduleResponse> findMySchedulesAsStudent() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.STUDENT) {
            throw new AccessDeniedException("Only STUDENT can access their own schedules");
        }

        // Only return schedules for classes the student is ACTIVELY enrolled in.
        // DROPPED/SUSPENDED/PENDING enrollments are intentionally excluded so that
        // a removed student immediately loses access to all class schedules.
        List<Long> activeEnrolledClassIds = classEnrollmentRepository
                .findAllByStudentUser_IdAndCenter_IdAndStatusIn(
                        currentUser.getId(), centerId,
                        List.of(com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.ACTIVE))
                .stream()
                .map(e -> e.getClazz().getId())
                .toList();

        if (scheduleEventRepository == null) {
            return List.of();
        }
        List<ScheduleResponse> combined = new ArrayList<>();
        activeEnrolledClassIds.forEach(classId -> combined.addAll(
                scheduleEventRepository.findAllByClazz_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(classId, centerId)
                        .stream()
                        .filter(this::isVisibleScheduleEvent)
                        .map(this::toScheduleResponse)
                        .toList()
        ));
        return sortScheduleResponses(combined);
    }

    // Find my classes — for logged-in TEACHER (distinct classes from schedules)
    @Transactional(readOnly = true)
    public List<Long> findMyClassIds() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("Only TEACHER can access their own classes");
        }

        if (scheduleEventRepository == null) {
            return List.of();
        }
        return scheduleEventRepository.findAllByTeacherUser_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(currentUser.getId(), centerId)
                .stream()
                .filter(this::isVisibleScheduleEvent)
                .map(s -> s.getClazz().getId())
                .distinct()
                .toList();
    }

    @Transactional
    public ScheduleRuleResponse createRule(Long classId, ScheduleRuleRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertOwnerAndCenterMembership(currentUser, centerId);

        Class clazz = getClassInCenter(classId, centerId);
        User teacher = getTeacherInCenter(request.getTeacherUserId(), centerId);
        Room room = roomRepository.findByIdAndCenter_Id(request.getRoomId(), centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng học với ID: " + request.getRoomId()));
        validateRuleRequest(request);
        int sessionCount = requiredSessionCount(clazz);
        LocalDate endDate = calculateRuleEndDate(request.getStartDate(), Set.copyOf(request.getDaysOfWeek()), sessionCount);
        validateRecurringRuleConflicts(
                clazz,
                teacher,
                room,
                Set.copyOf(request.getDaysOfWeek()),
                request.getStartDate(),
                endDate,
                request.getStartTime(),
                request.getEndTime(),
                centerId,
                null
        );

        ScheduleRecurringRule rule = ScheduleRecurringRule.builder()
                .center(clazz.getCenter())
                .clazz(clazz)
                .teacherUser(teacher)
                .room(room)
                .repeatType(ScheduleRepeatType.WEEKLY)
                .daysOfWeek(toDaysCsv(request.getDaysOfWeek()))
                .startDate(request.getStartDate())
                .endDate(endDate)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .type(request.getType() != null ? request.getType() : ScheduleType.THEORY_CLASS)
                .isActive(true)
                .build();
        return toRuleResponse(scheduleRecurringRuleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public List<ScheduleRuleResponse> findRulesByClass(Long classId) {
        Long centerId = requiredCurrentCenterId();
        assertCenterAndMembership(getCurrentUser(), centerId);
        getClassInCenter(classId, centerId);
        return scheduleRecurringRuleRepository.findAllByClazz_IdAndCenter_IdOrderByStartDateAscStartTimeAsc(classId, centerId)
                .stream()
                .map(this::toRuleResponse)
                .toList();
    }

    @Transactional
    public List<ScheduleEventResponse> generateEvents(Long classId, Long ruleId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertOwnerAndCenterMembership(currentUser, centerId);
        getClassInCenter(classId, centerId);

        ScheduleRecurringRule rule = scheduleRecurringRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy tắc lịch lặp với ID: " + ruleId));
        if (!rule.getCenter().getId().equals(centerId) || !rule.getClazz().getId().equals(classId)) {
            throw new TenancyViolationException("Quy tắc lịch lặp này không thuộc lớp hoặc trung tâm hiện tại.");
        }

        Set<Integer> days = parseDays(rule.getDaysOfWeek());
        int sessionCount = requiredSessionCount(rule.getClazz());
        List<ScheduleEvent> created = new ArrayList<>();
        int occurrenceNumber = 0;
        for (LocalDate date = rule.getStartDate(); occurrenceNumber < sessionCount; date = date.plusDays(1)) {
            if (!days.contains(date.getDayOfWeek().getValue())) {
                continue;
            }
            occurrenceNumber++;
            if (scheduleEventRepository.existsByRecurringRule_IdAndEventDateAndCenter_Id(ruleId, date, centerId)) {
                continue;
            }
            validateScheduleEventConflicts(
                    rule.getClazz(),
                    rule.getTeacherUser(),
                    rule.getRoom(),
                    date,
                    rule.getStartTime(),
                    rule.getEndTime(),
                    centerId,
                    null,
                    rule.getId(),
                    true
            );
            created.add(scheduleEventRepository.save(ScheduleEvent.builder()
                    .center(rule.getCenter())
                    .clazz(rule.getClazz())
                    .recurringRule(rule)
                    .teacherUser(rule.getTeacherUser())
                    .room(rule.getRoom())
                    .eventDate(date)
                    .startTime(rule.getStartTime())
                    .endTime(rule.getEndTime())
                    .lessonNumber(occurrenceNumber)
                    .eventType(rule.getType() == ScheduleType.ONLINE_CLASS ? ScheduleEventType.ONLINE_LESSON : ScheduleEventType.LESSON)
                    .status(ScheduleEventStatus.SCHEDULED)
                    .build()));
        }
        return created.stream().map(this::toEventResponse).toList();
    }

    @Transactional
    public ScheduleEventResponse createEvent(Long classId, ScheduleEventRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertOwnerAndCenterMembership(currentUser, centerId);
        Class clazz = getClassInCenter(classId, centerId);
        User teacher = request.getTeacherUserId() != null ? getTeacherInCenter(request.getTeacherUserId(), centerId) : clazz.getTeacherUser();
        Room room = request.getRoomId() != null
                ? roomRepository.findByIdAndCenter_Id(request.getRoomId(), centerId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng học với ID: " + request.getRoomId()))
                : null;
        validateTimeRange(request.getStartTime(), request.getEndTime());

        ScheduleEventStatus status = request.getStatus() != null ? request.getStatus() : ScheduleEventStatus.SCHEDULED;
        List<ScheduleEvent> overlappingEvents = scheduleEventRepository.findOverlappingClassEvents(
                centerId,
                classId,
                request.getEventDate(),
                request.getStartTime(),
                request.getEndTime(),
                ScheduleEventStatus.CANCELLED,
                null
        );
        ScheduleEvent generatedLesson = overlappingEvents.stream()
                .filter(event -> event.getRecurringRule() != null)
                .filter(event -> event.getEventType() == ScheduleEventType.LESSON || event.getEventType() == ScheduleEventType.ONLINE_LESSON)
                .findFirst()
                .orElse(null);
        if (generatedLesson != null) {
            User targetTeacher = teacher != null ? teacher : generatedLesson.getTeacherUser();
            Room targetRoom = room != null ? room : generatedLesson.getRoom();
            validateScheduleEventConflicts(
                    clazz,
                    targetTeacher,
                    targetRoom,
                    request.getEventDate(),
                    request.getStartTime(),
                    request.getEndTime(),
                    centerId,
                    generatedLesson.getId(),
                    generatedLesson.getRecurringRule().getId(),
                    status != ScheduleEventStatus.CANCELLED
            );
            generatedLesson.setTeacherUser(teacher != null ? teacher : generatedLesson.getTeacherUser());
            generatedLesson.setRoom(room != null ? room : generatedLesson.getRoom());
            generatedLesson.setEventDate(request.getEventDate());
            generatedLesson.setStartTime(request.getStartTime());
            generatedLesson.setEndTime(request.getEndTime());
            generatedLesson.setEventType(request.getEventType());
            generatedLesson.setStatus(status);
            generatedLesson.setTitle(request.getTitle());
            generatedLesson.setNote(request.getNote());
            return toEventResponse(scheduleEventRepository.save(generatedLesson));
        }
        if (!overlappingEvents.isEmpty()) {
            throw new DuplicateResourceException("Sự kiện lịch bị trùng với một sự kiện đã có.");
        }
        validateScheduleEventConflicts(
                clazz,
                teacher,
                room,
                request.getEventDate(),
                request.getStartTime(),
                request.getEndTime(),
                centerId,
                null,
                null,
                status != ScheduleEventStatus.CANCELLED
        );

        ScheduleEvent event = ScheduleEvent.builder()
                .center(clazz.getCenter())
                .clazz(clazz)
                .teacherUser(teacher)
                .room(room)
                .eventDate(request.getEventDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .eventType(request.getEventType())
                .status(status)
                .title(request.getTitle())
                .note(request.getNote())
                .build();
        return toEventResponse(scheduleEventRepository.save(event));
    }

    @Transactional
    public ScheduleEventResponse updateEvent(Long classId, Long eventId, ScheduleEventRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertOwnerAndCenterMembership(currentUser, centerId);
        getClassInCenter(classId, centerId);
        ScheduleEvent event = getEventInCenter(eventId, centerId);
        if (!event.getClazz().getId().equals(classId)) {
            throw new TenancyViolationException("Sự kiện lịch này không thuộc lớp hiện tại.");
        }
        validateTimeRange(request.getStartTime(), request.getEndTime());
        User teacher = request.getTeacherUserId() != null ? getTeacherInCenter(request.getTeacherUserId(), centerId) : event.getTeacherUser();
        Room room = request.getRoomId() != null ? roomRepository.findByIdAndCenter_Id(request.getRoomId(), centerId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng học với ID: " + request.getRoomId())) : event.getRoom();
        ScheduleEventStatus status = request.getStatus() != null ? request.getStatus() : event.getStatus();
        validateScheduleEventConflicts(
                event.getClazz(),
                teacher,
                room,
                request.getEventDate(),
                request.getStartTime(),
                request.getEndTime(),
                centerId,
                event.getId(),
                event.getRecurringRule() != null ? event.getRecurringRule().getId() : null,
                status != ScheduleEventStatus.CANCELLED
        );
        event.setTeacherUser(teacher);
        event.setRoom(room);
        event.setEventDate(request.getEventDate());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setEventType(request.getEventType());
        event.setStatus(status);
        event.setTitle(request.getTitle());
        event.setNote(request.getNote());
        return toEventResponse(scheduleEventRepository.save(event));
    }

    @Transactional
    public ScheduleEventResponse cancelEvent(Long classId, Long eventId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertOwnerAndCenterMembership(currentUser, centerId);
        getClassInCenter(classId, centerId);
        ScheduleEvent event = getEventInCenter(eventId, centerId);
        event.setStatus(ScheduleEventStatus.CANCELLED);
        return toEventResponse(scheduleEventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<ScheduleEventResponse> findEventsByClass(Long classId) {
        Long centerId = requiredCurrentCenterId();
        assertCenterAndMembership(getCurrentUser(), centerId);
        getClassInCenter(classId, centerId);
        return scheduleEventRepository.findAllByClazz_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(classId, centerId)
                .stream()
                .map(this::toEventResponse)
                .toList();
    }

    // Update
    @Transactional
    public ScheduleResponse update(Long classId, Long scheduleId, ScheduleRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học với ID: " + scheduleId));

        if(!schedule.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Lịch học này không thuộc trung tâm hiện tại.");
        }

        User teacher = userRepository.findById(request.getTeacherUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy giáo viên với ID: " + request.getTeacherUserId()
                ));

        if(!teacher.getRole().equals(Role.TEACHER)) {
            throw new BadRequestException("Người dùng được chọn không phải là giáo viên.");
        }

        boolean teacherInCenter = membershipRepository
                .findByUser_IdAndCenter_IdAndUserRole(
                        request.getTeacherUserId(),
                        centerId,
                        Role.TEACHER
                ).isPresent();
        if (!teacherInCenter) {
            throw new BadRequestException("Giáo viên không thuộc trung tâm hiện tại.");
        }

        Room room = roomRepository.findByIdAndCenter_Id(request.getRoomId(), centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng học với ID: " + request.getRoomId()));

        ScheduleValidationContext validationContext = ScheduleValidationContext.builder()
                .scheduleId(scheduleId)
                .clazz(schedule.getClazz())
                .room(room)
                .teacher(teacher)
                .dayOfWeek(request.getDayOfWeek() == null ? null : DayOfWeek.of(request.getDayOfWeek()))
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .centerId(centerId)
                .build();

        timeRangeValidator.validate(validationContext);
        classLifecycleValidator.validate(validationContext);
        roomConflictValidator.validate(validationContext);
        teacherConflictValidator.validate(validationContext);
        studentConflictValidator.validate(validationContext);

        schedule.setTeacherUser(teacher);
        schedule.setRoom(room);
        schedule.setDayOfWeek(request.getDayOfWeek() == null ? null : DayOfWeek.of(request.getDayOfWeek()));
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        if (request.getType() != null) {
            schedule.setType(request.getType());
        }

        schedule = scheduleRepository.save(schedule);

        return toResponse(schedule);
    }

    // Delete
    @Transactional
    public void delete(Long scheduleId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy lịch học với ID: " + scheduleId
                ));

        if(!schedule.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Lịch học này không thuộc trung tâm hiện tại.");
        }

        scheduleRepository.delete(schedule);
    }

    // Update type
    @Transactional
    public ScheduleResponse updateType(Long scheduleId, ScheduleType newType) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy lịch học với ID: " + scheduleId
                ));

        if (!schedule.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Lịch học này không thuộc trung tâm hiện tại.");
        }

        schedule.setType(newType);
        schedule = scheduleRepository.save(schedule);

        return toResponse(schedule);
    }

    // Helper
    // Validate time range
    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BadRequestException("Giờ bắt đầu phải trước giờ kết thúc.");
        }
    }

    // To response
    private ScheduleResponse toResponse(Schedule schedule) {
        return ScheduleResponse.builder()
            .id(schedule.getId())
            .classId(schedule.getClazz().getId())
            .className(schedule.getClazz().getName())
            .centerId(schedule.getCenter().getId())
                .teacherUserId(schedule.getTeacherUser() != null ? schedule.getTeacherUser().getId() : null)
                .teacherUserFullName(schedule.getTeacherUser() != null ? schedule.getTeacherUser().getFullName() : null)
                .teacherPhoneNumber(schedule.getTeacherUser() != null ? schedule.getTeacherUser().getPhoneNumber() : null)
                .roomId(schedule.getRoom() != null ? schedule.getRoom().getId() : null)
                .roomName(schedule.getRoom() != null ? schedule.getRoom().getName() : null)
                .roomCode(schedule.getRoom() != null ? schedule.getRoom().getCode() : null)
                .dayOfWeek(schedule.getDayOfWeek().getValue())
            .startTime(schedule.getStartTime())
            .endTime(schedule.getEndTime())
            .type(schedule.getType())
            .source("RECURRING_LEGACY")
            .createdAt(schedule.getCreatedAt())
            .build();
    }

    private ScheduleResponse toScheduleResponse(ScheduleEvent event) {
        return ScheduleResponse.builder()
                .id(event.getId())
                .classId(event.getClazz().getId())
                .className(event.getTitle() != null && !event.getTitle().isBlank() ? event.getTitle() : event.getClazz().getName())
                .centerId(event.getCenter().getId())
                .teacherUserId(event.getTeacherUser() != null ? event.getTeacherUser().getId() : null)
                .teacherUserFullName(event.getTeacherUser() != null ? event.getTeacherUser().getFullName() : null)
                .teacherPhoneNumber(event.getTeacherUser() != null ? event.getTeacherUser().getPhoneNumber() : null)
                .roomId(event.getRoom() != null ? event.getRoom().getId() : null)
                .roomName(event.getRoom() != null ? event.getRoom().getName() : null)
                .roomCode(event.getRoom() != null ? event.getRoom().getCode() : null)
                .dayOfWeek(event.getEventDate().getDayOfWeek().getValue())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .type(toScheduleType(event))
                .eventDate(event.getEventDate())
                .lessonNumber(event.getLessonNumber())
                .eventStatus(event.getStatus())
                .source("EVENT")
                .createdAt(event.getCreatedAt())
                .build();
    }

    private ScheduleType toScheduleType(ScheduleEvent event) {
        if (event.getStatus() == ScheduleEventStatus.CANCELLED) {
            return ScheduleType.CANCELLED;
        }
        return switch (event.getEventType()) {
            case EXAM -> ScheduleType.EXAM;
            case ONLINE_LESSON -> ScheduleType.ONLINE_CLASS;
            case LESSON, PRACTICE -> ScheduleType.THEORY_CLASS;
        };
    }

    private ScheduleEventResponse toEventResponse(ScheduleEvent event) {
        return ScheduleEventResponse.builder()
                .id(event.getId())
                .classId(event.getClazz().getId())
                .className(event.getClazz().getName())
                .recurringRuleId(event.getRecurringRule() != null ? event.getRecurringRule().getId() : null)
                .teacherUserId(event.getTeacherUser() != null ? event.getTeacherUser().getId() : null)
                .teacherUserFullName(event.getTeacherUser() != null ? event.getTeacherUser().getFullName() : null)
                .roomId(event.getRoom() != null ? event.getRoom().getId() : null)
                .roomName(event.getRoom() != null ? event.getRoom().getName() : null)
                .eventDate(event.getEventDate())
                .dayOfWeek(event.getEventDate().getDayOfWeek().getValue())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .lessonNumber(event.getLessonNumber())
                .eventType(event.getEventType())
                .status(event.getStatus())
                .title(event.getTitle())
                .note(event.getNote())
                .build();
    }

    private ScheduleRuleResponse toRuleResponse(ScheduleRecurringRule rule) {
        long generatedEventCount = scheduleEventRepository
                .findAllByRecurringRule_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(rule.getId(), rule.getCenter().getId())
                .size();
        return ScheduleRuleResponse.builder()
                .id(rule.getId())
                .classId(rule.getClazz().getId())
                .teacherUserId(rule.getTeacherUser() != null ? rule.getTeacherUser().getId() : null)
                .teacherUserFullName(rule.getTeacherUser() != null ? rule.getTeacherUser().getFullName() : null)
                .roomId(rule.getRoom() != null ? rule.getRoom().getId() : null)
                .roomName(rule.getRoom() != null ? rule.getRoom().getName() : null)
                .repeatType(rule.getRepeatType())
                .daysOfWeek(parseDays(rule.getDaysOfWeek()).stream().sorted().toList())
                .startDate(rule.getStartDate())
                .endDate(rule.getEndDate())
                .startTime(rule.getStartTime())
                .endTime(rule.getEndTime())
                .type(rule.getType())
                .isActive(rule.getIsActive())
                .generatedEventCount(generatedEventCount)
                .build();
    }

    private Class getClassInCenter(Long classId, Long centerId) {
        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));
        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Lớp học này không thuộc trung tâm hiện tại.");
        }
        return clazz;
    }

    private User getTeacherInCenter(Long teacherUserId, Long centerId) {
        User teacher = userRepository.findById(teacherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo viên với ID: " + teacherUserId));
        if (teacher.getRole() != Role.TEACHER) {
            throw new BadRequestException("Người dùng được chọn không phải là giáo viên.");
        }
        boolean teacherInCenter = membershipRepository
                .findByUser_IdAndCenter_IdAndUserRole(teacherUserId, centerId, Role.TEACHER)
                .isPresent();
        if (!teacherInCenter) {
            throw new BadRequestException("Giáo viên không thuộc trung tâm hiện tại.");
        }
        return teacher;
    }

    private ScheduleEvent getEventInCenter(Long eventId, Long centerId) {
        ScheduleEvent event = scheduleEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sự kiện lịch với ID: " + eventId));
        if (!event.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Sự kiện lịch này không thuộc trung tâm hiện tại.");
        }
        return event;
    }

    private void validateRecurringRuleConflicts(
            Class clazz,
            User teacher,
            Room room,
            Set<Integer> daysOfWeek,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            Long centerId,
            Long excludeRuleId
    ) {
        validateClassLifecycle(clazz, daysOfWeek.stream().findFirst().orElse(null), startTime, endTime, centerId);

        int checkedOccurrences = 0;
        int sessionCount = requiredSessionCount(clazz);
        for (LocalDate date = startDate; !date.isAfter(endDate) && checkedOccurrences < sessionCount; date = date.plusDays(1)) {
            if (!daysOfWeek.contains(date.getDayOfWeek().getValue())) {
                continue;
            }
            checkedOccurrences++;
            validateScheduleEventConflicts(
                    clazz,
                    teacher,
                    room,
                    date,
                    startTime,
                    endTime,
                    centerId,
                    null,
                    excludeRuleId,
                    true
            );
        }
    }

    private void validateScheduleEventConflicts(
            Class clazz,
            User teacher,
            Room room,
            LocalDate eventDate,
            LocalTime startTime,
            LocalTime endTime,
            Long centerId,
            Long excludeEventId,
            Long excludeRuleId,
            boolean occupiesResources
    ) {
        if (!occupiesResources) {
            return;
        }

        validateClassLifecycle(clazz, eventDate.getDayOfWeek().getValue(), startTime, endTime, centerId);
        validateLegacyScheduleConflicts(clazz, teacher, room, eventDate, startTime, endTime, centerId);
        validateEventTableConflicts(clazz, teacher, room, eventDate, startTime, endTime, centerId, excludeEventId);
        validateActiveRuleConflicts(clazz, teacher, room, eventDate, startTime, endTime, centerId, excludeRuleId);
    }

    private void validateClassLifecycle(
            Class clazz,
            Integer dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Long centerId
    ) {
        classLifecycleValidator.validate(ScheduleValidationContext.builder()
                .scheduleId(null)
                .clazz(clazz)
                .dayOfWeek(dayOfWeek == null ? null : DayOfWeek.of(dayOfWeek))
                .startTime(startTime)
                .endTime(endTime)
                .centerId(centerId)
                .build());
    }

    private void validateLegacyScheduleConflicts(
            Class clazz,
            User teacher,
            Room room,
            LocalDate eventDate,
            LocalTime startTime,
            LocalTime endTime,
            Long centerId
    ) {
        DayOfWeek dayOfWeek = eventDate.getDayOfWeek();

        boolean classOverlapsLegacy = scheduleRepository.findAllByClazz_IdAndCenter_Id(clazz.getId(), centerId)
                .stream()
                .anyMatch(schedule -> schedule.getDayOfWeek() == dayOfWeek
                        && schedule.getType() != ScheduleType.CANCELLED
                        && timeOverlaps(startTime, endTime, schedule.getStartTime(), schedule.getEndTime()));
        if (classOverlapsLegacy) {
            throw new DuplicateResourceException("Lớp này đã có lịch học trùng thời gian.");
        }

        if (room != null && !scheduleRepository.findOverlappingRoomSchedules(room.getId(), dayOfWeek, startTime, endTime, centerId, null).isEmpty()) {
            throw new BusinessRuleException("ROOM_CONFLICT", "Phòng đã được đặt vào thời gian này");
        }

        if (teacher != null && !scheduleRepository.findOverlappingTeacherSchedules(teacher.getId(), dayOfWeek, startTime, endTime, centerId, null).isEmpty()) {
            throw new BusinessRuleException("TEACHER_CONFLICT", "Giáo viên đã có lớp khác vào thời gian này");
        }

        for (Long studentId : activeStudentIdsForClass(clazz.getId())) {
            if (!scheduleRepository.findOverlappingStudentSchedules(studentId, dayOfWeek, startTime, endTime, centerId, null).isEmpty()) {
                throw new BusinessRuleException("STUDENT_CONFLICT", "Học viên trong lớp này đã có lịch học khác vào thời gian này");
            }
        }
    }

    private void validateEventTableConflicts(
            Class clazz,
            User teacher,
            Room room,
            LocalDate eventDate,
            LocalTime startTime,
            LocalTime endTime,
            Long centerId,
            Long excludeEventId
    ) {
        if (!scheduleEventRepository.findOverlappingClassEvents(centerId, clazz.getId(), eventDate, startTime, endTime, ScheduleEventStatus.CANCELLED, excludeEventId).isEmpty()) {
            throw new DuplicateResourceException("Lớp này đã có buổi học hoặc sự kiện trùng thời gian.");
        }

        if (room != null && !scheduleEventRepository.findOverlappingRoomEvents(centerId, room.getId(), eventDate, startTime, endTime, ScheduleEventStatus.CANCELLED, excludeEventId).isEmpty()) {
            throw new BusinessRuleException("ROOM_CONFLICT", "Phòng đã được đặt vào thời gian này");
        }

        if (teacher != null && !scheduleEventRepository.findOverlappingTeacherEvents(centerId, teacher.getId(), eventDate, startTime, endTime, ScheduleEventStatus.CANCELLED, excludeEventId).isEmpty()) {
            throw new BusinessRuleException("TEACHER_CONFLICT", "Giáo viên đã có lớp khác vào thời gian này");
        }

        for (Long studentId : activeStudentIdsForClass(clazz.getId())) {
            if (!scheduleEventRepository.findOverlappingStudentEvents(centerId, studentId, eventDate, startTime, endTime, ScheduleEventStatus.CANCELLED, excludeEventId).isEmpty()) {
                throw new BusinessRuleException("STUDENT_CONFLICT", "Học viên trong lớp này đã có lịch học khác vào thời gian này");
            }
        }
    }

    private void validateActiveRuleConflicts(
            Class clazz,
            User teacher,
            Room room,
            LocalDate eventDate,
            LocalTime startTime,
            LocalTime endTime,
            Long centerId,
            Long excludeRuleId
    ) {
        Set<Long> targetStudentIds = activeStudentIdsForClass(clazz.getId());
        for (ScheduleRecurringRule rule : scheduleRecurringRuleRepository.findAllByCenter_IdAndIsActiveTrue(centerId)) {
            if (excludeRuleId != null && excludeRuleId.equals(rule.getId())) {
                continue;
            }
            if (!ruleAppliesOn(rule, eventDate) || !timeOverlaps(startTime, endTime, rule.getStartTime(), rule.getEndTime())) {
                continue;
            }

            if (sameId(rule.getClazz().getId(), clazz.getId())) {
                throw new DuplicateResourceException("Lớp này đã có quy tắc lịch lặp trùng thời gian.");
            }
            if (room != null && rule.getRoom() != null && sameId(rule.getRoom().getId(), room.getId())) {
                throw new BusinessRuleException("ROOM_CONFLICT", "Phòng đã được đặt bởi một lịch lặp đang hoạt động.");
            }
            if (teacher != null && rule.getTeacherUser() != null && sameId(rule.getTeacherUser().getId(), teacher.getId())) {
                throw new BusinessRuleException("TEACHER_CONFLICT", "Giáo viên đã có lịch lặp khác vào thời gian này.");
            }
            if (!targetStudentIds.isEmpty() && !java.util.Collections.disjoint(targetStudentIds, activeStudentIdsForClass(rule.getClazz().getId()))) {
                throw new BusinessRuleException("STUDENT_CONFLICT", "Có học viên trong lớp này đã có lịch lặp khác vào thời gian này.");
            }
        }
    }

    private Set<Long> activeStudentIdsForClass(Long classId) {
        return classEnrollmentRepository.findAllByClazz_IdAndStatusIn(
                        classId,
                        List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.PENDING, EnrollmentStatus.SUSPENDED))
                .stream()
                .map(enrollment -> enrollment.getStudentUser().getId())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private boolean ruleAppliesOn(ScheduleRecurringRule rule, LocalDate date) {
        return Boolean.TRUE.equals(rule.getIsActive())
                && !date.isBefore(rule.getStartDate())
                && !date.isAfter(rule.getEndDate())
                && parseDays(rule.getDaysOfWeek()).contains(date.getDayOfWeek().getValue());
    }

    private boolean timeOverlaps(LocalTime startA, LocalTime endA, LocalTime startB, LocalTime endB) {
        return startA.isBefore(endB) && endA.isAfter(startB);
    }

    private boolean sameId(Long first, Long second) {
        return first != null && first.equals(second);
    }

    private void validateRuleRequest(ScheduleRuleRequest request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());
        request.getDaysOfWeek().forEach(day -> {
            if (day == null || day < 1 || day > 7) {
                throw new BadRequestException("Thứ học phải nằm trong khoảng từ 1 đến 7.");
            }
        });
    }

    private int requiredSessionCount(Class clazz) {
        if (clazz.getCourse() == null || clazz.getCourse().getDefaultSessionCount() == null || clazz.getCourse().getDefaultSessionCount() <= 0) {
            throw new BadRequestException("Khóa học cần có số buổi học lớn hơn 0 trước khi tạo lịch lặp.");
        }
        return clazz.getCourse().getDefaultSessionCount();
    }

    private LocalDate calculateRuleEndDate(LocalDate startDate, Set<Integer> daysOfWeek, int sessionCount) {
        int matchedSessions = 0;
        LocalDate date = startDate;
        while (matchedSessions < sessionCount) {
            if (daysOfWeek.contains(date.getDayOfWeek().getValue())) {
                matchedSessions++;
            }
            if (matchedSessions == sessionCount) {
                return date;
            }
            date = date.plusDays(1);
        }
        return startDate;
    }

    private boolean isVisibleScheduleEvent(ScheduleEvent event) {
        return event.getStatus() != ScheduleEventStatus.CANCELLED;
    }

    private String toDaysCsv(List<Integer> days) {
        return days.stream()
                .distinct()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private Set<Integer> parseDays(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Integer::valueOf)
                .collect(Collectors.toSet());
    }

    private List<ScheduleResponse> sortScheduleResponses(List<ScheduleResponse> responses) {
        return responses.stream()
                .sorted(Comparator
                        .comparing((ScheduleResponse response) -> response.getEventDate() == null ? LocalDate.MIN : response.getEventDate())
                        .thenComparing(ScheduleResponse::getDayOfWeek, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ScheduleResponse::getStartTime, Comparator.nullsLast(LocalTime::compareTo)))
                .toList();
    }

    // Get current user
    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new AccessDeniedException("Người dùng chưa đăng nhập.");
        }

        String phoneNumber = authentication.getName();

        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với số điện thoại: " + phoneNumber));
    }

    // Required current centerId
    private Long requiredCurrentCenterId() {
        Long centerId = TenantContext.getCurrentTenantId();

        if (centerId == null) {
            throw new BadRequestException("Tenant context not resolved. Ensure the user has an active membership.");
        }
        return centerId;
    }

    // Assert owner and center membership
    private void assertOwnerAndCenterMembership(User currentUser, Long centerId) {
        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can manage schedules");
        }
        assertCenterAndMembership(currentUser, centerId);
    }

    // Assert center membership
    private void assertCenterAndMembership(User currentUser, Long centerId) {
        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);

        if(!hasMembership) {
            throw new AccessDeniedException("Người dùng không thuộc trung tâm hiện tại.");
        }
    }
}
