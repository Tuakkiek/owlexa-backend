package com.owlexa.owlexabackend.modules.dashboard.controller;
import com.owlexa.owlexabackend.modules.dashboard.dto.response.DashboardStatsResponse;
import com.owlexa.owlexabackend.modules.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /owner/dashboard/stats
     * Returns aggregated stats for the current center:
     * total students, teachers, classes, fee records (paid/unpaid), and total revenue.
     */
    @GetMapping("/owner/dashboard/stats")
    @PreAuthorize("hasAuthority('DASHBOARD_OWNER')")
    public DashboardStatsResponse getOwnerStats() {
        return dashboardService.getOwnerStats();
    }
}
