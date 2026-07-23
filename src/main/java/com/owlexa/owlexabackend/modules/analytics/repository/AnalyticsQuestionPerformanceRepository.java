package com.owlexa.owlexabackend.modules.analytics.repository;

import com.owlexa.owlexabackend.modules.analytics.entity.AnalyticsQuestionPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalyticsQuestionPerformanceRepository extends JpaRepository<AnalyticsQuestionPerformance, Long> {
    Optional<AnalyticsQuestionPerformance> findByQuestion_IdAndHomework_IdAndCenter_Id(Long questionId, Long homeworkId, Long centerId);
}
