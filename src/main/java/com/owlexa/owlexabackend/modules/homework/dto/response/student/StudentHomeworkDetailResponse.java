package com.owlexa.owlexabackend.modules.homework.dto.response.student;

import com.owlexa.owlexabackend.modules.homework.enums.HomeworkStatus;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class StudentHomeworkDetailResponse {
    private Long id;
    private String title;
    private String description;
    private String instructions;
    private HomeworkStatus status;
    private Instant dueDate;
    private Instant publishedAt;
    private Instant closedAt;
    private Double maxScore;
    private Boolean allowLateSubmission;
    private Boolean allowResubmit;
    private Boolean publishScoreImmediately;
    private Boolean showAnswerAfterGrading;
    private Long clazzId;
    
    private List<StudentHomeworkQuestionResponse> questions;
}
