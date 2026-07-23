package com.owlexa.owlexabackend.modules.homework.dto.response.student;

import lombok.Data;

import java.util.List;

@Data
public class StudentHomeworkRubricResponse {
    private Long id;
    private String title;
    private String description;
    private Double maxScore;
    private List<StudentHomeworkRubricCriterionResponse> criteria;
}
