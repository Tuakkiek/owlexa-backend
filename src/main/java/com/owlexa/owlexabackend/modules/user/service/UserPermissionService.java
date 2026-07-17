package com.owlexa.owlexabackend.modules.user.service;

import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.user.dto.request.BulkPermissionOverrideRequest;
import com.owlexa.owlexabackend.modules.user.dto.request.PermissionOverrideItem;
import com.owlexa.owlexabackend.modules.user.dto.response.EffectivePermission;
import com.owlexa.owlexabackend.modules.user.dto.response.PermissionResponse;
import com.owlexa.owlexabackend.modules.user.dto.response.UserPermissionsResponse;
import com.owlexa.owlexabackend.modules.user.entity.Permission;
import com.owlexa.owlexabackend.modules.user.entity.PermissionOverrideType;
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
import java.util.Map;
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
    // EFFECTIVE PERMISSIONS (with source annotation)
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public UserPermissionsResponse getEffectivePermissions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Load all known permissions
        List<Permission> allPermissions = permissionRepository.findAllByOrderByCodeAsc();

        // Load role defaults
        Set<String> roleDefaultCodes = rolePermissionRepository.findAllByRole(user.getRole())
                .stream()
                .map(RolePermission::getPermission)
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        // Load user overrides — keyed by permission code
        Map<String, PermissionOverrideType> overrides = userPermissionRepository
                .findAllByUser_Id(userId)
                .stream()
                .collect(Collectors.toMap(
                        up -> up.getPermission().getCode(),
                        UserPermission::getType,
                        (existing, replacement) -> replacement));

        List<EffectivePermission> permissions = new ArrayList<>();
        for (Permission perm : allPermissions) {
            String code = perm.getCode();
            EffectivePermission.EffectivePermissionBuilder builder = EffectivePermission.builder()
                    .code(code)
                    .description(perm.getDescription());

            if (overrides.containsKey(code)) {
                PermissionOverrideType type = overrides.get(code);
                builder.source(type == PermissionOverrideType.ALLOW ? "ALLOW" : "DENY");
            } else if (roleDefaultCodes.contains(code)) {
                builder.source("ROLE_DEFAULT");
            } else {
                // Permission exists in system but is neither granted by role nor overridden
                continue;
            }

            permissions.add(builder.build());
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (request.getOverrides() == null || request.getOverrides().isEmpty()) {
            // Empty list = remove all overrides
            removeAllOverrides(userId);
            return getEffectivePermissions(userId);
        }

        // Validate all permission codes exist
        for (PermissionOverrideItem item : request.getOverrides()) {
            validateOverrideType(item.getType());
            if (!"INHERIT".equalsIgnoreCase(item.getType())) {
                permissionRepository.findByCode(item.getPermissionCode())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Permission not found: " + item.getPermissionCode()));
            }
        }

        // Delete existing overrides
        userPermissionRepository.deleteByUser_Id(userId);
        userPermissionRepository.flush(); // force DELETE to DB before INSERTs to avoid unique constraint violation

        // Create new overrides (skip INHERIT — that means "no override")
        for (PermissionOverrideItem item : request.getOverrides()) {
            if ("INHERIT".equalsIgnoreCase(item.getType())) {
                continue;
            }

            Permission permission = permissionRepository.findByCode(item.getPermissionCode())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Permission not found: " + item.getPermissionCode()));

            UserPermission up = new UserPermission();
            up.setUser(user);
            up.setPermission(permission);
            up.setType("DENY".equalsIgnoreCase(item.getType())
                    ? PermissionOverrideType.DENY
                    : PermissionOverrideType.ALLOW);
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        validateOverrideType(type);

        Permission permission = permissionRepository.findByCode(permissionCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Permission not found: " + permissionCode));

        // Remove existing override for this permission (if any)
        userPermissionRepository.findByUser_IdAndPermission_Code(userId, permissionCode)
                .ifPresent(userPermissionRepository::delete);

        if ("INHERIT".equalsIgnoreCase(type)) {
            // Remove override → fall back to role default
            permissionResolver.evictCache(userId);
            return buildEffectivePermission(permission, user, null);
        }

        // Create new override
        UserPermission up = new UserPermission();
        up.setUser(user);
        up.setPermission(permission);
        up.setType("DENY".equalsIgnoreCase(type)
                ? PermissionOverrideType.DENY
                : PermissionOverrideType.ALLOW);
        userPermissionRepository.save(up);

        permissionResolver.evictCache(userId);
        return buildEffectivePermission(permission, user, up.getType());
    }

    // ═══════════════════════════════════════════════════════════════
    // REMOVE ALL OVERRIDES (restore role defaults)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void removeAllOverrides(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        userPermissionRepository.deleteByUser_Id(userId);
        permissionResolver.evictCache(userId);
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void validateOverrideType(String type) {
        if (type == null || type.isBlank()) {
            throw new BadRequestException("Override type must not be empty");
        }
        String upper = type.trim().toUpperCase();
        if (!upper.equals("ALLOW") && !upper.equals("DENY") && !upper.equals("INHERIT")) {
            throw new BadRequestException(
                    "Invalid override type: " + type + ". Must be ALLOW, DENY, or INHERIT.");
        }
    }

    private EffectivePermission buildEffectivePermission(Permission permission, User user,
                                                          PermissionOverrideType overrideType) {
        Set<String> roleDefaults = rolePermissionRepository.findAllByRole(user.getRole())
                .stream()
                .map(RolePermission::getPermission)
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        String source;
        if (overrideType == PermissionOverrideType.ALLOW) {
            source = "ALLOW";
        } else if (overrideType == PermissionOverrideType.DENY) {
            source = "DENY";
        } else if (roleDefaults.contains(permission.getCode())) {
            source = "ROLE_DEFAULT";
        } else {
            source = "ROLE_DEFAULT"; // fallback — should not normally reach here
        }

        return EffectivePermission.builder()
                .code(permission.getCode())
                .description(permission.getDescription())
                .source(source)
                .build();
    }
}
