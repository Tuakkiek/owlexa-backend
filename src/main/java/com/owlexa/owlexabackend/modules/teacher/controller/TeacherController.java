package com.owlexa.owlexabackend.modules.teacher.controller;
import com.owlexa.owlexabackend.modules.teacher.dto.request.BulkTeacherRequest;
import com.owlexa.owlexabackend.modules.teacher.dto.request.TeacherRequest;
import com.owlexa.owlexabackend.modules.teacher.dto.response.BulkTeacherResult;
import com.owlexa.owlexabackend.modules.teacher.dto.response.TeacherResponse;
import com.owlexa.owlexabackend.modules.teacher.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TEACHER_ASSIGN')")
    public TeacherResponse create(@Valid @RequestBody TeacherRequest request) {
        return teacherService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TEACHER_VIEW')")
    public List<TeacherResponse> findAll() {
        return teacherService.findAll();
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TEACHER_ASSIGN')")
    public List<BulkTeacherResult> bulkCreate(@RequestBody BulkTeacherRequest request) {
        return teacherService.bulkCreate(request);
    }

    // Update
    @PutMapping("/{teacherId}")
    @PreAuthorize("hasAuthority('TEACHER_ASSIGN')")
    public TeacherResponse update(
            @PathVariable Long teacherId,
            @Valid @RequestBody TeacherRequest request
    ) {
        return teacherService.update(teacherId, request);
    }

    @DeleteMapping("/{teacherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TEACHER_ASSIGN')")
    public void delete(@PathVariable Long teacherId) {
        teacherService.delete(teacherId);
    }

}
