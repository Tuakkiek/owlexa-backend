package com.owlexa.owlexabackend.modules.user.controller;
import com.owlexa.owlexabackend.modules.user.dto.response.AdminStatsResponse;
import com.owlexa.owlexabackend.modules.user.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminStatsResponse getStats() {
        return adminService.getStats();
    }
}
