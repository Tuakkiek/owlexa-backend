package com.owlexa.owlexabackend.modules.homework.dto.response.teacher;

import lombok.Data;

@Data
public class TeacherSubmissionStatsResponse {
    private Long totalSubmissions;
    private Long gradedCount;
    private Long pendingCount;
    private Long returnedCount;
    private Double averageScore;
}
