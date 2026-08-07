package com.owlexa.owlexabackend.modules.assignment.repository;

import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentTarget;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentTargetRepository extends JpaRepository<AssignmentTarget, Long> {
    long countByClazz_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNull(Long classId, Long centerId);
}
