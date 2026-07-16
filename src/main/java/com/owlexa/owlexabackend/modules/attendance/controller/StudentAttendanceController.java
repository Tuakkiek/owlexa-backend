package com.owlexa.owlexabackend.modules.attendance.controller;

import com.owlexa.owlexabackend.modules.attendance.dto.response.AttendanceResponse;
import com.owlexa.owlexabackend.modules.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/student/attendance")
@RequiredArgsConstructor
public class StudentAttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    public List<AttendanceResponse> findMyAttendances(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) LocalDate date
    ) {
        return attendanceService.findMyAttendancesAsStudent(classId, date);
    }
}
