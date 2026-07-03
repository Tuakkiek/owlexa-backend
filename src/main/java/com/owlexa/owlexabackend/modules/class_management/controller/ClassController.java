package com.owlexa.owlexabackend.modules.class_management.controller;
import com.owlexa.owlexabackend.modules.class_management.dto.request.ClassRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.response.ClassResponse;
import com.owlexa.owlexabackend.modules.class_management.service.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.owlexa.owlexabackend.modules.teacher.dto.response.TeacherClassStudentsResponse;
@RestController
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    // ── OWNER: View all classes with students ────────────────────────────────

    @GetMapping("/owner/classes/with-students")
    public List<com.owlexa.owlexabackend.modules.teacher.dto.response.TeacherClassStudentsResponse> findAllClassesWithStudentsForOwner() {
        return classService.findAllClassesWithStudentsForOwner();
    }

    // ── OWNER: Manage classes ────────────────────────────────────────────────

    @PostMapping("/owner/classes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassResponse create(@Valid @RequestBody ClassRequest request) {
        return classService.create(request);
    }

    @GetMapping("/owner/classes")
    public List<ClassResponse> findAll() {
        return classService.findAll();
    }

    @PutMapping("/owner/classes/{classId}")
    public ClassResponse update(@PathVariable Long classId, @RequestBody ClassRequest request) {
        return classService.update(classId, request);
    }

    @DeleteMapping("/owner/classes/{classId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long classId) {
        classService.delete(classId);
    }

    // ── TEACHER: View own classes ────────────────────────────────────────────

    @GetMapping("/teacher/classes/me")
    public List<ClassResponse> findMyClassesAsTeacher() {
        return classService.findMyClassesAsTeacher();
    }

    @GetMapping("/teacher/classes/with-students")
    public List<com.owlexa.owlexabackend.modules.teacher.dto.response.TeacherClassStudentsResponse> findMyClassesWithStudentsAsTeacher() {
        return classService.findMyClassesWithStudentsAsTeacher();
    }
}

