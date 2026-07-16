package com.owlexa.owlexabackend.modules.teacher_attendance.controller;

import com.owlexa.owlexabackend.modules.teacher_attendance.dto.request.TeacherAttendanceMarkRequest;
import com.owlexa.owlexabackend.modules.teacher_attendance.dto.response.TeacherAttendanceResponse;
import com.owlexa.owlexabackend.modules.teacher_attendance.entity.TeacherAttendanceStatus;
import com.owlexa.owlexabackend.modules.teacher_attendance.service.TeacherAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/owner/teacher-attendance")
@RequiredArgsConstructor
public class TeacherAttendanceController {

    private final TeacherAttendanceService teacherAttendanceService;

    /** Batch mark teacher attendance for a date */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<TeacherAttendanceResponse> mark(
            @Valid @RequestBody TeacherAttendanceMarkRequest request
    ) {
        return teacherAttendanceService.mark(request);
    }

    /** Query teacher attendance with filters */
    @GetMapping
    public List<TeacherAttendanceResponse> findAll(
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        return teacherAttendanceService.findAll(teacherId, date, startDate, endDate);
    }

    /** Get a single teacher attendance record */
    @GetMapping("/{id}")
    public TeacherAttendanceResponse findById(@PathVariable Long id) {
        return teacherAttendanceService.findById(id);
    }

    /** Update a teacher attendance record */
    @PutMapping("/{id}")
    public TeacherAttendanceResponse update(
            @PathVariable Long id,
            @RequestParam TeacherAttendanceStatus status,
            @RequestParam(required = false) String note
    ) {
        return teacherAttendanceService.update(id, status, note);
    }

    /** Delete a teacher attendance record */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        teacherAttendanceService.delete(id);
    }
}
