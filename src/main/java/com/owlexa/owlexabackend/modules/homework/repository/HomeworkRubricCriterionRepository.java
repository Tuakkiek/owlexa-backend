package com.owlexa.owlexabackend.modules.homework.repository;

import com.owlexa.owlexabackend.modules.homework.entity.HomeworkRubricCriterion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HomeworkRubricCriterionRepository extends JpaRepository<HomeworkRubricCriterion, Long> {
}
