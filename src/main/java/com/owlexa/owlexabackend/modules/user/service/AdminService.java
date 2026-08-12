package com.owlexa.owlexabackend.modules.user.service;

import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.dto.PageResponse;
import com.owlexa.owlexabackend.dto.admin.AdminAuditLogResponse;
import com.owlexa.owlexabackend.dto.admin.AdminCenterResponse;
import com.owlexa.owlexabackend.dto.admin.AdminUserResponse;
import com.owlexa.owlexabackend.entity.AdminAuditAction;
import com.owlexa.owlexabackend.entity.AdminAuditLog;
import com.owlexa.owlexabackend.entity.AdminAuditTargetType;
import com.owlexa.owlexabackend.modules.user.dto.response.AdminStatsResponse;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Membership;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final CenterRepository centerRepository;
    private final MembershipRepository membershipRepository;
    private final AdminAuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        return AdminStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalOwners(userRepository.countByRole(Role.OWNER))
                .totalTeachers(userRepository.countByRole(Role.TEACHER))
                .totalStudents(userRepository.countByRole(Role.STUDENT))
                .totalCashiers(userRepository.countByRole(Role.CASHIER))
                .totalAdmins(userRepository.countByRole(Role.ADMIN))
                .totalCenters(centerRepository.count())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getUsers(String search, Role role, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        var users = userRepository.searchForAdmin(normalizeSearch(search), role, pageable);
        return PageResponse.from(users, this::toUserResponse);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(Long userId) {
        return toUserResponse(findUser(userId));
    }

    @Transactional
    public AdminUserResponse updateUserStatus(
            Long userId,
            boolean active,
            String reason,
            String adminPhoneNumber
    ) {
        User user = findUser(userId);
        if (user.getRole() == Role.ADMIN && !active) {
            throw new BadRequestException("Không thể khóa tài khoản quản trị hệ thống");
        }
        if (user.isActive() == active) {
            return toUserResponse(user);
        }

        User admin = findAdmin(adminPhoneNumber);
        boolean previousActive = user.isActive();
        user.setActive(active);
        auditLogRepository.save(new AdminAuditLog(
                admin,
                active ? AdminAuditAction.USER_UNLOCKED : AdminAuditAction.USER_LOCKED,
                AdminAuditTargetType.USER,
                user.getId(),
                displayName(user),
                statusOf(previousActive),
                statusOf(active),
                reason.trim()
        ));
        return toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminCenterResponse> getCenters(String search, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        var centers = centerRepository.searchForAdmin(normalizeSearch(search), pageable);
        return PageResponse.from(centers, this::toCenterResponse);
    }

    @Transactional(readOnly = true)
    public AdminCenterResponse getCenter(Long centerId) {
        return toCenterResponse(findCenter(centerId));
    }

    @Transactional
    public AdminCenterResponse updateCenterStatus(
            Long centerId,
            boolean active,
            String reason,
            String adminPhoneNumber
    ) {
        Center center = findCenter(centerId);
        if (center.isActive() == active) {
            return toCenterResponse(center);
        }

        User admin = findAdmin(adminPhoneNumber);
        boolean previousActive = center.isActive();
        center.setActive(active);
        auditLogRepository.save(new AdminAuditLog(
                admin,
                active ? AdminAuditAction.CENTER_UNLOCKED : AdminAuditAction.CENTER_LOCKED,
                AdminAuditTargetType.CENTER,
                center.getId(),
                center.getName(),
                statusOf(previousActive),
                statusOf(active),
                reason.trim()
        ));
        return toCenterResponse(center);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminAuditLogResponse> getAuditLogs(
            String search,
            AdminAuditTargetType targetType,
            AdminAuditAction action,
            int page,
            int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var logs = auditLogRepository.searchForAdmin(
                normalizeSearch(search), targetType, action, pageable);
        return PageResponse.from(logs, this::toAuditLogResponse);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private Center findCenter(Long centerId) {
        return centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trung tâm"));
    }

    private User findAdmin(String phoneNumber) {
        User admin = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quản trị viên"));
        if (admin.getRole() != Role.ADMIN) {
            throw new BadRequestException("Chỉ quản trị viên được thực hiện thao tác này");
        }
        return admin;
    }

    private AdminUserResponse toUserResponse(User user) {
        Center center = membershipRepository.findAllByUser_Id(user.getId()).stream()
                .findFirst()
                .map(Membership::getCenter)
                .orElse(null);
        return new AdminUserResponse(
                user.getId(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getRole().name(),
                center == null ? null : center.getId(),
                center == null ? null : center.getName(),
                user.isActive()
        );
    }

    private AdminCenterResponse toCenterResponse(Center center) {
        User owner = center.getOwner();
        LocalDateTime createdAt = center.getCreatedAt() == null
                ? null
                : LocalDateTime.ofInstant(center.getCreatedAt(), ZoneId.systemDefault());
        return new AdminCenterResponse(
                center.getId(),
                center.getName(),
                center.getSubdomain(),
                owner.getId(),
                owner.getFullName(),
                owner.getPhoneNumber(),
                membershipRepository.countByCenter_Id(center.getId()),
                createdAt,
                center.isActive()
        );
    }

    private AdminAuditLogResponse toAuditLogResponse(AdminAuditLog log) {
        User admin = log.getAdmin();
        return new AdminAuditLogResponse(
                log.getId(),
                admin.getId(),
                admin.getFullName(),
                admin.getPhoneNumber(),
                log.getAction().name(),
                log.getTargetType().name(),
                log.getTargetId(),
                log.getTargetName(),
                log.getPreviousStatus(),
                log.getNewStatus(),
                log.getReason(),
                log.getCreatedAt()
        );
    }

    private String displayName(User user) {
        return user.getFullName() == null || user.getFullName().isBlank()
                ? user.getPhoneNumber()
                : user.getFullName();
    }

    private String statusOf(boolean active) {
        return active ? "ACTIVE" : "INACTIVE";
    }

    private String normalizeSearch(String search) {
        return search == null ? "" : search.trim();
    }
}
