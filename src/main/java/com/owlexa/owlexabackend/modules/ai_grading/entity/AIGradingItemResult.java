package com.owlexa.owlexabackend.modules.ai_grading.entity;

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
@Table(name = "ai_grading_item_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIGradingItemResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    private AIGradingResult result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_answer_id", nullable = false)
    private SubmissionAnswer submissionAnswer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_item_id", nullable = false)
    private AssignmentItem assignmentItem;

    @Column(name = "ai_score", precision = 8, scale = 2)
    private BigDecimal aiScore;

    @Column(name = "max_score", precision = 8, scale = 2)
    private BigDecimal maxScore;

    @Column(columnDefinition = "LONGTEXT")
    private String feedback;

    @Column(name = "rubric_analysis", columnDefinition = "LONGTEXT")
    private String rubricAnalysis;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
