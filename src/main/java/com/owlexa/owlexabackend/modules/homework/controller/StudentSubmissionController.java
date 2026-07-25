package com.owlexa.owlexabackend.modules.homework.controller;

import com.owlexa.owlexabackend.modules.homework.dto.request.student.AutosaveSubmissionRequest;
import com.owlexa.owlexabackend.modules.homework.dto.response.student.StudentHomeworkSubmissionResponse;
import com.owlexa.owlexabackend.modules.homework.service.StudentSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/student/homeworks")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentSubmissionController {

    private final StudentSubmissionService studentSubmissionService;

    @PostMapping("/{homeworkId}/attempts")
    public StudentHomeworkSubmissionResponse getOrCreateAttempt(
            @PathVariable Long homeworkId,
            @AuthenticationPrincipal(expression = "id") Long studentId) {
        return studentSubmissionService.getOrCreateAttempt(homeworkId, studentId);
    }

    @GetMapping("/{homeworkId}/attempts/active")
    public StudentHomeworkSubmissionResponse getActiveAttempt(
            @PathVariable Long homeworkId,
            @AuthenticationPrincipal(expression = "id") Long studentId) {
        return studentSubmissionService.getActiveAttempt(homeworkId, studentId);
    }

    @PutMapping("/submissions/{submissionId}")
    public StudentHomeworkSubmissionResponse autosave(
            @PathVariable Long submissionId,
            @RequestBody AutosaveSubmissionRequest request,
            @AuthenticationPrincipal(expression = "id") Long studentId) {
        return studentSubmissionService.autosave(submissionId, request, studentId);
    }

    @PostMapping("/submissions/{submissionId}/submit")
    public StudentHomeworkSubmissionResponse submit(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal(expression = "id") Long studentId) {
        return studentSubmissionService.submit(submissionId, studentId);
    }
}
