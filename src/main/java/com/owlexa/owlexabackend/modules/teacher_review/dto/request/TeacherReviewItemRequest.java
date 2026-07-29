package com.owlexa.owlexabackend.modules.teacher_review.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherReviewItemRequest {

    @NotNull(message = "Mã mục bài tập không được để trống")
    private Long assignmentItemId;

    @DecimalMin(value = "0.00", message = "Điểm số phải lớn hơn hoặc bằng 0")
    @Digits(integer = 6, fraction = 2, message = "Điểm số chỉ được chứa tối đa 6 chữ số nguyên và 2 chữ số thập phân")
    private BigDecimal finalScore;

    private String itemComment;
}
