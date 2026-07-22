package com.owlexa.owlexabackend.modules.course.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer defaultDuration;
    private Double defaultMonthlyFee;
    private Integer defaultMaxStudents;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
