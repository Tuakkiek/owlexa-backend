package com.owlexa.owlexabackend.modules.homework.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class TeacherHomeworkRubricRequest {

    private Long id;

    @NotBlank(message = "Rubric title must not be blank")
    private String title;

    private String description;

    @Valid
    private List<TeacherHomeworkRubricCriterionRequest> criteria;
}
