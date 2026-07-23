package com.owlexa.owlexabackend.modules.homework.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeacherHomeworkRubricCriterionRequest {

    private Long id;

    @NotBlank(message = "Criterion name must not be blank")
    private String name;

    private String description;

    @NotNull
    private Double maxScore;

    @NotNull
    private Integer displayOrder;
}
