package com.owlexa.owlexabackend.modules.assignment.repository;

import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long>, JpaSpecificationExecutor<Assignment> {

    Optional<Assignment> findByIdAndCenter_IdAndDeletedAtIsNull(Long id, Long centerId);

    Optional<Assignment> findByIdAndCenter_IdAndCreatedBy_IdAndDeletedAtIsNull(Long id, Long centerId, Long teacherUserId);
}
