package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.entity.Center;
import com.owlexa.owlexabackend.entity.Role;
import com.owlexa.owlexabackend.entity.User;
import com.owlexa.owlexabackend.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.repository.CenterRepository;
import com.owlexa.owlexabackend.repository.UserPermissionRepository;
import com.owlexa.owlexabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.Authenticator;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationService {

    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final CenterRepository centerRepository;


    public boolean hasRole(Role role) {
        User currentUser = getCurrentUser();
        return currentUser.getRole() == role;
    }

    public boolean hasPermission(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return false;
        }

        User currentUser = getCurrentUser();
        String normalizedCode = permissionCode.trim().toUpperCase(Locale.ROOT);

        return userPermissionRepository.existsByUserIdAndPermissionCode(
                currentUser.getId(),
                normalizedCode
        );
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
