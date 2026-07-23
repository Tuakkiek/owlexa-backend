package com.owlexa.owlexabackend.modules.homework.controller;

import com.owlexa.owlexabackend.modules.homework.dto.request.TeacherHomeworkSaveRequest;
import com.owlexa.owlexabackend.modules.homework.service.TeacherHomeworkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teacher/homeworks")
@RequiredArgsConstructor
public class TeacherHomeworkController {

    private final TeacherHomeworkService teacherHomeworkService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createDraft(
            @AuthenticationPrincipal(expression = "id") Long teacherId,
            @Valid @RequestBody TeacherHomeworkSaveRequest request) {
        teacherHomeworkService.saveHomeworkTree(teacherId, null, request);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateDraft(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long teacherId,
            @Valid @RequestBody TeacherHomeworkSaveRequest request) {
        teacherHomeworkService.saveHomeworkTree(teacherId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDraft(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        teacherHomeworkService.deleteHomework(teacherId, id);
    }

    @PostMapping("/{id}/publish")
    public void publishHomework(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        teacherHomeworkService.publishHomework(teacherId, id);
    }

    @PostMapping("/{id}/release-grades")
    public void releaseGrades(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        teacherHomeworkService.releaseGrades(id, teacherId);
    }

    @PostMapping("/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        teacherHomeworkService.closeHomework(teacherId, id);
    }
}
