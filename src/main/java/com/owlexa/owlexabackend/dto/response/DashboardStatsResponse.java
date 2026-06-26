package com.owlexa.owlexabackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private long totalStudents;
    private long totalTeachers;
    private long totalClasses;
    private long totalFeeRecords;
    private long unpaidFeeRecords;
    private long paidFeeRecords;
    private BigDecimal totalRevenue;
}
