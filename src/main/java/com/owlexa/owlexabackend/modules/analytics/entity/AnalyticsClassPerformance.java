package com.owlexa.owlexabackend.modules.analytics.entity;

import com.owlexa.owlexabackend.common.context.TenantAware;
import com.owlexa.owlexabackend.common.listener.TenantEntityListener;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.homework.entity.Homework;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.Instant;

@Entity
@Table(name = "analytics_class_performance")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "center_id = :tenantId")
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsClassPerformance implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private Class clazz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_id", nullable = false)
    private Homework homework;

    @Column(name = "submitted_count", nullable = false)
    @Builder.Default
    private Integer submittedCount = 0;

    @Column(name = "graded_count", nullable = false)
    @Builder.Default
    private Integer gradedCount = 0;

    @Column(name = "late_submission_count", nullable = false)
    @Builder.Default
    private Integer lateSubmissionCount = 0;

    @Column(name = "missing_submission_count", nullable = false)
    @Builder.Default
    private Integer missingSubmissionCount = 0;

    @Column(name = "average_score", nullable = false)
    @Builder.Default
    private Double averageScore = 0.0;
    
    @Column(name = "highest_score")
    private Double highestScore;
    
    @Column(name = "lowest_score")
    private Double lowestScore;
    
    @Column(name = "pass_rate", nullable = false)
    @Builder.Default
    private Double passRate = 0.0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private Center center;

    @Override
    public Long getCenterId() {
        return center != null ? center.getId() : null;
    }
}
