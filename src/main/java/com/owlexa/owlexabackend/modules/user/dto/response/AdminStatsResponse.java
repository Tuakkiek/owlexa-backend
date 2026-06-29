package com.owlexa.owlexabackend.modules.user.dto.response;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsResponse {
    private long totalUsers;
    private long totalOwners;
    private long totalTeachers;
    private long totalStudents;
    private long totalCashiers;
    private long totalAdmins;
    private long totalCenters;
}
