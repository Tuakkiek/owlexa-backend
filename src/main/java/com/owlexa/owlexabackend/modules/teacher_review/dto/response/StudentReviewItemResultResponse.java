package com.owlexa.owlexabackend.modules.teacher_review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentReviewItemResultResponse {

    private Long assignmentItemId;
    private String questionTitle;
    private Integer displayOrder;
    private BigDecimal finalScore;
    private BigDecimal maxScore;
    private String teacherComment;
}
