package com.owlexa.owlexabackend.modules.homework.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class TeacherHomeworkSaveRequest {

    @NotBlank(message = "Title must not be blank")
    private String title;

    private String description;
    
    private String instructions;

    private Instant dueDate;

    private Boolean allowLateSubmission;
    private Boolean allowResubmit;
    private Boolean publishScoreImmediately;
    private Boolean showAnswerAfterGrading;

    @NotNull
    private Double maxScore;

    @NotNull
    private Long clazzId;

    @Valid
    private List<TeacherHomeworkQuestionRequest> questions;
}
