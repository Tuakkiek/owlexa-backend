package com.owlexa.owlexabackend.modules.teacher_review.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherReviewUpdateRequest {

    @NotNull(message = "Phiên bản đánh giá là bắt buộc")
    @PositiveOrZero(message = "Phiên bản đánh giá phải lớn hơn hoặc bằng 0")
    private Long version;

    private Long selectedAiGradingResultId;

    private String overallComment;

    @NotNull(message = "Danh sách mục đánh giá không được để trống")
    @Valid
    private List<TeacherReviewItemRequest> items;
}
