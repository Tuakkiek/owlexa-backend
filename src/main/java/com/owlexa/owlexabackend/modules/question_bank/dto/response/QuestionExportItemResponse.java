package com.owlexa.owlexabackend.modules.question_bank.dto.response;

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
public class QuestionExportItemResponse {
    private String sectionCode;
    private Integer displayOrder;
    private String type;
    private JsonNode content;
    private String difficulty;
    private BigDecimal points;
    private JsonNode explanation;
    private JsonNode sampleAnswer;
    private List<QuestionExportOptionResponse> options;
}
