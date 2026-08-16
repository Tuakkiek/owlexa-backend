package com.owlexa.owlexabackend.modules.student_submission.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean isCorrect;
}
