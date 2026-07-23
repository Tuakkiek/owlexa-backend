package com.owlexa.owlexabackend.modules.homework.repository;

import com.owlexa.owlexabackend.modules.homework.entity.HomeworkQuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HomeworkQuestionOptionRepository extends JpaRepository<HomeworkQuestionOption, Long> {
}
