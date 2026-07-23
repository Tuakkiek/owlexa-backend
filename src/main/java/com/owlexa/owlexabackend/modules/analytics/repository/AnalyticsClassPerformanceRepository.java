package com.owlexa.owlexabackend.modules.analytics.repository;

import com.owlexa.owlexabackend.modules.analytics.entity.AnalyticsClassPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalyticsClassPerformanceRepository extends JpaRepository<AnalyticsClassPerformance, Long> {
    Optional<AnalyticsClassPerformance> findByClazz_IdAndHomework_IdAndCenter_Id(Long classId, Long homeworkId, Long centerId);
    List<AnalyticsClassPerformance> findAllByClazz_IdAndCenter_Id(Long classId, Long centerId);
}
