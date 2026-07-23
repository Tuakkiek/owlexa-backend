package com.owlexa.owlexabackend.modules.analytics.controller;

import com.owlexa.owlexabackend.modules.analytics.dto.response.AiDriftResponse;
import com.owlexa.owlexabackend.modules.analytics.dto.response.AnalyticsClassPerformanceResponse;
import com.owlexa.owlexabackend.modules.analytics.service.TeacherAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teacher/analytics")
@RequiredArgsConstructor
public class TeacherAnalyticsController {

    private final TeacherAnalyticsService teacherAnalyticsService;

    @GetMapping("/classes/{classId}/performance")
    public List<AnalyticsClassPerformanceResponse> getClassPerformanceList(
            @PathVariable Long classId,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        return teacherAnalyticsService.getClassPerformanceList(classId, teacherId);
    }

    /**
     * Returns AI vs Teacher scoring drift per rubric criterion for the given class.
     * GET /teacher/analytics/classes/{classId}/ai-drift
     */
    @GetMapping("/classes/{classId}/ai-drift")
    public List<AiDriftResponse> getAiDrift(
            @PathVariable Long classId,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        return teacherAnalyticsService.getAiDrift(classId, teacherId);
    }
}
