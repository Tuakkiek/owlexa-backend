package com.owlexa.owlexabackend.modules.teacher_review.repository;

import com.owlexa.owlexabackend.modules.teacher_review.entity.TeacherReviewItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherReviewItemRepository extends JpaRepository<TeacherReviewItem, Long> {

    List<TeacherReviewItem> findAllByReview_IdOrderByDisplayOrderSnapshotAsc(Long reviewId);

    Optional<TeacherReviewItem> findByReview_IdAndAssignmentItem_Id(
            Long reviewId,
            Long assignmentItemId
    );
}
