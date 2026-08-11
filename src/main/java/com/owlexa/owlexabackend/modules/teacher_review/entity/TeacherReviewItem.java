package com.owlexa.owlexabackend.modules.teacher_review.entity;

import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItem;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAnswer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "teacher_review_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherReviewItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private TeacherReview review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_item_id", nullable = false)
    private AssignmentItem assignmentItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_answer_id")
    private SubmissionAnswer submissionAnswer;

    @Column(name = "question_title_snapshot")
    private String questionTitleSnapshot;

    @Column(name = "display_order_snapshot", nullable = false)
    private Integer displayOrderSnapshot;

    @Column(name = "final_score", precision = 8, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "max_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "item_comment", columnDefinition = "LONGTEXT")
    private String itemComment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
