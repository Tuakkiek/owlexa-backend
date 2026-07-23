package com.owlexa.owlexabackend.modules.homework.dto.response.student;

import com.owlexa.owlexabackend.modules.homework.enums.HomeworkSubmissionStatus;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class StudentHomeworkSubmissionResponse {
    private Long id;
    private Long homeworkId;
    private Integer attemptNumber;
    private HomeworkSubmissionStatus status;
    private Instant startedAt;
    private Instant lastSavedAt;
    private Instant submittedAt;
    private Instant gradedAt;
    private String teacherFeedback;
    private Long version;
    private Double totalScore; // Calculated field
    
    private List<StudentHomeworkQuestionSubmissionResponse> questionSubmissions;
}
