package com.owlexa.owlexabackend.modules.class_management.controller;
import com.owlexa.owlexabackend.modules.class_management.dto.request.ClassRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.response.ClassResponse;
import com.owlexa.owlexabackend.modules.class_management.service.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.owlexa.owlexabackend.modules.teacher.dto.response.TeacherClassStudentsResponse;
@RestController
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    // ── OWNER: View all classes with students ────────────────────────────────

    @GetMapping("/owner/classes/with-students")
    @PreAuthorize("hasAnyAuthority('CLASS_VIEW', 'ATTENDANCE_VIEW')")
    public List<com.owlexa.owlexabackend.modules.teacher.dto.response.TeacherClassStudentsResponse> findAllClassesWithStudentsForOwner() {
        return classService.findAllClassesWithStudentsForOwner();
    }

    // ── OWNER: Manage classes ────────────────────────────────────────────────

    @PostMapping("/owner/classes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CLASS_CREATE')")
    public ClassResponse create(@Valid @RequestBody ClassRequest request) {
        return classService.create(request);
    }

    @GetMapping("/owner/classes")
    @PreAuthorize("hasAuthority('CLASS_VIEW')")
    public List<ClassResponse> findAll() {
        return classService.findAll();
    }

    @GetMapping("/owner/classes/{classId}")
    @PreAuthorize("hasAuthority('CLASS_VIEW')")
    public ClassResponse findById(@PathVariable Long classId) {
        return classService.findById(classId);
    }

    @PutMapping("/owner/classes/{classId}")
    @PreAuthorize("hasAuthority('CLASS_CREATE')")
    public ClassResponse update(@PathVariable Long classId, @RequestBody ClassRequest request) {
        return classService.update(classId, request);
    }

    @DeleteMapping("/owner/classes/{classId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('CLASS_ARCHIVE')")
    public void delete(@PathVariable Long classId) {
        classService.delete(classId);
    }

    // ── TEACHER: View own classes ────────────────────────────────────────────

    @GetMapping("/teacher/classes/me")
    @PreAuthorize("hasAnyAuthority('TEACHER_DOCUMENTS', 'TEACHER_ASSIGNMENTS')")
    public List<ClassResponse> findMyClassesAsTeacher() {
        return classService.findMyClassesAsTeacher();
    }

    @GetMapping("/teacher/classes/with-students")
    @PreAuthorize("hasAnyAuthority('TEACHER_ATTENDANCE', 'TEACHER_ASSIGNMENTS')")
    public List<com.owlexa.owlexabackend.modules.teacher.dto.response.TeacherClassStudentsResponse> findMyClassesWithStudentsAsTeacher() {
        return classService.findMyClassesWithStudentsAsTeacher();
    }

    // ── Lifecycle: Update Status (any status → any status) ────────────────────

    @PatchMapping("/owner/classes/{classId}/status")
    @PreAuthorize("hasAnyAuthority('CLASS_OPEN', 'CLASS_START', 'CLASS_FINISH', 'CLASS_ARCHIVE')")
    public ClassResponse updateStatus(
            @PathVariable Long classId,
            @RequestBody com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus newStatus
    ) {
        return classService.updateStatus(classId, newStatus);
    }
}

