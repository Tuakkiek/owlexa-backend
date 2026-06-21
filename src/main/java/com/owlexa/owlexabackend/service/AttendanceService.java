package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.request.AttendanceMarkRequest;
import com.owlexa.owlexabackend.dto.response.AttendanceResponse;
import com.owlexa.owlexabackend.entity.*;
import com.owlexa.owlexabackend.exception.BadRequestException;
import com.owlexa.owlexabackend.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.filter.TenantFilter;
import com.owlexa.owlexabackend.repository.AttendanceRepository;
import com.owlexa.owlexabackend.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.repository.MembershipRepository;
import com.owlexa.owlexabackend.repository.ScheduleRepository;
import com.owlexa.owlexabackend.repository.UserRepository;
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

    @Transactional
    public List<AttendanceResponse> mark(Long scheduleId, AttendanceMarkRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + scheduleId));

        if (!schedule.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("You do not have permission to manage this schedule");
        }

        assertCanMarkAttendance(currentUser, centerId, schedule);

        List<AttendanceResponse> responses = new ArrayList<>();

        for (AttendanceMarkRequest.Item item : request.getRecords()) {
            User student = userRepository.findById(item.getStudentUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Student not found with id: " + item.getStudentUserId()
                    ));

            if (student.getRole() != Role.STUDENT) {
                throw new BadRequestException("User is not a STUDENT");
            }

            boolean activeEnrollment = classEnrollmentRepository.existsByClazzIdAndStudentUserIdAndStatus(
                    schedule.getClazz().getId(),
                    student.getId(),
                    EnrollmentStatus.ACTIVE
            );

            if (!activeEnrollment) {
                throw new BadRequestException(
                        "Student is not actively enrolled in this class: " + student.getId()
                );
            }

            Attendance attendance = attendanceRepository
                    .findByScheduleIdAndStudentUserIdAndSessionDate(
                            scheduleId,
                            student.getId(),
                            request.getSessionDate()
                    )
                    .orElseGet(() -> Attendance.builder()
                            .schedule(schedule)
                            .studentUser(student)
                            .center(schedule.getCenter())
                            .sessionDate(request.getSessionDate())
                            .build());

            attendance.setStatus(item.getStatus());
            attendance.setNotedByUser(currentUser);
            attendance.setNote(normalizeOptionalText(item.getNote()));

            attendance = attendanceRepository.save(attendance);
            responses.add(toResponse(attendance));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> findAllBySchedule(Long scheduleId, LocalDate sessionDate) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + scheduleId));

        if (!schedule.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("You do not have permission to view this schedule");
        }

        return attendanceRepository.findAllByScheduleIdAndSessionDate(scheduleId, sessionDate)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> findMyClassAttendances(Long classId, LocalDate sessionDate) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);

        List<Attendance> attendances = attendanceRepository.findAllByScheduleIdAndSessionDate(classId, sessionDate);

        return attendances.stream()
                .map(this::toResponse)
                .toList();
    }

    private void assertCanMarkAttendance(User currentUser, Long centerId, Schedule schedule) {
        if (currentUser.getRole() == Role.OWNER) {
            boolean hasMembership = membershipRepository.existsByUserIdAndCenterId(currentUser.getId(), centerId);
            if (!hasMembership) {
                throw new AccessDeniedException("User is not a member of this center");
            }
            return;
        }

        if (currentUser.getRole() == Role.TEACHER) {
            boolean hasMembership = membershipRepository.existsByUserIdAndCenterId(currentUser.getId(), centerId);
            if (!hasMembership) {
                throw new AccessDeniedException("User is not a member of this center");
            }

            if (!schedule.getTeacherUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Only assigned teacher can mark attendance");
            }
            return;
        }

        throw new AccessDeniedException("Only OWNER or TEACHER can mark attendance");
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
                .sessionDate(attendance.getSessionDate())
                .status(attendance.getStatus())
                .note(attendance.getNote())
                .notedByUserId(attendance.getNotedByUser().getId())
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
        Long centerId = TenantFilter.getCurrentCenterId();
        if (centerId == null) {
            throw new BadRequestException("Missing X-Tenant-ID header");
        }
        return centerId;
    }

    private void assertCenterMembership(User currentUser, Long centerId) {
        boolean hasMembership = membershipRepository.existsByUserIdAndCenterId(currentUser.getId(), centerId);
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