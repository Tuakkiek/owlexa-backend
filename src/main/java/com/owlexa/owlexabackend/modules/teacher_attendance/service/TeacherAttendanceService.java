package com.owlexa.owlexabackend.modules.teacher_attendance.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.teacher_attendance.dto.request.TeacherAttendanceMarkRequest;
import com.owlexa.owlexabackend.modules.teacher_attendance.dto.response.TeacherAttendanceResponse;
import com.owlexa.owlexabackend.modules.teacher_attendance.entity.TeacherAttendance;
import com.owlexa.owlexabackend.modules.teacher_attendance.entity.TeacherAttendanceStatus;
import com.owlexa.owlexabackend.modules.teacher_attendance.repository.TeacherAttendanceRepository;
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
public class TeacherAttendanceService {

    private final TeacherAttendanceRepository teacherAttendanceRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    @Transactional
    public List<TeacherAttendanceResponse> mark(TeacherAttendanceMarkRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertIsOwner(currentUser, centerId);

        List<TeacherAttendanceResponse> responses = new ArrayList<>();

        for (TeacherAttendanceMarkRequest.Item item : request.getRecords()) {
            User teacher = userRepository.findById(item.getTeacherUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy giáo viên với ID: " + item.getTeacherUserId()));

            if (teacher.getRole() != Role.TEACHER) {
                throw new BadRequestException("Người dùng không phải là Giáo viên: " + item.getTeacherUserId());
            }

            boolean isMember = membershipRepository.existsByUser_IdAndCenter_Id(
                    teacher.getId(), centerId);
            if (!isMember) {
                throw new BadRequestException(
                        "Giáo viên không thuộc trung tâm này: " + item.getTeacherUserId());
            }

            TeacherAttendance attendance = teacherAttendanceRepository
                    .findByTeacherUser_IdAndDate(teacher.getId(), request.getDate())
                    .orElseGet(() -> {
                        com.owlexa.owlexabackend.modules.user.entity.Center c =
                                new com.owlexa.owlexabackend.modules.user.entity.Center();
                        c.setId(centerId);
                        return TeacherAttendance.builder()
                                .teacherUser(teacher)
                                .center(c)
                                .date(request.getDate())
                                .build();
                    });

            attendance.setStatus(item.getStatus());
            attendance.setMarkedBy(currentUser);
            attendance.setNote(normalizeOptionalText(item.getNote()));

            attendance = teacherAttendanceRepository.save(attendance);
            responses.add(toResponse(attendance));
        }

        return responses;
    }

    @Transactional
    public TeacherAttendanceResponse update(Long id, TeacherAttendanceStatus status, String note) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertIsOwner(currentUser, centerId);

        TeacherAttendance attendance = teacherAttendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thông tin điểm danh giáo viên với ID: " + id));

        if (!attendance.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("Thông tin điểm danh giáo viên thuộc trung tâm khác");
        }

        attendance.setStatus(status);
        attendance.setNote(normalizeOptionalText(note));
        attendance.setMarkedBy(currentUser);

        return toResponse(teacherAttendanceRepository.save(attendance));
    }

    @Transactional
    public void delete(Long id) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertIsOwner(currentUser, centerId);

        TeacherAttendance attendance = teacherAttendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thông tin điểm danh giáo viên với ID: " + id));

        if (!attendance.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("Thông tin điểm danh giáo viên thuộc trung tâm khác");
        }

        teacherAttendanceRepository.delete(attendance);
    }

    @Transactional(readOnly = true)
    public List<TeacherAttendanceResponse> findAll(Long teacherId, LocalDate date,
                                                    LocalDate startDate, LocalDate endDate) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertIsOwner(currentUser, centerId);

        List<TeacherAttendance> attendances;

        if (teacherId != null && date != null) {
            attendances = List.of(teacherAttendanceRepository
                    .findByTeacherUser_IdAndDate(teacherId, date)
                    .orElse(null));
            attendances = attendances.get(0) != null ? attendances : List.of();
        } else if (teacherId != null && startDate != null && endDate != null) {
            attendances = teacherAttendanceRepository
                    .findAllByTeacherUser_IdAndDateBetween(teacherId, startDate, endDate);
        } else if (teacherId != null) {
            attendances = teacherAttendanceRepository
                    .findAllByTeacherUser_IdAndDateBetween(
                            teacherId, LocalDate.now().minusMonths(1), LocalDate.now());
        } else if (date != null) {
            attendances = teacherAttendanceRepository.findAllByCenter_IdAndDate(centerId, date);
        } else if (startDate != null && endDate != null) {
            attendances = teacherAttendanceRepository
                    .findAllByCenter_IdAndDateBetween(centerId, startDate, endDate);
        } else {
            attendances = teacherAttendanceRepository
                    .findAllByCenter_IdAndDate(centerId, LocalDate.now());
        }

        return attendances.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherAttendanceResponse findById(Long id) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertIsOwner(currentUser, centerId);

        TeacherAttendance attendance = teacherAttendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thông tin điểm danh giáo viên với ID: " + id));

        if (!attendance.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("Thông tin điểm danh giáo viên thuộc trung tâm khác");
        }

        return toResponse(attendance);
    }

    private void assertIsOwner(User currentUser, Long centerId) {
        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Chỉ có Chủ trung tâm mới có quyền quản lý điểm danh giáo viên");
        }

        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(
                currentUser.getId(), centerId);
        if (!hasMembership) {
            throw new AccessDeniedException("Người dùng không thuộc trung tâm này");
        }
    }

    private TeacherAttendanceResponse toResponse(TeacherAttendance attendance) {
        return TeacherAttendanceResponse.builder()
                .id(attendance.getId())
                .centerId(attendance.getCenter().getId())
                .teacherUserId(attendance.getTeacherUser().getId())
                .teacherFullName(attendance.getTeacherUser().getFullName())
                .teacherPhoneNumber(attendance.getTeacherUser().getPhoneNumber())
                .date(attendance.getDate())
                .status(attendance.getStatus())
                .note(attendance.getNote())
                .markedByUserId(attendance.getMarkedBy() != null
                        ? attendance.getMarkedBy().getId() : null)
                .createdAt(attendance.getCreatedAt())
                .build();
    }

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new AccessDeniedException("Chưa đăng nhập");
        }

        String phoneNumber = authentication.getName();

        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng hiện tại"));
    }

    private Long requiredCurrentCenterId() {
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new BadRequestException(
                    "Chưa xác định trung tâm hoạt động. Vui lòng đảm bảo người dùng có vai trò trong trung tâm.");
        }
        return centerId;
    }

    private String normalizeOptionalText(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
