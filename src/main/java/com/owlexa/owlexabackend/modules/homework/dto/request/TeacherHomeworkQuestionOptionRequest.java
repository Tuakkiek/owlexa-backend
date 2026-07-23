package com.owlexa.owlexabackend.modules.homework.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeacherHomeworkQuestionOptionRequest {

    private Long id;

    @NotBlank(message = "Option content must not be blank")
    private String content;

    @NotNull
    private Integer sortOrder;

    @NotNull
    private Boolean isCorrect;
}
