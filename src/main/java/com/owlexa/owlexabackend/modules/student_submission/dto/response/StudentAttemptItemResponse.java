package com.owlexa.owlexabackend.modules.student_submission.dto.response;

import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAttemptItemResponse {

    private Long assignmentItemId;
    private Long questionId;
    private QuestionType questionType;
    private String title;
    private JsonNode content;
    private QuestionDifficulty difficulty;
    private BigDecimal points;
    private Integer displayOrder;
    private List<SubmissionAttemptItemOptionResponse> options;
}
