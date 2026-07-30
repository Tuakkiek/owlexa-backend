package com.owlexa.owlexabackend.modules.assignment.repository;

import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentBlockRepository extends JpaRepository<AssignmentBlock, Long> {

    List<AssignmentBlock> findByAssignmentIdOrderByPositionAsc(Long assignmentId);
}
