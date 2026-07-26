package com.owlexa.owlexabackend.modules.question_bank.repository;

import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {

    Optional<Question> findByIdAndCenter_IdAndDeletedAtIsNull(Long id, Long centerId);

    boolean existsByGradingCriteria_IdAndCenter_IdAndTypeAndDeletedAtIsNull(
            Long gradingCriteriaId,
            Long centerId,
            QuestionType type
    );
}
