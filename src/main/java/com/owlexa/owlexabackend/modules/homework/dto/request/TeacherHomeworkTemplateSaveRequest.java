package com.owlexa.owlexabackend.modules.homework.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkDifficulty;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class TeacherHomeworkTemplateSaveRequest {

    @NotBlank(message = "Title must not be blank")
    private String title;

    private String description;
    
    private String instructions;

    @NotNull
    private HomeworkType homeworkType;

    private Integer estimatedTime;

    private HomeworkDifficulty difficulty;

    private Long parentTemplateId;

    @NotNull
    private Double maxScore;

    @Valid
    private List<TeacherHomeworkQuestionRequest> questions;
}
