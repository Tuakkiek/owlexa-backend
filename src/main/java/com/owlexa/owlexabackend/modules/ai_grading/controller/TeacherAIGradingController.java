package com.owlexa.owlexabackend.modules.ai_grading.controller;

import com.owlexa.owlexabackend.modules.ai_grading.dto.response.AIGradingJobSummaryResponse;
import com.owlexa.owlexabackend.modules.ai_grading.dto.response.AIGradingResultResponse;
import com.owlexa.owlexabackend.modules.ai_grading.service.AIGradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherAIGradingController {

    private final AIGradingService aiGradingService;

    @PostMapping("/submission-attempts/{attemptId}/ai-grading")
    public AIGradingJobSummaryResponse startGrading(@PathVariable Long attemptId) {
        return aiGradingService.startGrading(attemptId);
    }

    @PostMapping("/ai-grading-jobs/{jobId}/retry")
    public AIGradingJobSummaryResponse retryJob(@PathVariable Long jobId) {
        return aiGradingService.retryJob(jobId);
    }

    @GetMapping("/ai-grading-jobs/{jobId}")
    public AIGradingJobSummaryResponse getJob(@PathVariable Long jobId) {
        return aiGradingService.getJob(jobId);
    }

    @GetMapping("/submission-attempts/{attemptId}/ai-grading-jobs")
    public List<AIGradingJobSummaryResponse> listJobs(@PathVariable Long attemptId) {
        return aiGradingService.listJobs(attemptId);
    }

    @GetMapping("/submission-attempts/{attemptId}/ai-grading-results")
    public AIGradingResultResponse getLatestResult(@PathVariable Long attemptId) {
        return aiGradingService.getLatestResult(attemptId);
    }
}
