package com.owlexa.owlexabackend.modules.attendance.controller;

import com.owlexa.owlexabackend.modules.attendance.dto.response.AttendanceResponse;
import com.owlexa.owlexabackend.modules.attendance.dto.response.AttendanceStatsResponse;
import com.owlexa.owlexabackend.modules.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/owner/attendance")
@RequiredArgsConstructor
public class OwnerAttendanceViewController {

    private final AttendanceService attendanceService;

    /** View attendance for a specific schedule + date (read-only for owner) */
    @GetMapping("/schedules/{scheduleId}")
    public List<AttendanceResponse> findBySchedule(
            @PathVariable Long scheduleId,
            @RequestParam LocalDate date
    ) {
        return attendanceService.findAllBySchedule(scheduleId, date);
    }

    /** View attendance for a class on a specific date */
    @GetMapping("/classes/{classId}")
    public List<AttendanceResponse> findByClassAndDate(
            @PathVariable Long classId,
            @RequestParam LocalDate date
    ) {
        return attendanceService.findAllByClassAndDate(classId, date);
    }

    /** View attendance for a class in a date range */
    @GetMapping("/classes/{classId}/range")
    public List<AttendanceResponse> findByClassAndDateRange(
            @PathVariable Long classId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return attendanceService.findAllByClassAndDateRange(classId, startDate, endDate);
    }

    /** Get attendance statistics for a class in a date range */
    @GetMapping("/classes/{classId}/stats")
    public AttendanceStatsResponse getStats(
            @PathVariable Long classId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        return attendanceService.getStats(classId, startDate, endDate);
    }
}
