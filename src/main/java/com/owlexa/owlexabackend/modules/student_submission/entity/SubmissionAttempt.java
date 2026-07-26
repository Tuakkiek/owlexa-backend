package com.owlexa.owlexabackend.modules.student_submission.entity;

import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentType;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipient;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "submission_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_recipient_id", nullable = false)
    private AssignmentRecipient assignmentRecipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionAttemptStatus status;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "assignment_title_snapshot", nullable = false)
    private String assignmentTitleSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type_snapshot", nullable = false)
    private AssessmentType assignmentTypeSnapshot;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "last_saved_at")
    private Instant lastSavedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "auto_score", precision = 8, scale = 2)
    private BigDecimal autoScore;

    @Column(name = "max_score", precision = 8, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "active_attempt_key")
    private Long activeAttemptKey;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SubmissionAnswer> answers = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
