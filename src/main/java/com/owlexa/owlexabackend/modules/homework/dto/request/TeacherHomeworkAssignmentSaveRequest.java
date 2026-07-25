package com.owlexa.owlexabackend.modules.homework.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class TeacherHomeworkAssignmentSaveRequest {

    @NotNull
    private Long templateId;

    @NotNull
    private Long clazzId;

    private Instant availableFrom;
    private Instant dueDate;
    private Instant closeAt;

    private Boolean allowLateSubmission;
    private Boolean allowResubmit;
    private Boolean publishScoreImmediately;
    private Boolean showAnswerAfterGrading;
    
    private com.owlexa.owlexabackend.modules.homework.enums.HomeworkAssignmentStatus status;
}
