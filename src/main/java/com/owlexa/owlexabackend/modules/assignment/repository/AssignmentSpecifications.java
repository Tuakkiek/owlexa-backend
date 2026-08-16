package com.owlexa.owlexabackend.modules.assignment.repository;

import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class AssignmentSpecifications {

    private AssignmentSpecifications() {
    }

    public static Specification<Assignment> search(
            Long centerId,
            Long teacherUserId,
            String search,
            AssignmentStatus status,
            Long classId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("center").get("id"), centerId));
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (teacherUserId != null) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), teacherUserId));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (classId != null) {
                predicates.add(cb.equal(root.join("targets", JoinType.LEFT).get("clazz").get("id"), classId));
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Assignment> visibleToStudent(Long centerId, Long studentUserId, AssignmentStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("center").get("id"), centerId));
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(cb.equal(root.join("recipients", JoinType.INNER).get("studentUser").get("id"), studentUserId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
