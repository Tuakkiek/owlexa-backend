package com.owlexa.owlexabackend.modules.grading_criteria.repository;

import com.owlexa.owlexabackend.modules.grading_criteria.entity.GradingCriteria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GradingCriteriaRepository extends JpaRepository<GradingCriteria, Long> {

    List<GradingCriteria> findAllByCenter_IdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long centerId);

    List<GradingCriteria> findAllByCenter_IdAndDeletedAtIsNullAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(
            Long centerId,
            String name
    );

    Optional<GradingCriteria> findByIdAndCenter_IdAndDeletedAtIsNull(Long id, Long centerId);
}
