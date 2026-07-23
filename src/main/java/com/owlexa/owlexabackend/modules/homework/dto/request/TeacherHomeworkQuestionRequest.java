package com.owlexa.owlexabackend.modules.homework.dto.request;

import com.owlexa.owlexabackend.modules.homework.enums.HomeworkQuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class TeacherHomeworkQuestionRequest {

    private Long id;

    @NotNull
    private HomeworkQuestionType type;

    @NotBlank(message = "Question text must not be blank")
    private String questionText;

    private String attachedImageUrl;
    private String attachedAudioUrl;
    private String attachedFileUrl;

    @NotNull
    private Integer sortOrder;
    
    @NotNull
    private Double maxScore;

    @Valid
    private List<TeacherHomeworkQuestionOptionRequest> options;

    @Valid
    private TeacherHomeworkRubricRequest rubric;
}
