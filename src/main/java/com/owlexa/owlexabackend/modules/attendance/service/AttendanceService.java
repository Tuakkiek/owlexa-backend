package com.owlexa.owlexabackend.modules.attendance.service;


import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.modules.attendance.dto.request.AttendanceMarkRequest;
import com.owlexa.owlexabackend.modules.attendance.dto.response.AttendanceResponse;
import com.owlexa.owlexabackend.modules.attendance.dto.response.AttendanceStatsResponse;
import com.owlexa.owlexabackend.modules.attendance.dto.response.ClassSessionResponse;
import com.owlexa.owlexabackend.modules.attendance.entity.Attendance;
import com.owlexa.owlexabackend.modules.attendance.entity.AttendanceStatus;
import com.owlexa.owlexabackend.modules.attendance.repository.AttendanceRepository;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEvent;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventStatus;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleEventRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final FeeRecordRepository feeRecordRepository;

    @Transactional(readOnly = true)
    public List<ClassSessionResponse> findAllClassSessionsByDate(LocalDate date) {
        Long centerId = requiredCurrentCenterId();
        
        List<ScheduleEvent> events = scheduleEventRepository
            .findAllByCenter_IdAndEventDateAndStatusNotOrderByStartTimeAsc(centerId, date, ScheduleEventStatus.CANCELLED);
            
        List<ClassSessionResponse> responses = new ArrayList<>();
        
        for (ScheduleEvent event : events) {
            long studentCount = classEnrollmentRepository.countByClazz_IdAndStatus(
                event.getClazz().getId(), EnrollmentStatus.ACTIVE);
                
            List<Attendance> attendances = attendanceRepository.findAllByScheduleEvent_IdAndDate(event.getId(), date);
            
            int presentCount = 0;
            int absentCount = 0;
            int lateCount = 0;
            int excusedCount = 0;
            
            for (Attendance a : attendances) {
                switch (a.getStatus()) {
                    case PRESENT -> presentCount++;
                    case ABSENT -> absentCount++;
                    case LATE -> lateCount++;
                    case EXCUSED -> excusedCount++;
                }
            }
            
            String status = (attendances.size() >= studentCount && studentCount > 0) ? "COMPLETED" : "PENDING";
            if (studentCount == 0 && !attendances.isEmpty()) {
                status = "COMPLETED"; // Edge case: no active students but attendance was taken somehow
            }
            
            responses.add(ClassSessionResponse.builder()
                .scheduleEventId(event.getId())
                .classId(event.getClazz().getId())
                .className(event.getClazz().getName())
                .teacherUserId(event.getTeacherUser() != null ? event.getTeacherUser().getId() : null)
                .teacherUserFullName(event.getTeacherUser() != null ? event.getTeacherUser().getFullName() : null)
                .roomId(event.getRoom() != null ? event.getRoom().getId() : null)
                .roomName(event.getRoom() != null ? event.getRoom().getName() : null)
                .date(event.getEventDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .attendanceStatus(status)
                .studentCount((int) studentCount)
                .presentCount(presentCount)
                .absentCount(absentCount)
                .lateCount(lateCount)
                .excusedCount(excusedCount)
                .build());
        }
        
        return responses;
    }

    @Transactional(readOnly = true)
    public List<ClassSessionResponse> findTeacherClassSessionsByDate(LocalDate date) {
        User currentUser = getCurrentUser();
        Long teacherUserId = currentUser.getId();
        Long centerId = requiredCurrentCenterId();
        
        List<ScheduleEvent> events = scheduleEventRepository
            .findAllByTeacherUser_IdAndCenter_IdAndEventDateAndStatusNotOrderByStartTimeAsc(teacherUserId, centerId, date, ScheduleEventStatus.CANCELLED);
            
        List<ClassSessionResponse> responses = new ArrayList<>();
        
        for (ScheduleEvent event : events) {
            long studentCount = classEnrollmentRepository.countByClazz_IdAndStatus(
                event.getClazz().getId(), EnrollmentStatus.ACTIVE);
                
            List<Attendance> attendances = attendanceRepository.findAllByScheduleEvent_IdAndDate(event.getId(), date);
            
            int presentCount = 0;
            int absentCount = 0;
            int lateCount = 0;
            int excusedCount = 0;
            
            for (Attendance a : attendances) {
                switch (a.getStatus()) {
                    case PRESENT -> presentCount++;
                    case ABSENT -> absentCount++;
                    case LATE -> lateCount++;
                    case EXCUSED -> excusedCount++;
                }
            }
            
            String status = (attendances.size() >= studentCount && studentCount > 0) ? "COMPLETED" : "PENDING";
            if (studentCount == 0 && !attendances.isEmpty()) {
                status = "COMPLETED"; // Edge case: no active students but attendance was taken somehow
            }
            
            responses.add(ClassSessionResponse.builder()
                .scheduleEventId(event.getId())
                .classId(event.getClazz().getId())
                .className(event.getClazz().getName())
                .teacherUserId(event.getTeacherUser() != null ? event.getTeacherUser().getId() : null)
                .teacherUserFullName(event.getTeacherUser() != null ? event.getTeacherUser().getFullName() : null)
                .roomId(event.getRoom() != null ? event.getRoom().getId() : null)
                .roomName(event.getRoom() != null ? event.getRoom().getName() : null)
                .date(event.getEventDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .attendanceStatus(status)
                .studentCount((int) studentCount)
                .presentCount(presentCount)
                .absentCount(absentCount)
                .lateCount(lateCount)
                .excusedCount(excusedCount)
                .build());
        }
        
        return responses;
    }

    @Transactional(readOnly = true)
    public List<com.owlexa.owlexabackend.modules.attendance.dto.response.StudentClassSessionResponse> findStudentClassSessionsByDate(LocalDate date) {
        User currentUser = getCurrentUser();
        Long studentUserId = currentUser.getId();
        Long centerId = requiredCurrentCenterId();
        
        List<ScheduleEvent> events = scheduleEventRepository
            .findStudentEventsByDate(centerId, studentUserId, date, ScheduleEventStatus.CANCELLED);
            
        List<com.owlexa.owlexabackend.modules.attendance.dto.response.StudentClassSessionResponse> responses = new ArrayList<>();
        
        for (ScheduleEvent event : events) {
            Optional<Attendance> attendanceOpt = attendanceRepository.findByScheduleEvent_IdAndStudentUser_IdAndDate(
                event.getId(), studentUserId, date);
                
            responses.add(com.owlexa.owlexabackend.modules.attendance.dto.response.StudentClassSessionResponse.builder()
                .scheduleEventId(event.getId())
                .classId(event.getClazz().getId())
                .className(event.getClazz().getName())
                .roomName(event.getRoom() != null ? event.getRoom().getName() : null)
                .teacherName(event.getTeacherUser() != null ? event.getTeacherUser().getFullName() : null)
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .attendanceStatus(attendanceOpt.map(Attendance::getStatus).orElse(null))
                .note(attendanceOpt.map(Attendance::getNote).orElse(null))
                .build());
        }
        
        return responses;
    }

    @Transactional
    public List<AttendanceResponse> mark(Long scheduleId, AttendanceMarkRequest request) {
        return markResolvedTarget(findScheduleTarget(scheduleId), request);
    }

    @Transactional
    public List<AttendanceResponse> markScheduleEvent(Long scheduleEventId, AttendanceMarkRequest request) {
        return markResolvedTarget(findScheduleEventTarget(scheduleEventId), request);
    }

    private List<AttendanceResponse> markResolvedTarget(AttendanceTarget target, AttendanceMarkRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (!target.center().getId().equals(centerId)) {
            throw new TenancyViolationException("Attendance target " + target.id() + " belongs to another center");
        }

        assertCanMarkAttendance(currentUser, centerId, target);
        validateAttendanceDate(target, request.getDate());

        if (target.event() != null && target.event().getStatus() == ScheduleEventStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot mark attendance for a cancelled schedule event.");
        }

        if (target.clazz().getStatus() != com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "Attendance can only be marked for ACTIVE classes. Current: " + target.clazz().getStatus());
        }

        List<AttendanceResponse> responses = new ArrayList<>();

        for (AttendanceMarkRequest.Item item : request.getRecords()) {
            User student = userRepository.findById(item.getStudentUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Student not found with id: " + item.getStudentUserId()
                    ));

            if (student.getRole() != Role.STUDENT) {
                throw new BadRequestException("User is not a STUDENT");
            }

            boolean activeEnrollment = classEnrollmentRepository.existsByClazz_IdAndStudentUser_IdAndStatus(
                    target.clazz().getId(),
                    student.getId(),
                    EnrollmentStatus.ACTIVE
            );

            if (!activeEnrollment) {
                throw new BusinessRuleException(
                        "Student is not actively enrolled in this class: " + student.getId()
                );
            }

            boolean hasUnpaidOverdue = feeRecordRepository.countOutstandingDueByStudentAndClass(
                    student.getId(),
                    target.clazz().getId(),
                    List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL, FeeStatus.OVERDUE),
                    request.getDate()
            ) > 0;
            if (hasUnpaidOverdue) {
                throw new BusinessRuleException(
                        "Student has unpaid overdue fees: " + student.getId()
                );
            }

            Attendance attendance = findAttendance(target, student.getId(), request.getDate())
                    .orElseGet(() -> buildAttendance(target, student, request.getDate()));

            attendance.setStatus(item.getStatus());
            attendance.setMarkedBy(currentUser);
            attendance.setNote(normalizeOptionalText(item.getNote()));

            attendance = attendanceRepository.save(attendance);
            responses.add(toResponse(attendance));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> findAllBySchedule(Long scheduleId, LocalDate date) {
        return findAllByResolvedTarget(findScheduleTarget(scheduleId), date);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> findAllByScheduleEvent(Long scheduleEventId, LocalDate date) {
        return findAllByResolvedTarget(findScheduleEventTarget(scheduleEventId), date);
    }

    private List<AttendanceResponse> findAllByResolvedTarget(AttendanceTarget target, LocalDate date) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (!target.center().getId().equals(centerId)) {
            throw new TenancyViolationException("Attendance target " + target.id() + " belongs to another center");
        }

        assertCanViewAttendance(currentUser, centerId, target);
        validateAttendanceDate(target, date);

        List<Attendance> attendances = target.event() != null
                ? attendanceRepository.findAllByScheduleEvent_IdAndDate(target.id(), date)
                : attendanceRepository.findAllBySchedule_IdAndDate(target.id(), date);

        return attendances
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> findMyAttendancesAsStudent(Long classId, LocalDate date) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() != Role.STUDENT) {
            throw new AccessDeniedException("Only STUDENT can access their own attendance");
        }

        List<Attendance> attendances;
        if (classId != null && date != null) {
            attendances = new ArrayList<>();
            attendances.addAll(attendanceRepository.findByStudentUser_IdAndSchedule_Clazz_IdAndDate(
                    currentUser.getId(), classId, date));
            attendances.addAll(attendanceRepository.findByStudentUser_IdAndScheduleEvent_Clazz_IdAndDate(
                    currentUser.getId(), classId, date));
        } else if (classId != null) {
            attendances = new ArrayList<>();
            attendances.addAll(attendanceRepository.findByStudentUser_IdAndSchedule_Clazz_IdAndDateBetween(
                    currentUser.getId(), classId, LocalDate.now().minusMonths(3), LocalDate.now()));
            attendances.addAll(attendanceRepository.findByStudentUser_IdAndScheduleEvent_Clazz_IdAndDateBetween(
                    currentUser.getId(), classId, LocalDate.now().minusMonths(3), LocalDate.now()));
        } else if (date != null) {
            attendances = attendanceRepository.findByStudentUser_IdAndDate(currentUser.getId(), date);
        } else {
            attendances = attendanceRepository.findByStudentUser_IdAndDate(
                    currentUser.getId(), LocalDate.now());
        }

        return attendances.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AttendanceStatsResponse getStats(Long classId, LocalDate startDate, LocalDate endDate) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can view attendance statistics");
        }

        assertCenterMembership(currentUser, centerId);

        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();

        List<Object[]> results = attendanceRepository.countByClassAndDateRangeGroupByStatus(
                classId, startDate, endDate, centerId);

        java.util.Map<String, Long> statusCounts = new java.util.LinkedHashMap<>();
        long total = 0;
        for (Object[] row : results) {
            String status = row[0].toString();
            Long count = (Long) row[1];
            statusCounts.put(status, count);
            total += count;
        }

        long finalTotal = total;
        java.util.Map<String, Double> statusPercentages = new java.util.LinkedHashMap<>();
        statusCounts.forEach((status, count) -> {
            statusPercentages.put(status,
                    finalTotal > 0 ? Math.round(count * 1000.0 / finalTotal) / 10.0 : 0.0);
        });

        return AttendanceStatsResponse.builder()
                .classId(classId)
                .dateRangeLabel(startDate + " → " + endDate)
                .totalStudents(statusCounts.values().stream().mapToLong(Long::longValue).sum())
                .statusCounts(statusCounts)
                .statusPercentages(statusPercentages)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> findAllByClassAndDate(Long classId, LocalDate date) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);

        return attendanceRepository.findAllByClassIdAndDate(classId, date, centerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> findAllByClassAndDateRange(Long classId, LocalDate startDate, LocalDate endDate) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);

        return attendanceRepository.findAllByClassIdAndDateBetween(classId, startDate, endDate, centerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void assertCanMarkAttendance(User currentUser, Long centerId, AttendanceTarget target) {
        if (currentUser.getRole() == Role.OWNER) {
            throw new AccessDeniedException(
                    "OWNER is not allowed to mark student attendance. Only the assigned teacher can mark attendance.");
        }

        if (currentUser.getRole() == Role.TEACHER) {
            boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
            if (!hasMembership) {
                throw new AccessDeniedException("User is not a member of this center");
            }

            if (target.teacherUser() == null) {
                throw new BusinessRuleException(
                        "Schedule " + target.id() + " has no assigned teacher. Cannot mark attendance.");
            }

            if (!target.teacherUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException(
                        "Only the assigned teacher of this schedule can mark attendance. " +
                        "You are not the assigned teacher for schedule " + target.id());
            }
            return;
        }

        throw new AccessDeniedException("Only the assigned teacher can mark student attendance");
    }

    private void assertCanViewAttendance(User currentUser, Long centerId, AttendanceTarget target) {
        assertCenterMembership(currentUser, centerId);

        if (currentUser.getRole() == Role.OWNER) {
            return;
        }

        if (currentUser.getRole() == Role.TEACHER) {
            if (target.teacherUser() == null) {
                throw new BusinessRuleException(
                        "Schedule " + target.id() + " has no assigned teacher. Cannot view attendance.");
            }

            if (!target.teacherUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException(
                        "Only the assigned teacher of this schedule can view attendance. " +
                        "You are not the assigned teacher for schedule " + target.id());
            }
            return;
        }

        throw new AccessDeniedException("Only OWNER or the assigned teacher can view student attendance");
    }

    private void validateAttendanceDate(AttendanceTarget target, LocalDate date) {
        if (target.event() != null && !target.event().getEventDate().equals(date)) {
            throw new BusinessRuleException(
                    "Attendance date must match schedule event date: " + target.event().getEventDate());
        }

        if (target.schedule() != null
                && target.schedule().getDayOfWeek() != null
                && !target.schedule().getDayOfWeek().equals(date.getDayOfWeek())) {
            throw new BusinessRuleException(
                    "Attendance date must match schedule day of week: " + target.schedule().getDayOfWeek());
        }
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        Class clazz = attendance.getScheduleEvent() != null
                ? attendance.getScheduleEvent().getClazz()
                : attendance.getSchedule().getClazz();
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .scheduleId(attendance.getScheduleEvent() != null
                        ? null
                        : attendance.getSchedule().getId())
                .scheduleEventId(attendance.getScheduleEvent() != null ? attendance.getScheduleEvent().getId() : null)
                .classId(clazz.getId())
                .centerId(attendance.getCenter().getId())
                .studentUserId(attendance.getStudentUser().getId())
                .studentPhoneNumber(attendance.getStudentUser().getPhoneNumber())
                .studentFullName(attendance.getStudentUser().getFullName())
                .date(attendance.getDate())
                .status(attendance.getStatus())
                .note(attendance.getNote())
                .markedByUserId(attendance.getMarkedBy() != null ? attendance.getMarkedBy().getId() : null)
                .createdAt(attendance.getCreatedAt())
                .build();
    }

    private AttendanceTarget findScheduleTarget(Long id) {
        return scheduleRepository.findById(id)
                .map(schedule -> new AttendanceTarget(
                        schedule.getId(),
                        schedule.getCenter(),
                        schedule.getClazz(),
                        schedule.getTeacherUser(),
                        schedule,
                        null
                ))
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));
    }

    private AttendanceTarget findScheduleEventTarget(Long id) {
        return scheduleEventRepository.findById(id)
                .map(event -> new AttendanceTarget(
                        event.getId(),
                        event.getCenter(),
                        event.getClazz(),
                        event.getTeacherUser(),
                        null,
                        event
                ))
                .orElseThrow(() -> new ResourceNotFoundException("Schedule event not found with id: " + id));
    }

    private java.util.Optional<Attendance> findAttendance(AttendanceTarget target, Long studentUserId, LocalDate date) {
        if (target.event() != null) {
            return attendanceRepository.findByScheduleEvent_IdAndStudentUser_IdAndDate(
                    target.id(), studentUserId, date);
        }
        return attendanceRepository.findBySchedule_IdAndStudentUser_IdAndDate(
                target.id(), studentUserId, date);
    }

    private Attendance buildAttendance(AttendanceTarget target, User student, LocalDate date) {
        Attendance.AttendanceBuilder builder = Attendance.builder()
                .studentUser(student)
                .center(target.center())
                .date(date);
        if (target.event() != null) {
            builder.scheduleEvent(target.event());
        } else {
            builder.schedule(target.schedule());
        }
        return builder.build();
    }

    private record AttendanceTarget(
            Long id,
            com.owlexa.owlexabackend.modules.user.entity.Center center,
            Class clazz,
            User teacherUser,
            Schedule schedule,
            ScheduleEvent event
    ) {}

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new AccessDeniedException("User is not authenticated");
        }

        String phoneNumber = authentication.getName();

        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private Long requiredCurrentCenterId() {
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new BadRequestException("Tenant context not resolved. Ensure the user has an active membership.");
        }
        return centerId;
    }

    private void assertCenterMembership(User currentUser, Long centerId) {
        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
        if (!hasMembership) {
            throw new AccessDeniedException("User is not a member of this center");
        }
    }

    private String normalizeOptionalText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
