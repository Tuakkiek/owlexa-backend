package com.owlexa.owlexabackend.modules.question_bank.repository;

import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionCollectionRepository extends JpaRepository<QuestionCollection, Long> {

    List<QuestionCollection> findAllByCenter_IdAndCreatedBy_IdAndDeletedAtIsNullOrderByNameAsc(Long centerId, Long createdById);

    Optional<QuestionCollection> findByIdAndCenter_IdAndCreatedBy_IdAndDeletedAtIsNull(Long id, Long centerId, Long createdById);

    Optional<QuestionCollection> findByCodeAndCenter_IdAndCreatedBy_IdAndDeletedAtIsNull(String code, Long centerId, Long createdById);

    boolean existsByCenter_IdAndCreatedBy_IdAndCode(Long centerId, Long createdById, String code);

    boolean existsByCenter_IdAndCreatedBy_IdAndNameIgnoreCaseAndDeletedAtIsNull(Long centerId, Long createdById, String name);

    boolean existsByCenter_IdAndCreatedBy_IdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(
            Long centerId,
            Long createdById,
            String name,
            Long id
    );
}
