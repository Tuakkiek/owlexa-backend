package com.owlexa.owlexabackend.modules.homework.dto.response.student;

import lombok.Data;

import java.util.List;

@Data
public class StudentResultQuestionResponse {
    private Long id;
    private Long questionId;
    private String textAnswer;
    private Double effectiveScore;
    
    // Only populated if showAnswerAfterGrading = true
    private Boolean isCorrect; 
    private List<Long> correctOptionIds;

    private String teacherFeedback;
    private String aiFeedback;
    
    private List<StudentHomeworkSubmissionAttachmentResponse> attachments;
    private List<Long> selectedOptionIds;
    private List<StudentResultRubricScoreResponse> criterionScores;
}
