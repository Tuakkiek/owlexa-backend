package com.owlexa.owlexabackend.modules.assessment_builder.repository;

import com.owlexa.owlexabackend.modules.assessment_builder.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, Long>, JpaSpecificationExecutor<Assessment> {

    Optional<Assessment> findByIdAndCenter_IdAndDeletedAtIsNull(Long id, Long centerId);
}
