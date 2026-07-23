package com.owlexa.owlexabackend.modules.homework.repository;

import com.owlexa.owlexabackend.modules.homework.entity.Homework;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HomeworkRepository extends JpaRepository<Homework, Long> {

    @EntityGraph(attributePaths = {"questions", "questions.options", "questions.rubric", "questions.rubric.criteria"})
    Optional<Homework> findWithDetailsByIdAndCenter_IdAndTeacher_Id(Long id, Long centerId, Long teacherId);

    @EntityGraph(attributePaths = {"questions", "questions.options", "questions.rubric", "questions.rubric.criteria"})
    Optional<Homework> findWithDetailsByIdAndCenter_Id(Long id, Long centerId);

    Optional<Homework> findByIdAndCenter_IdAndTeacher_Id(Long id, Long centerId, Long teacherId);
    
    Optional<Homework> findByIdAndCenter_Id(Long id, Long centerId);

    List<Homework> findAllByCenter_IdAndTeacher_Id(Long centerId, Long teacherId);

    List<Homework> findAllByClazz_IdInAndStatusInAndCenter_Id(List<Long> classIds, List<com.owlexa.owlexabackend.modules.homework.enums.HomeworkStatus> statuses, Long centerId);
}
