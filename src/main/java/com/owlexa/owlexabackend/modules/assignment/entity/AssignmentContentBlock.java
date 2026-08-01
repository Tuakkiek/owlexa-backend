package com.owlexa.owlexabackend.modules.assignment.entity;

import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentContentBlock;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "assignment_content_blocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentContentBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_block_id")
    private AssessmentContentBlock assessmentBlock;

    @Column(nullable = false)
    private Integer position;

    @Column
    private String title;

    @Column(name = "content_json", nullable = false, columnDefinition = "LONGTEXT")
    @Builder.Default
    private String contentJson = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
