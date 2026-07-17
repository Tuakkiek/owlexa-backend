package com.owlexa.owlexabackend.modules.user.service;

import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.RolePermission;
import com.owlexa.owlexabackend.modules.user.entity.UserPermission;
import com.owlexa.owlexabackend.modules.user.repository.RolePermissionRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PermissionResolver {

    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionRepository userPermissionRepository;

    /**
     * Evict the cached permissions for a user.
     * Must be called after any change to the user's permission overrides
     * so that subsequent requests pick up the new effective permissions.
     */
    @CacheEvict(value = "userPermissions", key = "#userId")
    public void evictCache(Long userId) {
        // The annotation does the work — this method body is intentionally empty.
    }

    /**
     * Resolves the effective permissions for a user.
     *
     * Simplified RBAC model (Phase 1):
     *   1. Start with ALL permissions from the user's role (role_permission table).
     *   2. Remove any permission that has a user_permission record (disabled).
     *
     * User permissions are always a SUBSET of role permissions —
     * they can never be expanded beyond what the role allows.
     */
    @Cacheable(value = "userPermissions", key = "#userId")
    public Set<String> resolvePermissions(Long userId, Role role) {
        Set<String> finalPermissions = new HashSet<>();

        // 1. Load all Role Permissions (the baseline)
        if (role != null) {
            List<RolePermission> rolePerms = rolePermissionRepository.findAllByRole(role);
            for (RolePermission rp : rolePerms) {
                if (rp.getPermission() != null && rp.getPermission().getCode() != null) {
                    finalPermissions.add(rp.getPermission().getCode());
                }
            }
        }

        // 2. Remove any permission that has a user_permission record (disabled)
        List<UserPermission> userPerms = userPermissionRepository.findAllByUser_Id(userId);
        for (UserPermission up : userPerms) {
            if (up.getPermission() != null && up.getPermission().getCode() != null) {
                finalPermissions.remove(up.getPermission().getCode());
            }
        }

        return finalPermissions;
    }
}
