package com.owlexa.owlexabackend.modules.course.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDeleteValidationResponse {
    private boolean canDelete;
    private String message;
    private List<CourseDependencyDto> dependencies;
}
