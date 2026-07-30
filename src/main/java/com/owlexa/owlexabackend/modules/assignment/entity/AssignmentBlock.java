package com.owlexa.owlexabackend.modules.assignment.entity;

import com.owlexa.owlexabackend.common.assessment_document.AssessmentBlockType;
import com.owlexa.owlexabackend.common.assessment_document.BlockAlignment;
import com.owlexa.owlexabackend.modules.file.entity.StoredFile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "assignment_blocks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_assignment_blocks_assignment_position", columnNames = {"assignment_id", "position"}),
                @UniqueConstraint(name = "uk_assignment_blocks_assignment_item", columnNames = "assignment_item_id")
        },
        indexes = {
                @Index(name = "idx_assignment_blocks_file_id", columnList = "file_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class AssignmentBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false)
    private AssessmentBlockType blockType;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "content_json", columnDefinition = "LONGTEXT")
    private String contentJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private StoredFile file;

    @Column(length = 1000)
    private String caption;

    @Enumerated(EnumType.STRING)
    @Column
    private BlockAlignment alignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_item_id")
    private AssignmentItem assignmentItem;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
