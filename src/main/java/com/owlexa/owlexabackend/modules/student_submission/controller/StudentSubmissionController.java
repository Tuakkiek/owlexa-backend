package com.owlexa.owlexabackend.modules.student_submission.controller;

import com.owlexa.owlexabackend.modules.student_submission.dto.request.SaveSubmissionAnswersRequest;
import com.owlexa.owlexabackend.modules.student_submission.dto.request.StartAttemptRequest;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.StudentAttemptDetailResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.StudentAttemptSummaryResponse;
import com.owlexa.owlexabackend.modules.student_submission.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentSubmissionController {

    private final SubmissionService submissionService;

    @PostMapping("/assignments/{assignmentId}/attempts/start")
    public StudentAttemptDetailResponse startOrResumeAttempt(
            @PathVariable Long assignmentId,
            @RequestBody(required = false) StartAttemptRequest request
    ) {
        return submissionService.startOrResumeAttempt(assignmentId, request);
    }

    @GetMapping("/assignments/{assignmentId}/attempts")
    public List<StudentAttemptSummaryResponse> getAttemptHistory(@PathVariable Long assignmentId) {
        return submissionService.getAttemptHistory(assignmentId);
    }

    @GetMapping("/submission-attempts/{attemptId}")
    public StudentAttemptDetailResponse getAttemptDetail(@PathVariable Long attemptId) {
        return submissionService.getAttemptDetail(attemptId);
    }

    @PutMapping("/submission-attempts/{attemptId}/answers")
    public StudentAttemptDetailResponse saveAnswers(
            @PathVariable Long attemptId,
            @Valid @RequestBody SaveSubmissionAnswersRequest request
    ) {
        return submissionService.saveAnswers(attemptId, request);
    }

    @PostMapping("/submission-attempts/{attemptId}/submit")
    public StudentAttemptDetailResponse submitAttempt(@PathVariable Long attemptId) {
        return submissionService.submitAttemptWithAutoGrading(attemptId);
    }
}
