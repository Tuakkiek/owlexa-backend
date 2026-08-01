package com.owlexa.owlexabackend.modules.assignment.entity;

import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItem;
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
@Table(name = "assignment_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private AssignmentContentBlock block;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_item_id")
    private AssessmentItem assessmentItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Column
    private String title;

    @Column(name = "content_json", nullable = false, columnDefinition = "LONGTEXT")
    private String contentJson;

    @Enumerated(EnumType.STRING)
    @Column
    private QuestionDifficulty difficulty;

    @Column(precision = 6, scale = 2)
    private BigDecimal points;

    @Column(name = "explanation_json", columnDefinition = "LONGTEXT")
    private String explanationJson;

    @Column(name = "sample_answer_json", columnDefinition = "LONGTEXT")
    private String sampleAnswerJson;

    @Column(name = "grading_criteria_name")
    private String gradingCriteriaName;

    @Column(name = "grading_criteria_content_json", columnDefinition = "LONGTEXT")
    private String gradingCriteriaContentJson;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @OneToMany(mappedBy = "assignmentItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AssignmentItemOption> options = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
