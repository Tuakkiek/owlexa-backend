package com.owlexa.owlexabackend.modules.question_bank.repository;

import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionCollectionRepository extends JpaRepository<QuestionCollection, Long> {

    List<QuestionCollection> findAllByCenter_IdAndDeletedAtIsNullOrderByNameAsc(Long centerId);

    Optional<QuestionCollection> findByIdAndCenter_IdAndDeletedAtIsNull(Long id, Long centerId);

    Optional<QuestionCollection> findByCodeAndCenter_IdAndDeletedAtIsNull(String code, Long centerId);

    boolean existsByCenter_IdAndCode(Long centerId, String code);

    boolean existsByCenter_IdAndNameIgnoreCaseAndDeletedAtIsNull(Long centerId, String name);

    boolean existsByCenter_IdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(
            Long centerId,
            String name,
            Long id
    );
}
