package com.owlexa.owlexabackend.modules.question_bank.repository;

import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class QuestionSpecifications {

    private QuestionSpecifications() {
    }

    public static Specification<Question> search(
            Long centerId,
            String search,
            QuestionType type,
            QuestionDifficulty difficulty,
            Long gradingCriteriaId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("center").get("id"), centerId));
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("content")), pattern)
                ));
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
