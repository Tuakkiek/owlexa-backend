package com.owlexa.owlexabackend.modules.assessment_builder.dto.response;

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
public class AssessmentItemResponse {

    private Long id;
    private Long questionId;
    private QuestionType questionType;
    private String title;
    private String content;
    private QuestionDifficulty difficulty;
    private BigDecimal points;
    private String explanation;
    private String sampleAnswer;
    private Long gradingCriteriaId;
    private String gradingCriteriaName;
    private String gradingCriteriaContent;
    private Integer displayOrder;
    private List<AssessmentItemOptionResponse> options;
}
