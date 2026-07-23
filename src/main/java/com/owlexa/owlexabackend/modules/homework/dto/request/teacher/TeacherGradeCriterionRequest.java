package com.owlexa.owlexabackend.modules.homework.dto.request.teacher;

import lombok.Data;

@Data
public class TeacherGradeCriterionRequest {
    private Long criterionId;
    private Double score;
    private String comment;
}
