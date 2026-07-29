package com.owlexa.owlexabackend.modules.ai_grading.mapper;

import com.owlexa.owlexabackend.modules.ai_grading.dto.response.AIGradingItemResultResponse;
import com.owlexa.owlexabackend.modules.ai_grading.dto.response.AIGradingJobSummaryResponse;
import com.owlexa.owlexabackend.modules.ai_grading.dto.response.AIGradingResultResponse;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingItemResult;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingJob;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingResult;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class AIGradingMapper {

    public AIGradingJobSummaryResponse toJobSummaryResponse(AIGradingJob job) {
        AIGradingResult result = job.getResult();
        return AIGradingJobSummaryResponse.builder()
                .id(job.getId())
                .submissionAttemptId(job.getSubmissionAttempt().getId())
                .status(job.getStatus())
                .requestedByUserId(job.getRequestedBy().getId())
                .requestedByFullName(job.getRequestedBy().getFullName())
                .resultId(result == null ? null : result.getId())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .failedAt(job.getFailedAt())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    public AIGradingResultResponse toResultResponse(AIGradingResult result) {
        return AIGradingResultResponse.builder()
                .id(result.getId())
                .jobId(result.getJob().getId())
                .submissionAttemptId(result.getSubmissionAttempt().getId())
                .summary(result.getSummary())
                .overallFeedback(result.getOverallFeedback())
                .aiScore(result.getAiScore())
                .maxScore(result.getMaxScore())
                .confidence(result.getConfidence())
                .itemResults(toItemResultResponses(result.getItemResults()))
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .build();
    }

    public AIGradingItemResultResponse toItemResultResponse(AIGradingItemResult itemResult) {
        return AIGradingItemResultResponse.builder()
                .id(itemResult.getId())
                .submissionAnswerId(itemResult.getSubmissionAnswer().getId())
                .assignmentItemId(itemResult.getAssignmentItem().getId())
                .aiScore(itemResult.getAiScore())
                .maxScore(itemResult.getMaxScore())
                .feedback(itemResult.getFeedback())
                .rubricAnalysis(itemResult.getRubricAnalysis())
                .confidence(itemResult.getConfidence())
                .createdAt(itemResult.getCreatedAt())
                .updatedAt(itemResult.getUpdatedAt())
                .build();
    }

    private List<AIGradingItemResultResponse> toItemResultResponses(List<AIGradingItemResult> itemResults) {
        return itemResults.stream()
                .sorted(Comparator.comparing(itemResult -> itemResult.getAssignmentItem().getDisplayOrder()))
                .map(this::toItemResultResponse)
                .toList();
    }
}
