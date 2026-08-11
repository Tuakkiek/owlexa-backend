package com.owlexa.owlexabackend.modules.course.dto.response;

import com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseClassResponse {
    private Long id;
    private String name;
    private List<String> teachers;
    private ClassStatus status;
    private long studentCount;
    private long scheduleCount;
}
