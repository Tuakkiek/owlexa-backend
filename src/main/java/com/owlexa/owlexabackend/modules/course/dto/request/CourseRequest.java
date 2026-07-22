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

    @NotBlank(message = "code is required")
    private String code;

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    private Integer defaultDuration;

    @Min(value = 0, message = "defaultMonthlyFee cannot be negative")
    private Double defaultMonthlyFee;

    @Min(value = 1, message = "defaultMaxStudents must be at least 1")
    private Integer defaultMaxStudents;

    private Boolean isActive;
}
