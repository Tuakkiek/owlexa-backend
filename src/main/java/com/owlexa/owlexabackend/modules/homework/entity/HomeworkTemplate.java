package com.owlexa.owlexabackend.modules.homework.entity;

import com.owlexa.owlexabackend.common.context.TenantAware;
import com.owlexa.owlexabackend.common.listener.TenantEntityListener;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkDifficulty;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkType;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "homework_templates")
@org.hibernate.annotations.SQLDelete(sql = "UPDATE homework_templates SET archived = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@org.hibernate.annotations.SQLRestriction("archived = false")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "center_id = :tenantId")
@EntityListeners(TenantEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeworkTemplate implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(name = "homework_type")
    private HomeworkType homeworkType;

    @Column(name = "estimated_time")
    private Integer estimatedTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    private HomeworkDifficulty difficulty;

    @Column(name = "archived", nullable = false)
    @Builder.Default
    private Boolean archived = false;

    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "parent_template_id")
    private Long parentTemplateId;

    @Column(name = "max_score")
    private Double maxScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private Center center;

    @OneToMany(mappedBy = "homeworkTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HomeworkQuestion> questions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Override
    public Long getCenterId() {
        return center != null ? center.getId() : null;
    }
}
