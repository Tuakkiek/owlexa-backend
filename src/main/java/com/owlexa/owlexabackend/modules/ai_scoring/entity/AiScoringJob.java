package com.owlexa.owlexabackend.modules.ai_scoring.entity;

import com.owlexa.owlexabackend.modules.homework.enums.AiScoringStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Audit record for each AI scoring attempt on a single question submission.
 * Provides retry tracking, token usage, and error history.
 */
@Entity
@Table(name = "ai_scoring_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiScoringJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(name = "question_sub_id", nullable = false)
    private Long questionSubId;

    @Column(name = "center_id", nullable = false)
    private Long centerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AiScoringStatus status = AiScoringStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "model_used", length = 100)
    private String modelUsed;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "response_tokens")
    private Integer responseTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
