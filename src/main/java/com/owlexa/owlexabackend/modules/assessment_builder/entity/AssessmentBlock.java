package com.owlexa.owlexabackend.modules.assessment_builder.entity;

import com.owlexa.owlexabackend.common.assessment_document.AssessmentBlockType;
import com.owlexa.owlexabackend.common.assessment_document.BlockAlignment;
import com.owlexa.owlexabackend.modules.file.entity.StoredFile;
import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "assessment_blocks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_assessment_blocks_assessment_position", columnNames = {"assessment_id", "position"}),
                @UniqueConstraint(name = "uk_assessment_blocks_assessment_question", columnNames = {"assessment_id", "question_id"}),
                @UniqueConstraint(name = "uk_assessment_blocks_assessment_item", columnNames = "assessment_item_id")
        },
        indexes = {
                @Index(name = "idx_assessment_blocks_file_id", columnList = "file_id"),
                @Index(name = "idx_assessment_blocks_question_id", columnList = "question_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class AssessmentBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

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
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(precision = 6, scale = 2)
    private BigDecimal points;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_item_id")
    private AssessmentItem assessmentItem;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
