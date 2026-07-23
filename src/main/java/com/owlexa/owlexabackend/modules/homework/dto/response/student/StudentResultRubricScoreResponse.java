package com.owlexa.owlexabackend.modules.homework.dto.response.student;

import com.owlexa.owlexabackend.modules.homework.enums.GraderType;
import lombok.Data;

@Data
public class StudentResultRubricScoreResponse {
    private Long id;
    private Long criterionId;
    private Double score;
    private String comment;
    private GraderType graderType;
}
