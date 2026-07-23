package com.owlexa.owlexabackend.modules.analytics.repository;

import com.owlexa.owlexabackend.modules.analytics.entity.AnalyticsRubricWeakness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalyticsRubricWeaknessRepository extends JpaRepository<AnalyticsRubricWeakness, Long> {
    Optional<AnalyticsRubricWeakness> findByClazz_IdAndRubricCriterion_IdAndCenter_Id(Long classId, Long criterionId, Long centerId);
    List<AnalyticsRubricWeakness> findAllByHomework_IdAndCenter_Id(Long homeworkId, Long centerId);
    List<AnalyticsRubricWeakness> findAllByClazz_IdAndCenter_Id(Long classId, Long centerId);
}
