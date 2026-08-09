package com.owlexa.owlexabackend.modules.student_submission.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionAttemptItemOptionResponse {

    private Long assignmentItemOptionId;
    private String content;
    private Integer displayOrder;
    private Boolean isCorrect;
}
