package com.owlexa.owlexabackend.modules.homework.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "homework_rubric_criteria")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeworkRubricCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_id", nullable = false)
    private HomeworkRubric rubric;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "max_score")
    private Double maxScore;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
