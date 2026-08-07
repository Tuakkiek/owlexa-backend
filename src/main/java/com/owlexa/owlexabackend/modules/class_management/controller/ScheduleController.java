package com.owlexa.owlexabackend.modules.class_management.controller;
import com.owlexa.owlexabackend.modules.class_management.dto.request.ScheduleEventRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.request.ScheduleRuleRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.response.ScheduleEventResponse;
import com.owlexa.owlexabackend.modules.class_management.dto.response.ScheduleResponse;
import com.owlexa.owlexabackend.modules.class_management.dto.response.ScheduleRuleResponse;
import com.owlexa.owlexabackend.modules.class_management.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    // ── OWNER: View all schedules in center ───────────────────────────────────

    @GetMapping("/owner/schedules/me")
    @PreAuthorize("hasAnyAuthority('SCHEDULE_VIEW', 'ATTENDANCE_VIEW')")
    public List<ScheduleResponse> findAllForOwner() {
        return scheduleService.findAllForOwner();
    }

    // ── OWNER: Manage schedules ──────────────────────────────────────────────

    @GetMapping("/owner/classes/{classId}/schedules")
    @PreAuthorize("hasAuthority('SCHEDULE_VIEW')")
    public List<ScheduleResponse> findAllByClass(@PathVariable Long classId) {
        return scheduleService.findAllByClass(classId);
    }

    @PostMapping("/owner/classes/{classId}/schedule-rules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCHEDULE_GENERATE')")
    public ScheduleRuleResponse createRule(
            @PathVariable Long classId,
            @Valid @RequestBody ScheduleRuleRequest request
    ) {
        return scheduleService.createRule(classId, request);
    }

    @GetMapping("/owner/classes/{classId}/schedule-rules")
    @PreAuthorize("hasAuthority('SCHEDULE_VIEW')")
    public List<ScheduleRuleResponse> findRulesByClass(@PathVariable Long classId) {
        return scheduleService.findRulesByClass(classId);
    }

    @PostMapping("/owner/classes/{classId}/schedule-rules/{ruleId}/generate")
    @PreAuthorize("hasAuthority('SCHEDULE_GENERATE')")
    public List<ScheduleEventResponse> generateEvents(
            @PathVariable Long classId,
            @PathVariable Long ruleId
    ) {
        return scheduleService.generateEvents(classId, ruleId);
    }

    @PostMapping("/owner/classes/{classId}/schedule-events")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCHEDULE_EDIT_SINGLE')")
    public ScheduleEventResponse createEvent(
            @PathVariable Long classId,
            @Valid @RequestBody ScheduleEventRequest request
    ) {
        return scheduleService.createEvent(classId, request);
    }

    @GetMapping("/owner/classes/{classId}/schedule-events")
    @PreAuthorize("hasAuthority('SCHEDULE_VIEW')")
    public List<ScheduleEventResponse> findEventsByClass(@PathVariable Long classId) {
        return scheduleService.findEventsByClass(classId);
    }

    @PutMapping("/owner/classes/{classId}/schedule-events/{eventId}")
    @PreAuthorize("hasAuthority('SCHEDULE_EDIT_SINGLE')")
    public ScheduleEventResponse updateEvent(
            @PathVariable Long classId,
            @PathVariable Long eventId,
            @Valid @RequestBody ScheduleEventRequest request
    ) {
        return scheduleService.updateEvent(classId, eventId, request);
    }

    @PatchMapping("/owner/classes/{classId}/schedule-events/{eventId}/cancel")
    @PreAuthorize("hasAuthority('SCHEDULE_EDIT_SINGLE')")
    public ScheduleEventResponse cancelEvent(
            @PathVariable Long classId,
            @PathVariable Long eventId
    ) {
        return scheduleService.cancelEvent(classId, eventId);
    }

    @GetMapping("/owner/classes/{classId}/schedules/teacher/{teacherUserId}")
    @PreAuthorize("hasAuthority('SCHEDULE_VIEW')")
    public List<ScheduleResponse> findAllByTeacher(@PathVariable Long teacherUserId) {
        return scheduleService.findAllByTeacher(teacherUserId);
    }

    // ── TEACHER: View own schedule ───────────────────────────────────────────

    @GetMapping("/teacher/schedules/me")
    @PreAuthorize("hasAuthority('SCHEDULE_VIEW')")
    public List<ScheduleResponse> findMySchedules() {
        return scheduleService.findMySchedules();
    }

    // ── STUDENT: View own schedule ───────────────────────────────────────────

    @GetMapping("/student/schedules/me")
    @PreAuthorize("hasAuthority('SCHEDULE_VIEW')")
    public List<ScheduleResponse> findMySchedulesAsStudent() {
        return scheduleService.findMySchedulesAsStudent();
    }
}

