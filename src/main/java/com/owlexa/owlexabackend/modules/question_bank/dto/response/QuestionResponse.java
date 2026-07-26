package com.owlexa.owlexabackend.modules.question_bank.dto.response;

import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionResponse {

    private Long id;
    private QuestionType type;
    private String title;
    private String content;
    private QuestionDifficulty difficulty;
    private BigDecimal points;
    private GradingCriteriaSummaryResponse gradingCriteria;
    private String explanation;
    private String sampleAnswer;
    private List<QuestionOptionResponse> options;
    private Instant createdAt;
    private Instant updatedAt;
}
