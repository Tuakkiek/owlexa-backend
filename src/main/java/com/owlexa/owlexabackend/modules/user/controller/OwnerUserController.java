package com.owlexa.owlexabackend.modules.user.controller;

import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.user.dto.request.BulkPermissionOverrideRequest;
import com.owlexa.owlexabackend.modules.user.dto.request.SinglePermissionOverrideRequest;
import com.owlexa.owlexabackend.modules.user.dto.response.EffectivePermission;
import com.owlexa.owlexabackend.modules.user.dto.response.PermissionResponse;
import com.owlexa.owlexabackend.modules.user.dto.response.UserPermissionsResponse;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.user.service.UserPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner")
@RequiredArgsConstructor
public class OwnerUserController {

    private final UserPermissionService userPermissionService;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    // ═══════════════════════════════════════════════════════════════
    // PERMISSION CATALOG
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/permissions")
    public List<PermissionResponse> listAllPermissions() {
        assertOwner();
        return userPermissionService.listAllPermissions();
    }

    // ═══════════════════════════════════════════════════════════════
    // USER PERMISSIONS
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/users/{userId}/permissions")
    public UserPermissionsResponse getEffectivePermissions(@PathVariable Long userId) {
        assertOwner();
        assertSameCenter(userId);
        return userPermissionService.getEffectivePermissions(userId);
    }

    @PutMapping("/users/{userId}/permissions")
    public UserPermissionsResponse bulkUpdateOverrides(
            @PathVariable Long userId,
            @RequestBody BulkPermissionOverrideRequest request) {
        assertOwner();
        assertSameCenter(userId);
        return userPermissionService.applyOverrides(userId, request);
    }

    @PatchMapping("/users/{userId}/permissions/{permissionCode}")
    public EffectivePermission updateSingleOverride(
            @PathVariable Long userId,
            @PathVariable String permissionCode,
            @RequestBody SinglePermissionOverrideRequest request) {
        assertOwner();
        assertSameCenter(userId);
        return userPermissionService.updateSingleOverride(userId, permissionCode, request.getType());
    }

    @DeleteMapping("/users/{userId}/permissions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAllOverrides(@PathVariable Long userId) {
        assertOwner();
        assertSameCenter(userId);
        userPermissionService.removeAllOverrides(userId);
    }

    // ═══════════════════════════════════════════════════════════════
    // AUTHORIZATION HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void assertOwner() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Chỉ có Chủ trung tâm mới có quyền quản lý phân quyền người dùng");
        }
    }

    /**
     * Verifies that the target user shares at least one center membership with the current user.
     */
    private void assertSameCenter(Long targetUserId) {
        User currentUser = getCurrentUser();
        if (!userRepository.existsById(targetUserId)) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + targetUserId);
        }

        boolean sharesCenter = membershipRepository.findAllByUser_Id(currentUser.getId())
                .stream()
                .anyMatch(membership -> membershipRepository
                        .existsByUser_IdAndCenter_Id(targetUserId, membership.getCenter().getId()));

        if (!sharesCenter) {
            throw new AccessDeniedException("Bạn chỉ có thể quản lý phân quyền cho người dùng thuộc trung tâm của bạn");
        }
    }

    private User getCurrentUser() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng hiện tại"));
    }
}
