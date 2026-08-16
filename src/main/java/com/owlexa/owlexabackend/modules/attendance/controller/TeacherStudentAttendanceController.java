package com.owlexa.owlexabackend.modules.attendance.controller;

import com.owlexa.owlexabackend.modules.attendance.dto.request.AttendanceMarkRequest;
import com.owlexa.owlexabackend.modules.attendance.dto.response.AttendanceResponse;
import com.owlexa.owlexabackend.modules.attendance.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/teacher/attendance")
@RequiredArgsConstructor
public class TeacherStudentAttendanceController {

    private final AttendanceService attendanceService;

    /** Teacher views class sessions for a specific date */
    @GetMapping("/class-sessions")
    @PreAuthorize("hasAuthority('ATTENDANCE_MARK')")
    public List<com.owlexa.owlexabackend.modules.attendance.dto.response.ClassSessionResponse> findClassSessionsByDate(
            @RequestParam LocalDate date
    ) {
        return attendanceService.findTeacherClassSessionsByDate(date);
    }

    /** Teacher marks attendance for students in their own schedule */
    @PostMapping("/schedules/{scheduleId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ATTENDANCE_MARK')")
    public List<AttendanceResponse> mark(
            @PathVariable Long scheduleId,
            @Valid @RequestBody AttendanceMarkRequest request
    ) {
        return attendanceService.mark(scheduleId, request);
    }

    /** Teacher views attendance for their own schedule */
    @GetMapping("/schedules/{scheduleId}")
    @PreAuthorize("hasAuthority('ATTENDANCE_MARK')")
    public List<AttendanceResponse> findBySchedule(
            @PathVariable Long scheduleId,
            @RequestParam LocalDate date
    ) {
        return attendanceService.findAllBySchedule(scheduleId, date);
    }

    /** Teacher marks attendance for students in their own schedule event */
    @PostMapping("/schedule-events/{scheduleEventId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ATTENDANCE_MARK')")
    public List<AttendanceResponse> markScheduleEvent(
            @PathVariable Long scheduleEventId,
            @Valid @RequestBody AttendanceMarkRequest request
    ) {
        return attendanceService.markScheduleEvent(scheduleEventId, request);
    }

    /** Teacher views attendance for their own schedule event */
    @GetMapping("/schedule-events/{scheduleEventId}")
    @PreAuthorize("hasAuthority('ATTENDANCE_MARK')")
    public List<AttendanceResponse> findByScheduleEvent(
            @PathVariable Long scheduleEventId,
            @RequestParam LocalDate date
    ) {
        return attendanceService.findAllByScheduleEvent(scheduleEventId, date);
    }
}
