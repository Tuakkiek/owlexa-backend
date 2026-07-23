package com.owlexa.owlexabackend.modules.homework.entity;

import com.owlexa.owlexabackend.modules.homework.enums.AiScoringStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "homework_question_submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeworkQuestionSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_submission_id", nullable = false)
    private HomeworkSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private HomeworkQuestion question;

    @Column(columnDefinition = "TEXT")
    private String textAnswer;

    @Column(name = "score")
    private Double score;

    @Column(name = "teacher_override_score")
    private Double teacherOverrideScore;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(columnDefinition = "TEXT")
    private String teacherFeedback;

    @Column(columnDefinition = "TEXT")
    private String aiFeedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_scoring_status", nullable = false)
    @Builder.Default
    private AiScoringStatus aiScoringStatus = AiScoringStatus.PENDING;

    @Column(name = "ai_scored_at")
    private Instant aiScoredAt;

    @OneToMany(mappedBy = "questionSubmission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HomeworkSubmissionAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "questionSubmission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HomeworkQuestionSubmissionOption> selectedOptions = new ArrayList<>();

    @OneToMany(mappedBy = "questionSubmission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HomeworkRubricCriterionScore> criterionScores = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
