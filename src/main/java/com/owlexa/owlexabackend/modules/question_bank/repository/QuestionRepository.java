package com.owlexa.owlexabackend.modules.question_bank.repository;

import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {

    @Override
    @EntityGraph(attributePaths = {"collection", "gradingCriteria"})
    Page<Question> findAll(Specification<Question> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"collection", "gradingCriteria", "options"})
    Optional<Question> findByIdAndCenter_IdAndDeletedAtIsNull(Long id, Long centerId);

    @EntityGraph(attributePaths = {"collection", "gradingCriteria", "options"})
    List<Question> findAllByIdInAndCenter_IdAndDeletedAtIsNull(Collection<Long> ids, Long centerId);

    boolean existsByCollection_IdAndDeletedAtIsNull(Long collectionId);

    boolean existsByCollection_IdAndDisplayOrderAndDeletedAtIsNull(
            Long collectionId,
            Integer displayOrder
    );

    boolean existsByCollection_IdAndDisplayOrderAndDeletedAtIsNullAndIdNot(
            Long collectionId,
            Integer displayOrder,
            Long id
    );

    long countByCollection_IdAndDeletedAtIsNull(Long collectionId);

    @Query("""
            select q.collection.id as collectionId, count(q.id) as questionCount
            from Question q
            where q.collection.id in :collectionIds
              and q.deletedAt is null
            group by q.collection.id
            """)
    List<CollectionQuestionCount> countActiveByCollectionIds(
            @Param("collectionIds") Collection<Long> collectionIds
    );

    @Query("""
            select q.sectionCode
            from Question q
            where q.collection.id = :collectionId
              and q.deletedAt is null
            group by q.sectionCode
            order by min(q.displayOrder), q.sectionCode
            """)
    List<String> findActiveSectionCodes(@Param("collectionId") Long collectionId);

    boolean existsByGradingCriteria_IdAndCenter_IdAndTypeAndDeletedAtIsNull(
            Long gradingCriteriaId,
            Long centerId,
            QuestionType type
    );

    interface CollectionQuestionCount {
        Long getCollectionId();

        long getQuestionCount();
    }
}
