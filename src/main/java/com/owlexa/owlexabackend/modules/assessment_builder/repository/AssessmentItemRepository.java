package com.owlexa.owlexabackend.modules.assessment_builder.repository;

import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AssessmentItemRepository extends JpaRepository<AssessmentItem, Long> {

    boolean existsByAssessmentId(Long assessmentId);

    @EntityGraph(attributePaths = {"options"})
    List<AssessmentItem> findAllByIdIn(Collection<Long> ids);
}
