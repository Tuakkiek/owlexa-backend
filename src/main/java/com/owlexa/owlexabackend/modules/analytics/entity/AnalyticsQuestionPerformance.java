package com.owlexa.owlexabackend.modules.analytics.entity;

import com.owlexa.owlexabackend.common.context.TenantAware;
import com.owlexa.owlexabackend.common.listener.TenantEntityListener;
import com.owlexa.owlexabackend.modules.homework.entity.Homework;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkQuestion;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.Instant;

@Entity
@Table(name = "analytics_question_performance")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "center_id = :tenantId")
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsQuestionPerformance implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private HomeworkQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_id", nullable = false)
    private Homework homework;

    @Column(name = "correct_rate", nullable = false)
    @Builder.Default
    private Double correctRate = 0.0;

    @Column(name = "wrong_rate", nullable = false)
    @Builder.Default
    private Double wrongRate = 0.0;

    @Column(name = "average_score", nullable = false)
    @Builder.Default
    private Double averageScore = 0.0;
    
    @Column(name = "difficulty_indicator")
    private String difficultyIndicator;

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
