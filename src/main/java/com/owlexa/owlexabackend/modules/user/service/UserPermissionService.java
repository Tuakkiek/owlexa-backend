package com.owlexa.owlexabackend.modules.user.service;

import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.user.dto.request.BulkPermissionOverrideRequest;
import com.owlexa.owlexabackend.modules.user.dto.request.PermissionOverrideItem;
import com.owlexa.owlexabackend.modules.user.dto.response.EffectivePermission;
import com.owlexa.owlexabackend.modules.user.dto.response.PermissionResponse;
import com.owlexa.owlexabackend.modules.user.dto.response.UserPermissionsResponse;
import com.owlexa.owlexabackend.modules.user.entity.Permission;
import com.owlexa.owlexabackend.modules.user.entity.RolePermission;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.entity.UserPermission;
import com.owlexa.owlexabackend.modules.user.repository.PermissionRepository;
import com.owlexa.owlexabackend.modules.user.repository.RolePermissionRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserPermissionRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPermissionService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final UserSessionRepository sessionRepository;
    private final PermissionResolver permissionResolver;

    // ═══════════════════════════════════════════════════════════════
    // CATALOG
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<PermissionResponse> listAllPermissions() {
        return permissionRepository.findAllByOrderByCodeAsc()
                .stream()
                .map(p -> PermissionResponse.builder()
                        .code(p.getCode())
                        .description(p.getDescription())
                        .build())
                .toList();
    }

    // ═══════════════════════════════════════════════════════════════
    // EFFECTIVE PERMISSIONS (role-scoped — only the user's role permissions)
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public UserPermissionsResponse getEffectivePermissions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // Load only the permissions that belong to this user's ROLE
        List<RolePermission> rolePerms = rolePermissionRepository.findAllByRole(user.getRole());

        // Load disabled permission codes for this user
        Set<String> disabledCodes = userPermissionRepository.findAllByUser_Id(userId)
                .stream()
                .map(UserPermission::getPermission)
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        List<EffectivePermission> permissions = new ArrayList<>();
        for (RolePermission rp : rolePerms) {
            Permission perm = rp.getPermission();
            if (perm == null || perm.getCode() == null) continue;

            String code = perm.getCode();
            boolean enabled = !disabledCodes.contains(code);

            permissions.add(EffectivePermission.builder()
                    .code(code)
                    .description(perm.getDescription())
                    .source(enabled ? "ENABLED" : "DISABLED")
                    .build());
        }

        return UserPermissionsResponse.builder()
                .userId(user.getId())
                .roleName(user.getRole().name())
                .permissions(permissions)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    // BULK UPDATE OVERRIDES
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public UserPermissionsResponse applyOverrides(Long userId, BulkPermissionOverrideRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        if (request.getOverrides() == null || request.getOverrides().isEmpty()) {
            // Empty list = remove all overrides → re-enable all role permissions
            removeAllOverrides(userId);
            return getEffectivePermissions(userId);
        }

        // Load role permission codes for boundary validation
        Set<String> rolePermissionCodes = rolePermissionRepository.findAllByRole(user.getRole())
                .stream()
                .map(RolePermission::getPermission)
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        // Validate: each override must target a permission within the user's role
        for (PermissionOverrideItem item : request.getOverrides()) {
            validateOverrideType(item.getType());
            if ("INHERIT".equalsIgnoreCase(item.getType())) {
                continue; // INHERIT means "remove override" — always valid
            }
            if (!rolePermissionCodes.contains(item.getPermissionCode())) {
                throw new BadRequestException(
                        "Quyền '" + item.getPermissionCode()
                        + "' không thuộc vai trò " + user.getRole().name()
                        + ". Quyền của người dùng phải thuộc danh mục quyền của vai trò.");
            }
            permissionRepository.findByCode(item.getPermissionCode())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy quyền: " + item.getPermissionCode()));
        }

        // Delete existing overrides
        userPermissionRepository.deleteByUser_Id(userId);
        userPermissionRepository.flush();

        // Create new overrides: only DISABLED/DENY records disable the permission.
        // INHERIT is skipped (re-enables).
        for (PermissionOverrideItem item : request.getOverrides()) {
            if ("INHERIT".equalsIgnoreCase(item.getType())) {
                continue;
            }

            // DISABLED or DENY: create a user_permission row to disable this permission
            Permission permission = permissionRepository.findByCode(item.getPermissionCode())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy quyền: " + item.getPermissionCode()));

            UserPermission up = new UserPermission();
            up.setUser(user);
            up.setPermission(permission);
            userPermissionRepository.save(up);
        }

        permissionResolver.evictCache(userId);

        // Revoke all active sessions so the user must re-login with updated permissions.
        sessionRepository.deactivateAllByUserIdWithReason(userId, "PERMISSION_CHANGED");

        return getEffectivePermissions(userId);
    }

    // ═══════════════════════════════════════════════════════════════
    // SINGLE OVERRIDE UPDATE
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public EffectivePermission updateSingleOverride(Long userId, String permissionCode, String type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        validateOverrideType(type);

        // Validate: permission must belong to user's role
        Set<String> roleCodes = rolePermissionRepository.findAllByRole(user.getRole())
                .stream()
                .map(RolePermission::getPermission)
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        if (!roleCodes.contains(permissionCode)) {
            throw new BadRequestException(
                    "Quyền '" + permissionCode + "' không thuộc vai trò " + user.getRole().name());
        }

        Permission permission = permissionRepository.findByCode(permissionCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy quyền: " + permissionCode));

        // Remove existing override (if any)
        userPermissionRepository.findByUser_IdAndPermission_Code(userId, permissionCode)
                .ifPresent(userPermissionRepository::delete);

        if ("DENY".equalsIgnoreCase(type) || "DISABLED".equalsIgnoreCase(type)) {
            // Create a disable record
            UserPermission up = new UserPermission();
            up.setUser(user);
            up.setPermission(permission);
            userPermissionRepository.save(up);
        }
        // ALLOW and INHERIT both mean "enabled" — no record needed

        permissionResolver.evictCache(userId);
        return buildEffectivePermission(permission, user,
                "DENY".equalsIgnoreCase(type) || "DISABLED".equalsIgnoreCase(type));
    }

    // ═══════════════════════════════════════════════════════════════
    // REMOVE ALL OVERRIDES (restore role defaults)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void removeAllOverrides(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId);
        }
        userPermissionRepository.deleteByUser_Id(userId);
        permissionResolver.evictCache(userId);
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void validateOverrideType(String type) {
        if (type == null || type.isBlank()) {
            throw new BadRequestException("Loại ghi đè không được để trống");
        }
        String upper = type.trim().toUpperCase();
        // Accept DISABLED (new) + DENY/INHERIT (backward compat). Reject ALLOW.
        if (!upper.equals("DISABLED") && !upper.equals("DENY") && !upper.equals("INHERIT")) {
            throw new BadRequestException(
                    "Loại ghi đè không hợp lệ: " + type + ". Phải là DISABLED hoặc INHERIT.");
        }
    }

    private EffectivePermission buildEffectivePermission(Permission permission, User user,
                                                          boolean disabled) {
        return EffectivePermission.builder()
                .code(permission.getCode())
                .description(permission.getDescription())
                .source(disabled ? "DISABLED" : "ENABLED")
                .build();
    }
}
