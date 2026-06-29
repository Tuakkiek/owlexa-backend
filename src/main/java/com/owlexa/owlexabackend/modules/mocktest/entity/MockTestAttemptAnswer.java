package com.owlexa.owlexabackend.modules.mocktest.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "mock_test_attempt_answers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"attempt_id", "question_id"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockTestAttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private MockTestAttempt attempt;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "student_answer", length = 1)
    private String studentAnswer;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Column(name = "correct_answer", nullable = false, length = 1)
    private String correctAnswer;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
