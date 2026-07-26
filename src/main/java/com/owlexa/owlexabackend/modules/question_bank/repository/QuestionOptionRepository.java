package com.owlexa.owlexabackend.modules.question_bank.repository;

import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    List<QuestionOption> findAllByQuestion_IdOrderByDisplayOrderAsc(Long questionId);

    void deleteAllByQuestion_Id(Long questionId);
}
