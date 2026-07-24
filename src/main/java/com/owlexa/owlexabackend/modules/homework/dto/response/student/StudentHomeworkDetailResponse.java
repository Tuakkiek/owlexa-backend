package com.owlexa.owlexabackend.modules.homework.dto.response.student;

import com.owlexa.owlexabackend.modules.homework.enums.HomeworkAssignmentStatus;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkDifficulty;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkType;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class StudentHomeworkDetailResponse {
    private Long id;
    private String title;
    private String description;
    private String instructions;
    
    private HomeworkType homeworkType;
    private Integer estimatedTime;
    private HomeworkDifficulty difficulty;

    private HomeworkAssignmentStatus status;
    private Instant availableFrom;
    private Instant dueDate;
    private Instant closeAt;
    private Double maxScore;
    private Boolean allowLateSubmission;
    private Boolean allowResubmit;
    private Boolean publishScoreImmediately;
    private Boolean showAnswerAfterGrading;
    private Long clazzId;
    
    private List<StudentHomeworkQuestionResponse> questions;
}
