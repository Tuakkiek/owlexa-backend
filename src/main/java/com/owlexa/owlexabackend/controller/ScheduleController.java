package com.owlexa.owlexabackend.controller;

import com.owlexa.owlexabackend.dto.request.ScheduleRequest;
import com.owlexa.owlexabackend.dto.response.ScheduleResponse;
import com.owlexa.owlexabackend.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/classes/{classId}/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleResponse create (
            @PathVariable Long classId,
            @Valid @RequestBody ScheduleRequest request
            ) {
        return scheduleService.create(classId, request);
    }

    @GetMapping
    public List<ScheduleResponse> findAllByClass(@PathVariable Long classId) {
        return scheduleService.findAllByClass(classId);
    }

    @GetMapping("/teacher/{teacherUserId}")
    public List<ScheduleResponse> findAllByTeacher(@PathVariable Long teacherUserId) {
        return scheduleService.findAllByTeacher(teacherUserId);
    }

    @PutMapping("/{scheduleId}")
    public ScheduleResponse update(
            @PathVariable Long classId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleRequest request
    ) {
        return scheduleService.update(scheduleId, request);
    }

    @DeleteMapping("/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long classId,
            @PathVariable Long scheduleId
    ) {
        scheduleService.delete(scheduleId);
    }
}
