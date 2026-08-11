package com.owlexa.owlexabackend.modules.question_bank.dto.request;

import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import tools.jackson.databind.JsonNode;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionRequest {

    @NotNull(message = "Collection id is required")
    private Long collectionId;

    @NotBlank(message = "Section code is required")
    @Size(max = 50, message = "Section code must not exceed 50 characters")
    private String sectionCode;

    @NotNull(message = "Display order is required")
    @Min(value = 1, message = "Display order must be greater than or equal to 1")
    private Integer displayOrder;

    @NotNull(message = "Loại câu hỏi không được để trống")
    private QuestionType type;

    private JsonNode content;

    private QuestionDifficulty difficulty;

    @DecimalMin(value = "0.01", message = "Điểm câu hỏi phải lớn hơn 0")
    private BigDecimal points;

    private Long gradingCriteriaId;

    private JsonNode explanation;

    private JsonNode sampleAnswer;

    @Valid
    private List<QuestionOptionRequest> options;
}
