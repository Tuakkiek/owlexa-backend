package com.owlexa.owlexabackend.modules.homework.dto.response.student;

import com.owlexa.owlexabackend.modules.homework.enums.HomeworkSubmissionStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class StudentResultSummaryResponse {
    private Long id;
    private Long homeworkId;
    private Integer attemptNumber;
    private HomeworkSubmissionStatus status;
    private Double totalScore;
    private Instant submittedAt;
    private Instant gradedAt;
}
