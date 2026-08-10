package com.owlexa.owlexabackend.dto.admin;

public record AdminStatsResponse(
        long totalUsers,
        long totalOwners,
        long totalTeachers,
        long totalStudents,
        long totalCashiers,
        long totalAdmins,
        long totalCenters
) {
}
