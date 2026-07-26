package com.owlexa.owlexabackend.modules.assessment_builder.repository;

import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentItemRepository extends JpaRepository<AssessmentItem, Long> {
}
