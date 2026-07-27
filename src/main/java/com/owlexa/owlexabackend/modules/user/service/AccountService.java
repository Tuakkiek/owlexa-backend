package com.owlexa.owlexabackend.modules.user.service;

import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.user.dto.request.ChangePasswordRequest;
import com.owlexa.owlexabackend.modules.user.dto.request.UpdateAccountRequest;
import com.owlexa.owlexabackend.modules.user.dto.response.AccountResponse;
import com.owlexa.owlexabackend.modules.user.entity.Membership;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Self-service "tài khoản của tôi": mỗi user chỉ xem/sửa được dữ liệu của
 * chính mình (không phải quản trị người dùng khác — xem OwnerUserController,
 * StudentService, TeacherService, CashierController cho việc đó).
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PermissionResolver permissionResolver;
    private final PasswordEncoder passwordEncoder;

    // ═══════════════════════════════════════════════════════════════
    // VIEW
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public AccountResponse getMyAccount() {
        User currentUser = getCurrentUser();
        return toResponse(currentUser);
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE PROFILE
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public AccountResponse updateMyAccount(UpdateAccountRequest request) {
        User currentUser = getCurrentUser();

        currentUser.setFullName(request.getFullName());
        currentUser.setEmail(request.getEmail());
        userRepository.save(currentUser);

        return toResponse(currentUser);
    }

    // ═══════════════════════════════════════════════════════════════
    // CHANGE PASSWORD
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void changeMyPassword(ChangePasswordRequest request) {
        User currentUser = getCurrentUser();

        verifyPassword(request.getCurrentPassword(), currentUser);

        String encoded = passwordEncoder.encode(request.getNewPassword());
        userRepository.updatePasswordById(currentUser.getId(), encoded);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private AccountResponse toResponse(User user) {
        Membership firstMembership = membershipRepository.findAllByUser_Id(user.getId())
                .stream()
                .findFirst()
                .orElse(null);

        String centerName = firstMembership != null ? firstMembership.getCenter().getName() : null;
        Long centerId = firstMembership != null ? firstMembership.getCenter().getId() : null;

        List<String> permissions = new ArrayList<>(
                permissionResolver.resolvePermissions(user.getId(), user.getRole())
        );

        return AccountResponse.builder()
                .userId(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roleName(user.getRole() != null ? user.getRole().name() : null)
                .centerName(centerName)
                .centerId(centerId)
                .permissions(permissions)
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

    private void verifyPassword(String rawPassword, User user) {
        boolean bcryptMatch = passwordEncoder.matches(rawPassword, user.getPassword());
        boolean legacyMatch = rawPassword != null && rawPassword.equals(user.getPassword());
        if (!bcryptMatch && !legacyMatch) {
            throw new BadRequestException("Mật khẩu hiện tại không chính xác");
        }
    }
}
