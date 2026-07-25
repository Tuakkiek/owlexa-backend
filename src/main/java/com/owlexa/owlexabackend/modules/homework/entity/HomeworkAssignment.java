package com.owlexa.owlexabackend.modules.homework.entity;

import com.owlexa.owlexabackend.common.context.TenantAware;
import com.owlexa.owlexabackend.common.listener.TenantEntityListener;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkAssignmentStatus;
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

@Entity
@Table(name = "homework_assignments")
@org.hibernate.annotations.SQLDelete(sql = "UPDATE homework_assignments SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "center_id = :tenantId")
@EntityListeners(TenantEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeworkAssignment implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_template_id", nullable = false)
    private HomeworkTemplate homeworkTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clazz_id", nullable = false)
    private Class clazz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HomeworkAssignmentStatus status;

    @Column(name = "publish_at")
    private Instant publishAt;

    @Column(name = "available_from")
    private Instant availableFrom;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "close_at")
    private Instant closeAt;

    @Column(name = "allow_late_submission", nullable = false)
    @Builder.Default
    private Boolean allowLateSubmission = false;

    @Column(name = "allow_resubmit", nullable = false)
    @Builder.Default
    private Boolean allowResubmit = false;

    @Column(name = "publish_score_immediately", nullable = false)
    @Builder.Default
    private Boolean publishScoreImmediately = false;

    @Column(name = "is_grades_released", nullable = false)
    @Builder.Default
    private Boolean isGradesReleased = false;

    @Column(name = "show_answer_after_grading", nullable = false)
    @Builder.Default
    private Boolean showAnswerAfterGrading = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private Center center;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Override
    public Long getCenterId() {
        return center != null ? center.getId() : null;
    }

    public void schedule(java.time.Clock clock) {
        if (this.status != HomeworkAssignmentStatus.DRAFT) {
            throw new com.owlexa.owlexabackend.common.exception.BusinessRuleException("Only DRAFT assignments can be SCHEDULED.");
        }
        this.status = HomeworkAssignmentStatus.SCHEDULED;
        this.scheduledAt = Instant.now(clock);
    }

    public void open(java.time.Clock clock) {
        if (this.status != HomeworkAssignmentStatus.SCHEDULED && this.status != HomeworkAssignmentStatus.DRAFT) {
            throw new com.owlexa.owlexabackend.common.exception.BusinessRuleException("Only DRAFT or SCHEDULED assignments can be opened.");
        }
        this.status = HomeworkAssignmentStatus.OPEN;
        this.openedAt = Instant.now(clock);
    }

    public void close(java.time.Clock clock) {
        if (this.status != HomeworkAssignmentStatus.OPEN) {
            throw new com.owlexa.owlexabackend.common.exception.BusinessRuleException("Only OPEN assignments can be closed.");
        }
        this.status = HomeworkAssignmentStatus.CLOSED;
        this.closedAt = Instant.now(clock);
    }

    public void archive(java.time.Clock clock) {
        this.status = HomeworkAssignmentStatus.ARCHIVED;
        this.archivedAt = Instant.now(clock);
    }

    public void cancel(java.time.Clock clock) {
        if (this.status == HomeworkAssignmentStatus.CLOSED || this.status == HomeworkAssignmentStatus.ARCHIVED) {
            throw new com.owlexa.owlexabackend.common.exception.BusinessRuleException("Cannot cancel a closed or archived assignment.");
        }
        this.status = HomeworkAssignmentStatus.CANCELLED;
        this.cancelledAt = Instant.now(clock);
    }
}
