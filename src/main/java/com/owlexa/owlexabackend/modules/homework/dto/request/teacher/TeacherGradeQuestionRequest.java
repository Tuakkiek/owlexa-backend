package com.owlexa.owlexabackend.modules.homework.dto.request.teacher;

import lombok.Data;

import java.util.List;

@Data
public class TeacherGradeQuestionRequest {
    private Long questionSubmissionId;
    private Double score;
    private String teacherFeedback;
    private List<TeacherGradeCriterionRequest> criterionScores;
}
