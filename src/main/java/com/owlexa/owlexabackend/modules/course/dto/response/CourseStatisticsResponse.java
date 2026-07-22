package com.owlexa.owlexabackend.modules.course.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseStatisticsResponse {
    private long totalClasses;
    private long totalEnrolledStudents;
    private long activeClasses;
    private long finishedClasses;
    private long plannedClasses;
}
