package com.owlexa.owlexabackend.modules.assessment_builder.entity;

import com.owlexa.owlexabackend.modules.grading_criteria.entity.GradingCriteria;
import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
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
@Table(name = "assessment_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Column
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column
    private QuestionDifficulty difficulty;

    @Column(precision = 6, scale = 2)
    private BigDecimal points;

    @Column(columnDefinition = "LONGTEXT")
    private String explanation;

    @Column(name = "sample_answer", columnDefinition = "LONGTEXT")
    private String sampleAnswer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grading_criteria_id")
    private GradingCriteria gradingCriteria;

    @Column(name = "grading_criteria_name")
    private String gradingCriteriaName;

    @Column(name = "grading_criteria_content", columnDefinition = "LONGTEXT")
    private String gradingCriteriaContent;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @OneToMany(mappedBy = "assessmentItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AssessmentItemOption> options = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
