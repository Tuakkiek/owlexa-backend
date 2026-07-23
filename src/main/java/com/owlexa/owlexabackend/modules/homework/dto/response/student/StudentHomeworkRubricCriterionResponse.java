package com.owlexa.owlexabackend.modules.homework.dto.response.student;

import lombok.Data;

@Data
public class StudentHomeworkRubricCriterionResponse {
    private Long id;
    private String name;
    private String description;
    private Double maxScore;
    private Integer displayOrder;
}
