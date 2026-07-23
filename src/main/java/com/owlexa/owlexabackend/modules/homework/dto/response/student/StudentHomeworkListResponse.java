package com.owlexa.owlexabackend.modules.homework.dto.response.student;

import com.owlexa.owlexabackend.modules.homework.enums.HomeworkStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class StudentHomeworkListResponse {
    private Long id;
    private String title;
    private HomeworkStatus status;
    private Instant dueDate;
    private Double maxScore;
    private Boolean allowLateSubmission;
    private Long clazzId;
}
