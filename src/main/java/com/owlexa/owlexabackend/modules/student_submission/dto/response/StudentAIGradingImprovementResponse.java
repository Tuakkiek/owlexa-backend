package com.owlexa.owlexabackend.modules.student_submission.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAIGradingImprovementResponse {

    private String category;
    private String issue;
    private String suggestion;
    private String example;
}
