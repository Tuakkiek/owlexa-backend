package com.owlexa.owlexabackend.modules.student_submission.entity;

import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItemOption;
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

import java.time.Instant;

@Entity
@Table(name = "submission_answer_options")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionAnswerOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_answer_id", nullable = false)
    private SubmissionAnswer submissionAnswer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_item_option_id", nullable = false)
    private AssignmentItemOption assignmentItemOption;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
