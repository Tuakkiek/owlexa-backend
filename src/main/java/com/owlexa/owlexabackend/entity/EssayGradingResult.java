package com.owlexa.owlexabackend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "essay_grading_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EssayGradingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false, unique = true)
    private EssaySubmission submission;

    @Column(name = "total_score", nullable = false)
    private Double totalScore;

    @Column(name = "max_score", nullable = false)
    private Double maxScore;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String feedback;

    @CreationTimestamp
    @Column(name = "graded_at", nullable = false, updatable = false)
    private Instant gradedAt;

    @OneToMany(mappedBy = "gradingResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EssayCriteriaScore> criteriaScores = new ArrayList<>();
}
