package com.owlexa.owlexabackend.modules.teacher_review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherReviewItemResponse {

    private Long id;
    private Long assignmentItemId;
    private Long submissionAnswerId;
    private String questionTitleSnapshot;
    private Integer displayOrderSnapshot;
    private BigDecimal finalScore;
    private BigDecimal maxScore;
    private String itemComment;
    private Instant createdAt;
    private Instant updatedAt;
}
