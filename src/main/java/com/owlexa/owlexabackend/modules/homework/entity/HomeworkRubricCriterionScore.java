package com.owlexa.owlexabackend.modules.homework.entity;

import com.owlexa.owlexabackend.modules.homework.enums.GraderType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "homework_rubric_criterion_scores")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeworkRubricCriterionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_submission_id", nullable = false)
    private HomeworkQuestionSubmission questionSubmission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criterion_id", nullable = false)
    private HomeworkRubricCriterion criterion;

    @Column(name = "score")
    private Double score;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "grader_type", nullable = false)
    private GraderType graderType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
