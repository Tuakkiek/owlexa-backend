package com.owlexa.owlexabackend.modules.question_bank.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionOptionRequest {

    @NotBlank(message = "Nội dung lựa chọn không được để trống")
    private String content;

    @NotNull(message = "Trạng thái đáp án đúng không được để trống")
    private Boolean isCorrect;

    @NotNull(message = "Thứ tự lựa chọn không được để trống")
    @Min(value = 1, message = "Thứ tự lựa chọn phải lớn hơn hoặc bằng 1")
    private Integer displayOrder;
}
