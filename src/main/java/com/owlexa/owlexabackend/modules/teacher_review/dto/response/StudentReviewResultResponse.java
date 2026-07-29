package com.owlexa.owlexabackend.modules.teacher_review.dto.response;

import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentType;
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
    private AssessmentType assignmentTypeSnapshot;
    private Integer attemptNumber;
    private BigDecimal finalScore;
    private BigDecimal maxScore;
    private String overallComment;
    private Instant releasedAt;
    private List<StudentReviewItemResultResponse> essayItems;
}
