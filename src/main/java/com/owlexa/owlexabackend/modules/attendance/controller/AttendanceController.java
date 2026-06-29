package com.owlexa.owlexabackend.modules.attendance.controller;
import com.owlexa.owlexabackend.modules.attendance.dto.request.AttendanceMarkRequest;
import com.owlexa.owlexabackend.modules.attendance.dto.response.AttendanceResponse;
import com.owlexa.owlexabackend.modules.attendance.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/schedules/{scheduleId}")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AttendanceResponse> mark(
            @PathVariable Long scheduleId,
            @Valid @RequestBody AttendanceMarkRequest request
    ) {
        return attendanceService.mark(scheduleId, request);
    }

    @GetMapping("/schedules/{scheduleId}")
    public List<AttendanceResponse> findAllBySchedule(
            @PathVariable Long scheduleId,
            @RequestParam LocalDate sessionDate
    ) {
        return attendanceService.findAllBySchedule(scheduleId, sessionDate);
    }
}