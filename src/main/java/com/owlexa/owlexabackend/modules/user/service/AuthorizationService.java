package com.owlexa.owlexabackend.modules.user.service;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationService {

    private final UserRepository userRepository;
    private final PermissionResolver permissionResolver;
    private final CenterRepository centerRepository;


    public boolean hasRole(Role role) {
        User currentUser = getCurrentUser();
        return currentUser.getRole() == role;
    }

    /**
     * Checks whether the current user has the given permission,
     * considering BOTH role defaults (role_permission table) AND
     * user-level overrides (user_permission table).
     */
    public boolean hasPermission(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return false;
        }

        User currentUser = getCurrentUser();
        String normalizedCode = permissionCode.trim().toUpperCase(Locale.ROOT);

        Set<String> effectivePermissions = permissionResolver.resolvePermissions(
                currentUser.getId(), currentUser.getRole());

        return effectivePermissions.contains(normalizedCode);
    }

    public boolean isOwnerOfCenter(Long centerId) {
        if (centerId == null) {
            return false;
        }

        User currentUser = getCurrentUser();

        return centerRepository.findById(centerId)
                .map(center -> isSameUser(center, currentUser))
                .orElse(false);
    }

    // Helper
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        String phoneNumber = authentication.getName();

        if (phoneNumber == null || phoneNumber.isBlank() || "anonymousUser".equals(phoneNumber)) {
            throw new AccessDeniedException("User is not authenticated");
        }

        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private boolean isSameUser(Center center, User currentUser) {
        return center.getOwner() != null
                && center.getOwner().getId() != null
                && center.getOwner().getId().equals(currentUser.getId());
    }
}
