package com.owlexa.owlexabackend.modules.assessment_builder.repository;

import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentBlockRepository extends JpaRepository<AssessmentBlock, Long> {

    List<AssessmentBlock> findByAssessmentIdOrderByPositionAsc(Long assessmentId);

    boolean existsByAssessmentIdAndQuestionId(Long assessmentId, Long questionId);
}
