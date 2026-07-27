package com.owlexa.owlexabackend.modules.ai_grading.entity;

import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttempt;
import com.owlexa.owlexabackend.modules.user.entity.User;
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

@Entity
@Table(name = "ai_grading_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIGradingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_attempt_id", nullable = false)
    private SubmissionAttempt submissionAttempt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AIGradingJobStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Column(name = "prompt_template_version", nullable = false)
    private String promptTemplateVersion;

    @Column(name = "prompt_builder_version", nullable = false)
    private String promptBuilderVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_provider", nullable = false)
    private AIModelProvider modelProvider;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(precision = 4, scale = 2)
    private BigDecimal temperature;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(name = "system_prompt", nullable = false, columnDefinition = "LONGTEXT")
    private String systemPrompt;

    @Column(name = "user_prompt", nullable = false, columnDefinition = "LONGTEXT")
    private String userPrompt;

    @Column(name = "active_job_key")
    private Long activeJobKey;

    @OneToOne(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private AIGradingResult result;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
