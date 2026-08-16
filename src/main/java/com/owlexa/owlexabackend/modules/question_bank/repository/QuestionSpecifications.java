package com.owlexa.owlexabackend.modules.question_bank.repository;

import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class QuestionSpecifications {

    private QuestionSpecifications() {
    }

    public static Specification<Question> search(
            Long centerId,
            Long createdById,
            String search,
            QuestionType type,
            QuestionDifficulty difficulty,
            Long gradingCriteriaId
    ) {
        return search(centerId, createdById, search, null, null, type, difficulty, gradingCriteriaId);
    }

    public static Specification<Question> search(
            Long centerId,
            Long createdById,
            String search,
            Long collectionId,
            String sectionCode,
            QuestionType type,
            QuestionDifficulty difficulty,
            Long gradingCriteriaId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            var collection = root.get("collection");

            predicates.add(cb.equal(root.get("center").get("id"), centerId));
            predicates.add(cb.equal(collection.get("createdBy").get("id"), createdById));
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(cb.isNull(collection.get("deletedAt")));

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(collection.get("name")), pattern),
                        cb.like(cb.lower(root.get("sectionCode")), pattern),
                        cb.like(cb.lower(root.get("questionCode")), pattern),
                        cb.like(cb.lower(root.get("contentJson")), pattern)
                ));
            }

            if (collectionId != null) {
                predicates.add(cb.equal(collection.get("id"), collectionId));
            }

            if (sectionCode != null && !sectionCode.isBlank()) {
                predicates.add(cb.equal(root.get("sectionCode"), sectionCode));
            }

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            if (difficulty != null) {
                predicates.add(cb.equal(root.get("difficulty"), difficulty));
            }

            if (gradingCriteriaId != null) {
                predicates.add(cb.equal(root.get("gradingCriteria").get("id"), gradingCriteriaId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
