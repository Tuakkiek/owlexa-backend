package com.owlexa.owlexabackend.modules.student_submission.dto.response;

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
public class SubmissionAnswerResponse {

    private Long assignmentItemId;
    private String answerText;
    private List<Long> selectedOptionIds;
    private BigDecimal autoScore;
    private BigDecimal maxScore;
    private Instant gradedAt;
}
