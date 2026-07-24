package com.owlexa.owlexabackend.modules.homework.repository;

import com.owlexa.owlexabackend.modules.homework.entity.HomeworkTemplate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HomeworkTemplateRepository extends JpaRepository<HomeworkTemplate, Long> {

    @EntityGraph(attributePaths = {"questions", "questions.options", "questions.rubric", "questions.rubric.criteria"})
    Optional<HomeworkTemplate> findWithDetailsByIdAndCenter_IdAndTeacher_Id(Long id, Long centerId, Long teacherId);

    @EntityGraph(attributePaths = {"questions", "questions.options", "questions.rubric", "questions.rubric.criteria"})
    Optional<HomeworkTemplate> findWithDetailsByIdAndCenter_Id(Long id, Long centerId);

    Optional<HomeworkTemplate> findByIdAndCenter_IdAndTeacher_Id(Long id, Long centerId, Long teacherId);
    
    Optional<HomeworkTemplate> findByIdAndCenter_Id(Long id, Long centerId);

    List<HomeworkTemplate> findAllByCenter_IdAndTeacher_Id(Long centerId, Long teacherId);
    
    List<HomeworkTemplate> findAllByCenter_Id(Long centerId);
}
