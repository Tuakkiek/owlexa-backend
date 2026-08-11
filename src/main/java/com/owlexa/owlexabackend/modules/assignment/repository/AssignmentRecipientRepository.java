package com.owlexa.owlexabackend.modules.assignment.repository;

import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentRecipientRepository extends JpaRepository<AssignmentRecipient, Long> {

    Optional<AssignmentRecipient> findByAssignment_IdAndStudentUser_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNull(
            Long assignmentId,
            Long studentUserId,
            Long centerId
    );

    List<AssignmentRecipient> findAllByStudentUser_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNullOrderByAssignedAtDesc(
            Long studentUserId,
            Long centerId
    );

    Page<AssignmentRecipient> findAllByAssignment_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNull(
            Long assignmentId,
            Long centerId,
            Pageable pageable
    );

    long countByClazz_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNull(Long classId, Long centerId);
}
