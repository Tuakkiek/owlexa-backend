package com.owlexa.owlexabackend.modules.analytics.entity;

import com.owlexa.owlexabackend.common.context.TenantAware;
import com.owlexa.owlexabackend.common.listener.TenantEntityListener;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkAssignment;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkRubricCriterion;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.Instant;

@Entity
@Table(name = "analytics_rubric_weakness")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "center_id = :tenantId")
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsRubricWeakness implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private Class clazz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_assignment_id", nullable = false)
    private HomeworkAssignment homeworkAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_criterion_id", nullable = false)
    private HomeworkRubricCriterion rubricCriterion;

    @Column(name = "submission_count", nullable = false)
    @Builder.Default
    private Integer submissionCount = 0;
    
    @Column(name = "average_score", nullable = false)
    @Builder.Default
    private Double averageScore = 0.0;
    
    @Column(name = "max_score")
    private Double maxScore;
    
    @Column(name = "percentage", nullable = false)
    @Builder.Default
    private Double percentage = 0.0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ── AI Drift Analytics ────────────────────────────────────────────────

    /** Rolling average of AI-assigned scores for this criterion. Null until first AI scoring. */
    @Column(name = "ai_average_score")
    private Double aiAverageScore;

    /** Rolling average of teacher override scores for this criterion. Null until first override. */
    @Column(name = "teacher_average_score")
    private Double teacherAverageScore;

    /**
     * Drift rate = |aiAverage - teacherAverage| / maxScore.
     * Ranges 0.0 (perfect agreement) to 1.0 (maximum disagreement).
     * Null until both AI and teacher scores exist.
     */
    @Column(name = "drift_rate")
    private Double driftRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private Center center;

    @Override
    public Long getCenterId() {
        return center != null ? center.getId() : null;
    }
}
