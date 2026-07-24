package com.owlexa.owlexabackend.modules.homework.dto.response.student;

import com.owlexa.owlexabackend.modules.homework.enums.HomeworkAssignmentStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class StudentHomeworkListResponse {
    private Long id;
    private String title;
    private HomeworkAssignmentStatus status;
    private Instant availableFrom;
    private Instant dueDate;
    private Instant closeAt;
    private Double maxScore;
    private Boolean allowLateSubmission;
    private Long clazzId;
}
