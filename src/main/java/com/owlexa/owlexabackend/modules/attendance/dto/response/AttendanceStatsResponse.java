package com.owlexa.owlexabackend.modules.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceStatsResponse {

    private Long classId;
    private String className;
    private String dateRangeLabel;
    private long totalStudents;
    private Map<String, Long> statusCounts;
    private Map<String, Double> statusPercentages;
}
