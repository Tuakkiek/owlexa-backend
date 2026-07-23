package com.owlexa.owlexabackend.modules.homework.dto.response.teacher;

import com.owlexa.owlexabackend.modules.homework.enums.HomeworkSubmissionStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class TeacherSubmissionListResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Integer attemptNumber;
    private HomeworkSubmissionStatus status;
    private Instant submittedAt;
    private Double totalScore;
}
