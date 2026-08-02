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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    // ── OWNER: View all schedules in center ───────────────────────────────────

    @GetMapping("/owner/schedules/me")
    public List<ScheduleResponse> findAllForOwner() {
        return scheduleService.findAllForOwner();
    }

    // ── OWNER: Manage schedules ──────────────────────────────────────────────

    @GetMapping("/owner/classes/{classId}/schedules")
    public List<ScheduleResponse> findAllByClass(@PathVariable Long classId) {
        return scheduleService.findAllByClass(classId);
    }

    @PostMapping("/owner/classes/{classId}/schedule-rules")
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleRuleResponse createRule(
            @PathVariable Long classId,
            @Valid @RequestBody ScheduleRuleRequest request
    ) {
        return scheduleService.createRule(classId, request);
    }

    @GetMapping("/owner/classes/{classId}/schedule-rules")
    public List<ScheduleRuleResponse> findRulesByClass(@PathVariable Long classId) {
        return scheduleService.findRulesByClass(classId);
    }

    @PostMapping("/owner/classes/{classId}/schedule-rules/{ruleId}/generate")
    public List<ScheduleEventResponse> generateEvents(
            @PathVariable Long classId,
            @PathVariable Long ruleId
    ) {
        return scheduleService.generateEvents(classId, ruleId);
    }

    @PostMapping("/owner/classes/{classId}/schedule-events")
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleEventResponse createEvent(
            @PathVariable Long classId,
            @Valid @RequestBody ScheduleEventRequest request
    ) {
        return scheduleService.createEvent(classId, request);
    }

    @GetMapping("/owner/classes/{classId}/schedule-events")
    public List<ScheduleEventResponse> findEventsByClass(@PathVariable Long classId) {
        return scheduleService.findEventsByClass(classId);
    }

    @PutMapping("/owner/classes/{classId}/schedule-events/{eventId}")
    public ScheduleEventResponse updateEvent(
            @PathVariable Long classId,
            @PathVariable Long eventId,
            @Valid @RequestBody ScheduleEventRequest request
    ) {
        return scheduleService.updateEvent(classId, eventId, request);
    }

    @PatchMapping("/owner/classes/{classId}/schedule-events/{eventId}/cancel")
    public ScheduleEventResponse cancelEvent(
            @PathVariable Long classId,
            @PathVariable Long eventId
    ) {
        return scheduleService.cancelEvent(classId, eventId);
    }

    @GetMapping("/owner/classes/{classId}/schedules/teacher/{teacherUserId}")
    public List<ScheduleResponse> findAllByTeacher(@PathVariable Long teacherUserId) {
        return scheduleService.findAllByTeacher(teacherUserId);
    }

    // ── TEACHER: View own schedule ───────────────────────────────────────────

    @GetMapping("/teacher/schedules/me")
    public List<ScheduleResponse> findMySchedules() {
        return scheduleService.findMySchedules();
    }

    // ── STUDENT: View own schedule ───────────────────────────────────────────

    @GetMapping("/student/schedules/me")
    public List<ScheduleResponse> findMySchedulesAsStudent() {
        return scheduleService.findMySchedulesAsStudent();
    }
}

