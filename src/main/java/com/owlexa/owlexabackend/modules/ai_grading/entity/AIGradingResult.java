package com.owlexa.owlexabackend.modules.ai_grading.entity;

import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttempt;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
@Table(name = "ai_grading_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIGradingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private AIGradingJob job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_attempt_id", nullable = false)
    private SubmissionAttempt submissionAttempt;

    @Column(columnDefinition = "LONGTEXT")
    private String summary;

    @Column(name = "overall_feedback", columnDefinition = "LONGTEXT")
    private String overallFeedback;

    @Column(name = "ai_score", precision = 8, scale = 2)
    private BigDecimal aiScore;

    @Column(name = "max_score", precision = 8, scale = 2)
    private BigDecimal maxScore;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "raw_response", columnDefinition = "LONGTEXT")
    private String rawResponse;

    @OneToMany(mappedBy = "result", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AIGradingItemResult> itemResults = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
