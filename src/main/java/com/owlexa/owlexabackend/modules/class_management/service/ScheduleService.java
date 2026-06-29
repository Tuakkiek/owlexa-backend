package com.owlexa.owlexabackend.modules.class_management.service;
import com.owlexa.owlexabackend.modules.class_management.dto.request.ScheduleRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.response.ScheduleResponse;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.filter.TenantFilter;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserSessionRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserPermissionRepository;
import com.owlexa.owlexabackend.modules.user.repository.PermissionRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.attendance.repository.AttendanceRepository;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.payment.repository.PaymentRepository;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.mocktest.repository.MockTestRepository;
import com.owlexa.owlexabackend.modules.mocktest.repository.MockTestQuestionRepository;
import com.owlexa.owlexabackend.modules.mocktest.repository.MockTestAttemptRepository;
import com.owlexa.owlexabackend.modules.mocktest.repository.MockTestAttemptAnswerRepository;
import com.owlexa.owlexabackend.modules.essay.repository.EssaySubmissionRepository;
import com.owlexa.owlexabackend.modules.essay.repository.EssayRubricRepository;
import com.owlexa.owlexabackend.modules.essay.repository.EssayGradingResultRepository;
import com.owlexa.owlexabackend.modules.document.repository.StudentDocumentRepository;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final UserRepository userRepository;
    private final CenterRepository centerRepository;
    private final ClassRepository classRepository;
    private final MembershipRepository membershipRepository;
    private final ScheduleRepository scheduleRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;

    // Create
    @Transactional
    public ScheduleResponse create(Long classId, ScheduleRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with classId: " + classId));

        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("You do not have permission this center");
        }

        User teacher = userRepository.findById(request.getTeacherUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with teacherUserId: " + request.getTeacherUserId()));

        if (teacher.getRole() != Role.TEACHER) {
            throw new BadRequestException("User is not a TEACHER");
        }

        boolean teacherInCenter = membershipRepository
                .findByUserIdAndCenterIdAndUserRole(teacher.getId(), centerId, Role.TEACHER)
                .isPresent();

        if(!teacherInCenter) {
            throw new BadRequestException("Teacher is not member of this center");
        }

        validateTimeRange(request.getStartTime(), request.getEndTime());

        if (scheduleRepository.existsByClazzIdAndDayOfWeekAndStartTimeAndCenterId(
                classId,
                request.getDayOfWeek(),
                request.getEndTime(),
                centerId
        )) {
            throw new DuplicateResourceException("Schedule already exists for this class at this time");
        }

        Schedule schedule = Schedule.builder()
                .clazz(clazz)
                .center(clazz.getCenter())
                .teacherUser(teacher)
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .room(request.getRoom().trim())
                .isActive(true)
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
            throw new AccessDeniedException("You do not have permission to view this class");
        }

        return scheduleRepository.findAllByClazzIdAndCenterId(classId, centerId)
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

        return scheduleRepository.findAllByTeacherUserIdAndCenterId(teacherUserId, centerId)
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

        return scheduleRepository.findAllByTeacherUserIdAndCenterId(currentUser.getId(), centerId)
                .stream()
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
                .findAllByStudentUserIdAndCenterId(currentUser.getId(), centerId)
                .stream()
                .map(e -> e.getClazz().getId())
                .toList();

        return enrolledClassIds.stream()
                .flatMap(classId -> scheduleRepository.findAllByClazzIdAndCenterId(classId, centerId).stream())
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

        return scheduleRepository.findAllByTeacherUserIdAndCenterId(currentUser.getId(), centerId)
                .stream()
                .map(s -> s.getClazz().getId())
                .distinct()
                .toList();
    }

    // Update
    @Transactional
    public ScheduleResponse update(Long scheduleId, ScheduleRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + scheduleId));

        if(!schedule.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("You do not have permission to manage this schedule");
        }

        User teacher = userRepository.findById(request.getTeacherUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher not found with id: " + request.getTeacherUserId()
                ));

        if(!teacher.getRole().equals(Role.TEACHER)) {
            throw new BadRequestException("User is not TEACHER");
        }

        boolean teacherInCenter = membershipRepository
                .findByUserIdAndCenterIdAndUserRole(
                        request.getTeacherUserId(),
                        centerId,
                        Role.TEACHER
                ).isPresent();
        if (!teacherInCenter) {
            throw new BadRequestException("Teacher is not a member of this center");
        }

        validateTimeRange(request.getStartTime(), request.getEndTime());

        schedule.setTeacherUser(teacher);
        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setRoom(request.getRoom().trim());

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
            throw new AccessDeniedException("You do not have permission to manage this schedule");
        }

        scheduleRepository.delete(schedule);
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
                .teacherUserId(schedule.getTeacherUser().getId())
                .teacherUserFullName(schedule.getTeacherUser().getFullName())
                .teacherPhoneNumber(schedule.getTeacherUser().getPhoneNumber())
                .dayOfWeek(schedule.getDayOfWeek())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .room(schedule.getRoom())
                .isActive(schedule.getIsActive())
                .createAt(schedule.getCreatedAt())
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
        Long centerId = TenantFilter.getCurrentCenterId();

        if (centerId == null) {
            throw new BadRequestException("Missing X-Tenant-ID header");
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
        boolean hasMembership = membershipRepository.existsByUserIdAndCenterId(currentUser.getId(), centerId);

        if(!hasMembership) {
            throw new AccessDeniedException("User not found a member of this center");
        }
    }
}
