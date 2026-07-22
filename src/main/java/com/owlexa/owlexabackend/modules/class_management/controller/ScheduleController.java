package com.owlexa.owlexabackend.modules.class_management.controller;
import com.owlexa.owlexabackend.modules.class_management.dto.request.ScheduleRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.response.ScheduleResponse;
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

    @PostMapping("/owner/classes/{classId}/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleResponse create(
            @PathVariable Long classId,
            @Valid @RequestBody ScheduleRequest request
    ) {
        return scheduleService.create(classId, request);
    }

    @GetMapping("/owner/classes/{classId}/schedules")
    public List<ScheduleResponse> findAllByClass(@PathVariable Long classId) {
        return scheduleService.findAllByClass(classId);
    }

    @GetMapping("/owner/classes/{classId}/schedules/teacher/{teacherUserId}")
    public List<ScheduleResponse> findAllByTeacher(@PathVariable Long teacherUserId) {
        return scheduleService.findAllByTeacher(teacherUserId);
    }

    @PutMapping("/owner/classes/{classId}/schedules/{scheduleId}")
    public ScheduleResponse update(
            @PathVariable Long classId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleRequest request
    ) {
        return scheduleService.update(classId, scheduleId, request);
    }

    @DeleteMapping("/owner/classes/{classId}/schedules/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long classId,
            @PathVariable Long scheduleId
    ) {
        scheduleService.delete(scheduleId);
    }

    @PatchMapping("/owner/classes/{classId}/schedules/{scheduleId}/type")
    public ScheduleResponse updateType(
            @PathVariable Long classId,
            @PathVariable Long scheduleId,
            @RequestBody com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType type
    ) {
        return scheduleService.updateType(scheduleId, type);
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

