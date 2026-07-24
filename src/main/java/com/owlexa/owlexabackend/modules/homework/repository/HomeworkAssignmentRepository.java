package com.owlexa.owlexabackend.modules.homework.repository;

import com.owlexa.owlexabackend.modules.homework.entity.HomeworkAssignment;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkAssignmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HomeworkAssignmentRepository extends JpaRepository<HomeworkAssignment, Long> {

    Optional<HomeworkAssignment> findByIdAndCenter_Id(Long id, Long centerId);

    @EntityGraph(attributePaths = {"homeworkTemplate", "homeworkTemplate.questions"})
    Optional<HomeworkAssignment> findWithTemplateByIdAndCenter_Id(Long id, Long centerId);

    List<HomeworkAssignment> findAllByClazz_IdInAndStatusInAndCenter_Id(List<Long> classIds, List<HomeworkAssignmentStatus> statuses, Long centerId);

    List<HomeworkAssignment> findAllByStatus(HomeworkAssignmentStatus status);

    boolean existsByHomeworkTemplate_Id(Long templateId);
    
    long countByHomeworkTemplate_Id(Long templateId);

    List<HomeworkAssignment> findAllByCenter_Id(Long centerId);
}
