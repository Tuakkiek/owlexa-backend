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
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
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

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ScheduleRepository scheduleRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final FeeRecordRepository feeRecordRepository;

    @Transactional
    public List<AttendanceResponse> mark(Long scheduleId, AttendanceMarkRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + scheduleId));

        if (!schedule.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Schedule " + scheduleId + " belongs to another center");
        }

        assertCanMarkAttendance(currentUser, centerId, schedule);

        if (schedule.getClazz().getStatus() != com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus.IN_PROGRESS) {
            throw new BusinessRuleException(
                    "Attendance can only be marked for IN_PROGRESS classes. Current: " + schedule.getClazz().getStatus());
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
                    schedule.getClazz().getId(),
                    student.getId(),
                    EnrollmentStatus.ACTIVE
            );

            if (!activeEnrollment) {
                throw new BusinessRuleException(
                        "Student is not actively enrolled in this class: " + student.getId()
                );
            }

            boolean hasUnpaidOverdue = feeRecordRepository
                    .existsByStudentUser_IdAndClazz_IdAndStatusAndDueDateBefore(
                            student.getId(),
                            schedule.getClazz().getId(),
                            FeeStatus.UNPAID,
                            request.getDate() != null ? request.getDate() : LocalDate.now()
                    );
            if (hasUnpaidOverdue) {
                throw new BusinessRuleException(
                        "Student has unpaid overdue fees: " + student.getId()
                );
            }

            Attendance attendance = attendanceRepository
                    .findBySchedule_IdAndStudentUser_IdAndDate(
                            scheduleId,
                            student.getId(),
                            request.getDate()
                    )
                    .orElseGet(() -> Attendance.builder()
                            .schedule(schedule)
                            .studentUser(student)
                            .center(schedule.getCenter())
                            .date(request.getDate())
                            .build());

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
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + scheduleId));

        if (!schedule.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Schedule " + scheduleId + " belongs to another center");
        }

        return attendanceRepository.findAllBySchedule_IdAndDate(scheduleId, date)
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
            attendances = attendanceRepository.findByStudentUser_IdAndSchedule_Clazz_IdAndDate(
                    currentUser.getId(), classId, date);
        } else if (classId != null) {
            attendances = attendanceRepository.findByStudentUser_IdAndSchedule_Clazz_IdAndDateBetween(
                    currentUser.getId(), classId, LocalDate.now().minusMonths(3), LocalDate.now());
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

    private void assertCanMarkAttendance(User currentUser, Long centerId, Schedule schedule) {
        if (currentUser.getRole() == Role.OWNER) {
            throw new AccessDeniedException(
                    "OWNER is not allowed to mark student attendance. Only the assigned teacher can mark attendance.");
        }

        if (currentUser.getRole() == Role.TEACHER) {
            boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
            if (!hasMembership) {
                throw new AccessDeniedException("User is not a member of this center");
            }

            if (schedule.getTeacherUser() == null) {
                throw new BusinessRuleException(
                        "Schedule " + schedule.getId() + " has no assigned teacher. Cannot mark attendance.");
            }

            if (!schedule.getTeacherUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException(
                        "Only the assigned teacher of this schedule can mark attendance. " +
                        "You are not the assigned teacher for schedule " + schedule.getId());
            }
            return;
        }

        throw new AccessDeniedException("Only the assigned teacher can mark student attendance");
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .scheduleId(attendance.getSchedule().getId())
                .classId(attendance.getSchedule().getClazz().getId())
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
