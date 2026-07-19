package com.owlexa.owlexabackend.modules.payment.controller;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.modules.payment.entity.AuditLog;
import com.owlexa.owlexabackend.modules.payment.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/owner/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('DASHBOARD_OWNER')")
    public Page<AuditLog> findAll(@RequestParam(required = false) String action,
                                   @RequestParam(required = false) Long userId,
                                   @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Long centerId = TenantContext.getCurrentTenantId();
        // For simplicity, return all by center; filtering can be added later
        return auditLogRepository.findAllByCenter_IdOrderByCreatedAtDesc(centerId, pageable);
    }
}
