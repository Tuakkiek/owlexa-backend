package com.owlexa.owlexabackend.modules.student_submission.controller;

import com.owlexa.owlexabackend.modules.student_submission.dto.response.TeacherAttemptDetailResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.TeacherSubmissionSummaryResponse;
import com.owlexa.owlexabackend.modules.student_submission.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('TEACHER_ASSIGNMENTS')")
public class TeacherSubmissionController {

    private final SubmissionService submissionService;

    @GetMapping("/assignments/{assignmentId}/submissions")
    public Page<TeacherSubmissionSummaryResponse> findAssignmentSubmissions(
            @PathVariable Long assignmentId,
            @PageableDefault(size = 20, sort = "assignedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return submissionService.findAssignmentSubmissions(assignmentId, pageable);
    }

    @GetMapping("/submission-attempts/{attemptId}")
    public TeacherAttemptDetailResponse findAttemptDetail(@PathVariable Long attemptId) {
        return submissionService.findAttemptDetailForTeacher(attemptId);
    }
}
