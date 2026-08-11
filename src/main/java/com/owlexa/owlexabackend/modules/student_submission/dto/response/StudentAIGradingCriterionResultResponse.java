package com.owlexa.owlexabackend.modules.student_submission.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAIGradingCriterionResultResponse {

    private String name;
    private BigDecimal score;
    private BigDecimal maxScore;
    private String feedback;
}
