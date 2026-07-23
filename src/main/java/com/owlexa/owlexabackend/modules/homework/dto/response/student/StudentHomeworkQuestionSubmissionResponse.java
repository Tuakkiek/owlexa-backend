package com.owlexa.owlexabackend.modules.homework.dto.response.student;

import lombok.Data;

import java.util.List;

@Data
public class StudentHomeworkQuestionSubmissionResponse {
    private Long id;
    private Long questionId;
    private String textAnswer;
    private Double score;
    private Boolean isCorrect;
    private String teacherFeedback;
    private String aiFeedback;
    
    private List<StudentHomeworkSubmissionAttachmentResponse> attachments;
    private List<Long> selectedOptionIds; // Simplified to just IDs
    private List<StudentHomeworkRubricCriterionScoreResponse> criterionScores;
}
