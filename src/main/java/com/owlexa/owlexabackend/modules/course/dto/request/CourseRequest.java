package com.owlexa.owlexabackend.modules.course.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRequest {

    @NotBlank(message = "Mã khóa học không được để trống")
    private String code;

    @NotBlank(message = "Tên khóa học không được để trống")
    private String name;

    private String description;

    private Integer defaultDuration;

    @Min(value = 0, message = "Học phí hàng tháng mặc định không được âm")
    private Double defaultMonthlyFee;

    @Min(value = 1, message = "Sĩ số tối đa mặc định phải ít nhất là 1")
    private Integer defaultMaxStudents;

    private Boolean isActive;
}
