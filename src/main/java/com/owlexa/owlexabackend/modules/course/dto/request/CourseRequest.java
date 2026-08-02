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

    @NotBlank(message = "Course code is required")
    private String code;

    @NotBlank(message = "Course name is required")
    private String name;

    private String description;

    private Integer defaultDuration;

    @Min(value = 1, message = "Default session count must be at least 1")
    private Integer defaultSessionCount;

    @Min(value = 0, message = "Default monthly fee cannot be negative")
    private Double defaultMonthlyFee;

    private Long defaultTeacherUserId;

    private Boolean isActive;
}
