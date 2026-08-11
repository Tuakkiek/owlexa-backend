package com.owlexa.owlexabackend.modules.user.controller;

import com.owlexa.owlexabackend.dto.PageResponse;
import com.owlexa.owlexabackend.dto.admin.AdminAuditLogResponse;
import com.owlexa.owlexabackend.dto.admin.AdminCenterResponse;
import com.owlexa.owlexabackend.dto.admin.AdminStatusRequest;
import com.owlexa.owlexabackend.dto.admin.AdminUserResponse;
import com.owlexa.owlexabackend.entity.AdminAuditAction;
import com.owlexa.owlexabackend.entity.AdminAuditTargetType;
import com.owlexa.owlexabackend.modules.user.dto.response.AdminStatsResponse;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.service.AdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Validated
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public AdminStatsResponse getStats() {
        return adminService.getStats();
    }

    @GetMapping("/users")
    public PageResponse<AdminUserResponse> getUsers(
            @RequestParam(defaultValue = "") @Size(max = 100) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return adminService.getUsers(search, role, page, size);
    }

    @GetMapping("/users/{userId}")
    public AdminUserResponse getUser(@PathVariable Long userId) {
        return adminService.getUser(userId);
    }

    @PatchMapping("/users/{userId}/status")
    public AdminUserResponse updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminStatusRequest request,
            Authentication authentication
    ) {
        return adminService.updateUserStatus(
                userId, request.active(), request.reason(), authentication.getName());
    }

    @GetMapping("/centers")
    public PageResponse<AdminCenterResponse> getCenters(
            @RequestParam(defaultValue = "") @Size(max = 100) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return adminService.getCenters(search, page, size);
    }

    @GetMapping("/centers/{centerId}")
    public AdminCenterResponse getCenter(@PathVariable Long centerId) {
        return adminService.getCenter(centerId);
    }

    @PatchMapping("/centers/{centerId}/status")
    public AdminCenterResponse updateCenterStatus(
            @PathVariable Long centerId,
            @Valid @RequestBody AdminStatusRequest request,
            Authentication authentication
    ) {
        return adminService.updateCenterStatus(
                centerId, request.active(), request.reason(), authentication.getName());
    }

    @GetMapping("/audit-logs")
    public PageResponse<AdminAuditLogResponse> getAuditLogs(
            @RequestParam(defaultValue = "") @Size(max = 100) String search,
            @RequestParam(required = false) AdminAuditTargetType targetType,
            @RequestParam(required = false) AdminAuditAction action,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return adminService.getAuditLogs(search, targetType, action, page, size);
    }
}
