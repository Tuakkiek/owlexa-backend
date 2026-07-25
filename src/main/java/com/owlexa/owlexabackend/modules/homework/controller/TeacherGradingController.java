package com.owlexa.owlexabackend.modules.homework.controller;

import com.owlexa.owlexabackend.modules.ai_scoring.service.AiScoringService;
import com.owlexa.owlexabackend.modules.homework.dto.request.teacher.AiRescoreRequest;
import com.owlexa.owlexabackend.modules.homework.dto.request.teacher.TeacherGradeSubmissionRequest;
import com.owlexa.owlexabackend.modules.homework.dto.response.teacher.AiScoringStatusResponse;
import com.owlexa.owlexabackend.modules.homework.dto.response.teacher.TeacherSubmissionDetailResponse;
import com.owlexa.owlexabackend.modules.homework.dto.response.teacher.TeacherSubmissionListResponse;
import com.owlexa.owlexabackend.modules.homework.dto.response.teacher.TeacherSubmissionStatsResponse;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkSubmissionStatus;
import com.owlexa.owlexabackend.modules.homework.service.TeacherGradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teacher")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class TeacherGradingController {

    private final TeacherGradingService teacherGradingService;
    private final AiScoringService aiScoringService;

    @GetMapping("/homeworks/{id}/submissions")
    public Page<TeacherSubmissionListResponse> getSubmissions(
            @PathVariable Long id,
            @RequestParam(required = false) HomeworkSubmissionStatus status,
            Pageable pageable,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        return teacherGradingService.getSubmissions(id, teacherId, status, pageable);
    }

    @GetMapping("/homeworks/{id}/grading-stats")
    public TeacherSubmissionStatsResponse getSubmissionStats(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        return teacherGradingService.getSubmissionStats(id, teacherId);
    }

    @GetMapping("/submissions/{id}")
    public TeacherSubmissionDetailResponse getSubmissionDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        return teacherGradingService.getSubmissionDetails(id, teacherId);
    }

    @PutMapping("/submissions/{id}/grade/draft")
    public TeacherSubmissionDetailResponse saveGradeDraft(
            @PathVariable Long id,
            @RequestBody TeacherGradeSubmissionRequest request,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        return teacherGradingService.gradeSubmission(id, teacherId, request, true);
    }

    @PostMapping("/submissions/{id}/grade/finalize")
    public TeacherSubmissionDetailResponse finalizeGrade(
            @PathVariable Long id,
            @RequestBody TeacherGradeSubmissionRequest request,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        return teacherGradingService.gradeSubmission(id, teacherId, request, false);
    }

    @PostMapping("/submissions/{id}/return")
    public void returnSubmission(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        teacherGradingService.returnSubmission(id, teacherId);
    }

    // ── AI Scoring Endpoints ──────────────────────────────────────────────────

    /**
     * Returns the AI scoring status for each question submission.
     * GET /teacher/homeworks/{homeworkId}/submissions/{submissionId}/ai-status
     */
    @GetMapping("/homeworks/{homeworkId}/submissions/{submissionId}/ai-status")
    public java.util.List<AiScoringStatusResponse> getAiScoringStatus(
            @PathVariable Long homeworkId,
            @PathVariable Long submissionId,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        return teacherGradingService.getAiScoringStatus(submissionId, teacherId);
    }

    /**
     * Triggers AI re-scoring for the specified question submissions.
     * POST /teacher/homeworks/{homeworkId}/submissions/{submissionId}/ai-rescore
     * Returns 202 Accepted immediately; scoring happens asynchronously.
     */
    @PostMapping("/homeworks/{homeworkId}/submissions/{submissionId}/ai-rescore")
    public ResponseEntity<Void> triggerAiRescore(
            @PathVariable Long homeworkId,
            @PathVariable Long submissionId,
            @RequestBody AiRescoreRequest request,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        // Validate teacher owns the submission (done inside getAiScoringStatus as a side-effect check,
        // but here we trigger re-scoring per question submission ID)
        teacherGradingService.getAiScoringStatus(submissionId, teacherId); // authorization check

        if (request.getQuestionSubmissionIds() != null) {
            request.getQuestionSubmissionIds().forEach(aiScoringService::rescoreEssaySubmissionAsync);
        }
        return ResponseEntity.accepted().build();
    }
}
