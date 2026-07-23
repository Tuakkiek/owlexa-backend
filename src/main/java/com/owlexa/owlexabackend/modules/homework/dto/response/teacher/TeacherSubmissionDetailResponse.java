package com.owlexa.owlexabackend.modules.homework.dto.response.teacher;

import com.owlexa.owlexabackend.modules.homework.enums.HomeworkSubmissionStatus;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class TeacherSubmissionDetailResponse {
    private Long id;
    private Long homeworkId;
    private Long studentId;
    private String studentName;
    private Integer attemptNumber;
    private HomeworkSubmissionStatus status;
    private Instant submittedAt;
    private Instant gradedAt;
    private String teacherFeedback;
    private Double totalScore;
    
    private List<TeacherQuestionSubmissionResponse> questionSubmissions;
}
