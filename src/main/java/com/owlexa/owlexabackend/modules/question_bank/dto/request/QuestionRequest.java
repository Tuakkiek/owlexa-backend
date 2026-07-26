package com.owlexa.owlexabackend.modules.question_bank.dto.request;

import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class QuestionRequest {

    @NotNull(message = "Loại câu hỏi không được để trống")
    private QuestionType type;

    @Size(max = 255, message = "Tiêu đề câu hỏi không được vượt quá 255 ký tự")
    private String title;

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String content;

    private QuestionDifficulty difficulty;

    @DecimalMin(value = "0.01", message = "Điểm câu hỏi phải lớn hơn 0")
    private BigDecimal points;

    private Long gradingCriteriaId;

    private String explanation;

    private String sampleAnswer;

    @Valid
    private List<QuestionOptionRequest> options;
}
