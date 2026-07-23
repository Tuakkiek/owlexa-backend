package com.owlexa.owlexabackend.modules.analytics.repository;

import com.owlexa.owlexabackend.modules.analytics.entity.AnalyticsStudentPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalyticsStudentPerformanceRepository extends JpaRepository<AnalyticsStudentPerformance, Long> {
    Optional<AnalyticsStudentPerformance> findByStudent_IdAndClazz_IdAndCenter_Id(Long studentId, Long classId, Long centerId);
}
