package com.owlexa.owlexabackend.modules.course.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDependencyDto {
    private String className;
    private String status;
    private String teacherNames;
    private Integer studentCount;
}
