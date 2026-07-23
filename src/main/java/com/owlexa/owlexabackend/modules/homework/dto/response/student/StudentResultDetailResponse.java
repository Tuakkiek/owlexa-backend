package com.owlexa.owlexabackend.modules.homework.dto.response.student;

import com.owlexa.owlexabackend.modules.homework.enums.HomeworkSubmissionStatus;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class StudentResultDetailResponse {
    private Long id;
    private Long homeworkId;
    private Integer attemptNumber;
    private HomeworkSubmissionStatus status;
    private Double totalScore;
    private Instant submittedAt;
    private Instant gradedAt;
    
    private String teacherFeedback;
    private String aiImprovementSuggestions; // Future Epic 8
    
    private List<StudentResultQuestionResponse> questions;
}
