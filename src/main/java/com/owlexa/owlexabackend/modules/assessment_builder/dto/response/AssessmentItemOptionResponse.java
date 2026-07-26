package com.owlexa.owlexabackend.modules.assessment_builder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentItemOptionResponse {

    private Long id;
    private String content;
    private Boolean isCorrect;
    private Integer displayOrder;
}
