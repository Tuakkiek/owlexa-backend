package com.owlexa.owlexabackend.modules.homework.dto.response.teacher;

import com.owlexa.owlexabackend.modules.homework.dto.response.student.StudentHomeworkSubmissionAttachmentResponse;
import com.owlexa.owlexabackend.modules.homework.dto.response.student.StudentHomeworkRubricCriterionScoreResponse;
import lombok.Data;

import java.util.List;

@Data
public class TeacherQuestionSubmissionResponse {
    private Long id;
    private Long questionId;
    private String textAnswer;
    private Double autoScore;
    private Double teacherOverrideScore;
    private Double effectiveScore;
    private Boolean isCorrect;
    private String teacherFeedback;
    private String aiFeedback;
    
    private List<StudentHomeworkSubmissionAttachmentResponse> attachments;
    private List<Long> selectedOptionIds;
    private List<StudentHomeworkRubricCriterionScoreResponse> criterionScores;
}
