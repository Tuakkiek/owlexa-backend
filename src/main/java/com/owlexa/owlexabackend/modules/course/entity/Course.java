package com.owlexa.owlexabackend.modules.course.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Represents a curriculum course (e.g., "VSTEP B1", "IELTS 6.5").
 *
 * <p>A Course is a global template — it does NOT belong to any specific Center.
 * Multiple Classes across different Centers can reference the same Course.
 *
 * <p>Future modules (Lessons, Materials, MockTests, Essays, Certificates)
 * will belong to Course rather than Class.
 */
@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 50)
    private String level;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "default_duration")
    private Integer defaultDuration;

    @Column(name = "default_monthly_fee")
    private Double defaultMonthlyFee;

    @Column(name = "default_max_students")
    private Integer defaultMaxStudents;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
