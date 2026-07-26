package com.owlexa.owlexabackend.modules.student_submission.dto.response;

import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionAttemptItemResponse {

    private Long assignmentItemId;
    private QuestionType questionType;
    private String title;
    private String content;
    private QuestionDifficulty difficulty;
    private BigDecimal points;
    private String explanation;
    private String sampleAnswer;
    private String gradingCriteriaName;
    private String gradingCriteriaContent;
    private Integer displayOrder;
    private List<SubmissionAttemptItemOptionResponse> options;
}
