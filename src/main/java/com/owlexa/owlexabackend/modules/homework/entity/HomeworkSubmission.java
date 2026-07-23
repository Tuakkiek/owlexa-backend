package com.owlexa.owlexabackend.modules.homework.entity;

import com.owlexa.owlexabackend.common.context.TenantAware;
import com.owlexa.owlexabackend.common.listener.TenantEntityListener;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkSubmissionStatus;
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
@Table(name = "homework_submissions")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "center_id = :tenantId")
@EntityListeners(TenantEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeworkSubmission implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_id", nullable = false)
    private Homework homework;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User student;

    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private Integer attemptNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HomeworkSubmissionStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "last_saved_at")
    private Instant lastSavedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by_user_id")
    private User gradedBy;

    @Column(name = "graded_at")
    private Instant gradedAt;

    @Column(columnDefinition = "TEXT")
    private String teacherFeedback;

    @Version
    @Column(name = "version")
    @Builder.Default
    private Long version = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private Center center;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HomeworkQuestionSubmission> questionSubmissions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public Long getCenterId() {
        return center != null ? center.getId() : null;
    }
}
