package com.owlexa.owlexabackend.modules.question_bank.dto.response;

import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionImportPreviewItemResponse {

    private int questionNumber;
    private String sectionCode;
    private Integer displayOrder;
    private String type;
    private String content;
    private QuestionDifficulty difficulty;
    private BigDecimal points;
    private int optionCount;
}
