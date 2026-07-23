package com.owlexa.owlexabackend.modules.homework.dto.request.teacher;

import lombok.Data;

import java.util.List;

@Data
public class TeacherGradeSubmissionRequest {
    private String overallFeedback;
    private List<TeacherGradeQuestionRequest> questionGrades;
}
