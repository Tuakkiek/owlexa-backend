package com.owlexa.owlexabackend.modules.teacher_review.dto.response;

import com.owlexa.owlexabackend.modules.student_submission.dto.response.StudentAttemptItemResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.SubmissionAnswerResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentReviewResultResponse {

    private Long submissionAttemptId;
    private String assignmentTitleSnapshot;
    private Integer attemptNumber;
    private BigDecimal finalScore;
    private BigDecimal maxScore;
    private String overallComment;
    private Instant releasedAt;
    private List<StudentAttemptItemResponse> items;
    private List<SubmissionAnswerResponse> answers;
    private List<StudentReviewItemResultResponse> essayItems;
}
