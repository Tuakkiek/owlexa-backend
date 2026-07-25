package com.owlexa.owlexabackend.modules.homework.controller;

import com.owlexa.owlexabackend.modules.homework.dto.response.student.StudentResultDetailResponse;
import com.owlexa.owlexabackend.modules.homework.dto.response.student.StudentResultSummaryResponse;
import com.owlexa.owlexabackend.modules.homework.service.StudentResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/student/homeworks")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentResultController {

    private final StudentResultService studentResultService;

    @GetMapping("/{homeworkId}/attempts/results")
    public List<StudentResultSummaryResponse> getAttemptResults(
            @PathVariable Long homeworkId,
            @AuthenticationPrincipal(expression = "id") Long studentId) {
        return studentResultService.getAttemptResults(homeworkId, studentId);
    }

    @GetMapping("/submissions/{submissionId}/result")
    public StudentResultDetailResponse getResultDetails(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal(expression = "id") Long studentId) {
        return studentResultService.getResultDetails(submissionId, studentId);
    }
}
