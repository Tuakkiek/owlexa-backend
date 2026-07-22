package com.owlexa.owlexabackend.modules.class_management.service;
import com.owlexa.owlexabackend.modules.class_management.dto.request.ScheduleRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.response.ScheduleResponse;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
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
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.class_management.service.validation.ClassLifecycleValidator;
import com.owlexa.owlexabackend.modules.class_management.service.validation.TimeRangeValidator;
import com.owlexa.owlexabackend.modules.class_management.service.validation.RoomConflictValidator;
import com.owlexa.owlexabackend.modules.class_management.service.validation.TeacherConflictValidator;
import com.owlexa.owlexabackend.modules.class_management.service.validation.StudentConflictValidator;
import com.owlexa.owlexabackend.modules.class_management.service.validation.ScheduleValidationContext;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final MembershipRepository membershipRepository;
    private final ScheduleRepository scheduleRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final RoomRepository roomRepository;

    private final ClassLifecycleValidator classLifecycleValidator;
    private final TimeRangeValidator timeRangeValidator;
    private final RoomConflictValidator roomConflictValidator;
    private final TeacherConflictValidator teacherConflictValidator;
    private final StudentConflictValidator studentConflictValidator;

    // Create
    @Transactional
    public ScheduleResponse create(Long classId, ScheduleRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with classId: " + classId));

        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Class " + classId + " belongs to another center");
        }

        User teacher = userRepository.findById(request.getTeacherUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with teacherUserId: " + request.getTeacherUserId()));

        if (teacher.getRole() != Role.TEACHER) {
            throw new BadRequestException("User is not a TEACHER");
        }

        boolean teacherInCenter = membershipRepository
                .findByUser_IdAndCenter_IdAndUserRole(teacher.getId(), centerId, Role.TEACHER)
                .isPresent();

        if(!teacherInCenter) {
            throw new BadRequestException("Teacher is not member of this center");
        }

        Room room = roomRepository.findByIdAndCenter_Id(request.getRoomId(), centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + request.getRoomId() + " in this center"));

        if (scheduleRepository.existsByClazz_IdAndDayOfWeekAndStartTimeAndCenter_Id(
                classId,
                request.getDayOfWeek() == null ? null : DayOfWeek.of(request.getDayOfWeek()),
                request.getStartTime(),
                centerId
        )) {
            throw new DuplicateResourceException("Schedule already exists for this class at this time");
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
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        if(!clazz.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Class " + classId + " belongs to another center");
        }

        return scheduleRepository.findAllByClazz_IdAndCenter_Id(classId, centerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Find all by teacher
    @Transactional(readOnly = true)
    public List<ScheduleResponse> findAllByTeacher(Long teacherUserId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterAndMembership(currentUser, centerId);

        User teacher = userRepository.findById(teacherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherUserId));

        if (!teacher.getRole().equals(Role.TEACHER)) {
            throw new BadRequestException("User is not TEACHER");
        }

        return scheduleRepository.findAllByTeacherUser_IdAndCenter_Id(teacherUserId, centerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Find my schedules — for logged-in TEACHER
    @Transactional(readOnly = true)
    public List<ScheduleResponse> findMySchedules() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("Only TEACHER can access their own schedules");
        }

        return scheduleRepository.findAllByTeacherUser_IdAndCenter_Id(currentUser.getId(), centerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Find all active schedules in center — for OWNER attendance overview
    @Transactional(readOnly = true)
    public List<ScheduleResponse> findAllForOwner() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can access all schedules");
        }

        return scheduleRepository.findAllByCenter_Id(centerId)
                .stream()
                .filter(s -> s.getType() != ScheduleType.CANCELLED)
                .map(this::toResponse)
                .toList();
    }

    // Find my schedules — for logged-in STUDENT (via enrolled classes)
    @Transactional(readOnly = true)
    public List<ScheduleResponse> findMySchedulesAsStudent() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.STUDENT) {
            throw new AccessDeniedException("Only STUDENT can access their own schedules");
        }

        List<Long> enrolledClassIds = classEnrollmentRepository
                .findAllByStudentUser_IdAndCenter_Id(currentUser.getId(), centerId)
                .stream()
                .map(e -> e.getClazz().getId())
                .toList();

        return enrolledClassIds.stream()
                .flatMap(classId -> scheduleRepository.findAllByClazz_IdAndCenter_Id(classId, centerId).stream())
                .map(this::toResponse)
                .toList();
    }

    // Find my classes — for logged-in TEACHER (distinct classes from schedules)
    @Transactional(readOnly = true)
    public List<Long> findMyClassIds() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("Only TEACHER can access their own classes");
        }

        return scheduleRepository.findAllByTeacherUser_IdAndCenter_Id(currentUser.getId(), centerId)
                .stream()
                .map(s -> s.getClazz().getId())
                .distinct()
                .toList();
    }

    // Update
    @Transactional
    public ScheduleResponse update(Long classId, Long scheduleId, ScheduleRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + scheduleId));

        if(!schedule.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Schedule " + scheduleId + " belongs to another center");
        }

        User teacher = userRepository.findById(request.getTeacherUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher not found with id: " + request.getTeacherUserId()
                ));

        if(!teacher.getRole().equals(Role.TEACHER)) {
            throw new BadRequestException("User is not TEACHER");
        }

        boolean teacherInCenter = membershipRepository
                .findByUser_IdAndCenter_IdAndUserRole(
                        request.getTeacherUserId(),
                        centerId,
                        Role.TEACHER
                ).isPresent();
        if (!teacherInCenter) {
            throw new BadRequestException("Teacher is not a member of this center");
        }

        Room room = roomRepository.findByIdAndCenter_Id(request.getRoomId(), centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + request.getRoomId() + " in this center"));

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
                        "Schedule not found with id: " + scheduleId
                ));

        if(!schedule.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Schedule " + scheduleId + " belongs to another center");
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
                        "Schedule not found with id: " + scheduleId
                ));

        if (!schedule.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Schedule " + scheduleId + " belongs to another center");
        }

        schedule.setType(newType);
        schedule = scheduleRepository.save(schedule);

        return toResponse(schedule);
    }

    // Helper
    // Validate time range
    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BadRequestException("startTime must be before endTime");
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
                .createdAt(schedule.getCreatedAt())
                .build();
    }

    // Get current user
    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new AccessDeniedException("User is not authenticated");
        }

        String phoneNumber = authentication.getName();

        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with phoneNumber: " + phoneNumber));
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
            throw new AccessDeniedException("User not found a member of this center");
        }
    }
}
