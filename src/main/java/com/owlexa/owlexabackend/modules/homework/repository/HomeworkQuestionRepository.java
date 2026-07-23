package com.owlexa.owlexabackend.modules.homework.repository;

import com.owlexa.owlexabackend.modules.homework.entity.HomeworkQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HomeworkQuestionRepository extends JpaRepository<HomeworkQuestion, Long> {
}
